package com.superai.app.compiler.script

import android.content.Context
import timber.log.Timber
import java.io.File
import java.time.LocalDate

data class BuildConfig(
    val projectName: String,
    val packageName: String,
    val minSdk: Int = 26,
    val targetSdk: Int = 34,
    val compileSdk: Int = 34,
    val versionCode: Int = 1,
    val versionName: String = "1.0.0",
    val outputDir: String = "/sdcard/SuperAI/builds"
)

class BuildScriptGenerator(private val context: Context) {

    private val scriptsDir: File
        get() = File(context.filesDir, "build_scripts").also { it.mkdirs() }

    fun generateBuildScript(config: BuildConfig): File {
        val scriptFile = File(scriptsDir, "build_${config.projectName}_${System.currentTimeMillis()}.sh")
        scriptFile.writeText(buildScript(config))
        scriptFile.setExecutable(true)
        Timber.d("Build script generated: %s", scriptFile.absolutePath)
        return scriptFile
    }

    private fun buildScript(c: BuildConfig): String = """
#!/bin/bash
# SuperAI Build Script — generated ${LocalDate.now()} for ${c.projectName}
set -euo pipefail
PROJECT="${"$"}{1:-${c.projectName}}"
PKG="${c.packageName}"
OUT="${c.outputDir}/${"$"}PROJECT"
SDK="${"$"}{ANDROID_HOME:-/opt/android-sdk}"
BT="${"$"}SDK/build-tools/${c.compileSdk}.0.0"
PLT="${"$"}SDK/platforms/android-${c.compileSdk}"
mkdir -p "${"$"}OUT"/{gen,obj,dex_out}
echo "=== SuperAI Build: ${"$"}PROJECT ==="
"${"$"}BT/aapt2" compile --dir src/main/res -o "${"$"}OUT/res.zip"
"${"$"}BT/aapt2" link -o "${"$"}OUT/base.apk" -I "${"$"}PLT/android.jar" \
  --manifest src/main/AndroidManifest.xml --java "${"$"}OUT/gen" -R "${"$"}OUT/res.zip" \
  --auto-add-overlay --min-sdk-version ${c.minSdk} --target-sdk-version ${c.targetSdk} \
  --version-code ${c.versionCode} --version-name "${c.versionName}"
javac -source 17 -target 17 -cp "${"$"}PLT/android.jar" -d "${"$"}OUT/obj" \
  "${"$"}OUT/gen/${"$"}{PKG//./\/}/R.java"
"${"$"}BT/d8" --output "${"$"}OUT/dex_out/" --lib "${"$"}PLT/android.jar" \
  --min-api ${c.minSdk} $(find "${"$"}OUT/obj" -name "*.class" | tr '\n' ' ')
cp "${"$"}OUT/base.apk" "${"$"}OUT/with_dex.apk"
cd "${"$"}OUT/dex_out" && zip -j "${"$"}OUT/with_dex.apk" classes.dex && cd -
"${"$"}BT/zipalign" -v 4 "${"$"}OUT/with_dex.apk" "${"$"}OUT/aligned.apk"
[ ! -f debug.keystore ] && keytool -genkey -v -keystore debug.keystore \
  -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android -dname "CN=SuperAI,O=SuperAI,C=US"
"${"$"}BT/apksigner" sign --ks debug.keystore --ks-pass pass:android \
  --key-pass pass:android --ks-key-alias androiddebugkey --v3-signing-enabled true \
  --out "${"$"}OUT/${"$"}PROJECT-debug.apk" "${"$"}OUT/aligned.apk"
echo "=== BUILD COMPLETE: ${"$"}OUT/${"$"}PROJECT-debug.apk ==="
""".trimIndent()
}
