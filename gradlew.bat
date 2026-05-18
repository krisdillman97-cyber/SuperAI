@rem Gradle startup script for Windows
@if "%DEBUG%"=="" @echo off
setlocal
set JAVA_HOME_KEY=HKLM\SOFTWARE\JavaSoft\Java Development Kit
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar
"%JAVA_HOME%\bin\java.exe" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
