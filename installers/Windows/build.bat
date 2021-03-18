@echo off
rem Use jpackage to build a Run-REDUCE installer for Windows.
rem User's Guide: https://docs.oracle.com/en/java/javase/16/jpackage/
rem Man Page: https://docs.oracle.com/en/java/javase/16/docs/specs/man/jpackage.html
rem Requires JDK 14+ and WiX 3.0+.
rem Requires (e.g.) JAVA_HOME = "D:\Program Files\Java\jdk-16"
rem and PATH_TO_FX_MODS = "D:\Program Files\AdoptOpenJDK\javafx-jmods-16"
rem and PATH_TO_FX = "D:\Program Files\Java\javafx-16\lib" (to run Version)

rem --type app-image creates only the application image for testing.
rem --verbose enables verbose output.

echo java version
    java --version
echo jpackage version
    jpackage --version
echo jlink version
    jlink --version
echo JAVA_HOME = %JAVA_HOME%
echo PATH_TO_FX_MODS = %PATH_TO_FX_MODS%

for /f %%v in ('java --module-path %PATH_TO_FX%^;..\..\out\production ^
--module Run.REDUCE/fjwright.runreduce.Version') do set VERSION=%%v

echo Run-REDUCE version = %VERSION%
echo ---

jpackage --name Run-REDUCE --app-version %VERSION% ^
--module-path "%PATH_TO_FX_MODS%;..\..\out\production" ^
--module Run.REDUCE/fjwright.runreduce.RunREDUCE ^
--type msi ^
--icon RR-icon-128.ico ^
--description "A JavaFX GUI to run the REDUCE CAS" ^
--vendor "Francis Wright" ^
--copyright "2021, Francis Wright, All rights reserved" ^
--win-upgrade-uuid 83c944a0-27b3-43a2-99ff-455d746acabc ^
--win-shortcut --win-dir-chooser ^
--win-menu --win-menu-group Reduce %*
