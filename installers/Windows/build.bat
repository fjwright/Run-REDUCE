@echo off
rem Use jpackage to build a Run-REDUCE installer for Windows.
rem User's Guide: https://docs.oracle.com/en/java/javase/15/jpackage/
rem Man Page: https://docs.oracle.com/en/java/javase/15/docs/specs/man/jpackage.html
rem Requires JDK 14+ and WiX 3.0+. Uses JAVA_HOME.

rem --type app-image creates only the application image for testing.

jpackage --name Run-REDUCE --app-version 2.65 ^
--module Run.REDUCE/fjwright.runreduce.RunREDUCE ^
--module-path %PATH_TO_FX_MODS%;..\..\out\production ^
--type msi ^
--icon RR-icon-128.ico ^
--description "A JavaFX GUI to run the REDUCE CAS" ^
--vendor "Francis Wright" ^
--copyright "2020, Francis Wright, All rights reserved" ^
--win-upgrade-uuid 83c944a0-27b3-43a2-99ff-455d746acabc ^
--win-shortcut --win-dir-chooser ^
--win-menu --win-menu-group Reduce
