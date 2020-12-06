rem Use jpackage to build a Run-REDUCE installer for Windows
rem User's Guide: https://docs.oracle.com/en/java/javase/15/jpackage/
rem Man Page: https://docs.oracle.com/en/java/javase/15/docs/specs/man/jpackage.html
rem Requires JDK 14+ and WiX 3.0+

rem The launcher Run-REDUCE.exe does not yet work!
rem --type app-image creates only the application image for testing.

jpackage --name Run-REDUCE --app-version 2.63 ^
--input ..\..\out\artifacts\Run_REDUCE_jar ^
--main-jar Run-REDUCE.jar ^
--module-path %PATH_TO_FX% ^
--add-modules javafx.controls,javafx.fxml,javafx.web ^
--type app-image ^
--icon RR-icon-128.ico ^
--description "A JavaFX GUI to run the REDUCE CAS" ^
--vendor "Francis Wright -- https://github.com/fjwright/Run-REDUCE" ^
--copyright "Copyright (c) 2020, Francis Wright, All rights reserved, BSD 2-Clause License" ^
--verbose
