package com.superai.app.compiler.script

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class BuildConfig(
    val projectName: String,
    val packageName: String,
    val minSdk: Int = 29,
    val targetSdk: Int = 35,
    val compileSdk: Int = 35,
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val outputDir: String = "/sdcard/SuperAI/builds",
    val useHilt: Boolean = true,
    val useCompose: Boolean = true,
    val useRoom: Boolean = true,
    val extraDependencies: List<String> = emptyList()
)

@Singleton
class BuildScriptGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scriptsDir: File
        get() = File(context.filesDir, "build_scripts").also { it.mkdirs() }

    fun generateBuildScript(config: BuildConfig): File {
        val scriptFile = File(scriptsDir, "build_${config.projectName}_${System.currentTimeMillis()}.sh")
        scriptFile.writeText(buildScript(config))
        scriptFile.setExecutable(true)
        Timber.d("Build script generated: ${scriptFile.absolutePath}")
        return scriptFile
    }

    private fun buildScript(c: BuildConfig): String = """
#!/bin/bash
# SuperAI Build Script — generated ${java.time.LocalDate.now()} for ${c.projectName}
set -euo pipefail

PROJECT="${"$"}{1:-${c.projectName}}"
PKG="${c.packageName}"
OUT="${c.outputDir}/${"$"}PROJECT"
ANDROID_SDK="${"$"}{ANDROID_HOME:-/opt/android-sdk}"
BUILD_TOOLS="${"$"}ANDROID_SDK/build-tools/${c.compileSdk}.0.0"
PLATFORM="${"$"}ANDROID_SDK/platforms/android-${c.compileSdk}"

echo "=== SuperAI Build: ${"$"}PROJECT ==="
mkdir -p "${"$"}OUT"/{gen,obj,dex_out,res_out}

# 1 — Compile resources
"${"$"}BUILD_TOOLS/aapt2" compile --dir src/main/res -o "${"$"}OUT/res.zip"

# 2 — Link resources
"${"$"}BUILD_TOOLS/aapt2" link \
  -o "${"$"}OUT/base.apk" \
  -I "${"$"}PLATFORM/android.jar" \
  --manifest src/main/AndroidManifest.xml \
  --java "${"$"}OUT/gen" \
  -R "${"$"}OUT/res.zip" \
  --auto-add-overlay \
  --min-sdk-version ${c.minSdk} \
  --target-sdk-version ${c.targetSdk} \
  --version-code ${c.versionCode} \
  --version-name "${c.versionName}"

# 3 — Compile Java/Kotlin sources
javac -source 17 -target 17 \
  -cp "${"$"}PLATFORM/android.jar" \
  -d "${"$"}OUT/obj" \
  "${"$"}OUT/gen/${"$"}{PKG//./\/}/R.java" \
  $(find src/main/java -name "*.java" 2>/dev/null | tr '\n' ' ')

# 4 — Dex
"${"$"}BUILD_TOOLS/d8" \
  --output "${"$"}OUT/dex_out/" \
  --lib "${"$"}PLATFORM/android.jar" \
  --min-api ${c.minSdk} \
  $(find "${"$"}OUT/obj" -name "*.class" | tr '\n' ' ')

# 5 — Add dex to APK
python3 -c "
import zipfile, shutil
shutil.copy('${"$"}OUT/base.apk', '${"$"}OUT/with_dex.apk')
with zipfile.ZipFile('${"$"}OUT/with_dex.apk', 'a') as z:
    z.write('${"$"}OUT/dex_out/classes.dex', 'classes.dex')
"

# 6 — Align
"${"$"}BUILD_TOOLS/zipalign" -v 4 "${"$"}OUT/with_dex.apk" "${"$"}OUT/aligned.apk"

# 7 — Sign (debug key)
if [ ! -f debug.keystore ]; then
  keytool -genkey -v -keystore debug.keystore -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass android -keypass android \
    -dname "CN=SuperAI Debug, O=SuperAI, C=US"
fi
"${"$"}BUILD_TOOLS/apksigner" sign \
  --ks debug.keystore --ks-pass pass:android \
  --key-pass pass:android --ks-key-alias androiddebugkey \
  --v3-signing-enabled true \
  --out "${"$"}OUT/${"$"}PROJECT-debug.apk" \
  "${"$"}OUT/aligned.apk"

echo "=== BUILD COMPLETE: ${"$"}OUT/${"$"}PROJECT-debug.apk ==="
ls -lh "${"$"}OUT/${"$"}PROJECT-debug.apk"
""".trimIndent()
}
