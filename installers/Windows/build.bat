rem Use jpackage to build a Run-REDUCE installer for Windows
rem User's Guide: https://docs.oracle.com/en/java/javase/15/jpackage/
rem Man Page: https://docs.oracle.com/en/java/javase/15/docs/specs/man/jpackage.html
rem Requires JDK 14+ and WiX 3.0+

rem The launcher Run-REDUCE.exe does not yet work!
rem --type app-image creates only the application image for testing.
rem --add-modules javafx.fxml,javafx.web is probably not necessary.

jpackage --name Run-REDUCE --app-version 2.65 ^
--module Run.REDUCE/fjwright.runreduce.RunREDUCE ^
--module-path "D:\Program Files\AdoptOpenJDK\jdk-14.0.2.12-hotspot\jmods";%PATH_TO_FX%;C:\Users\franc\IdeaProjects\Run-REDUCE\out\production ^
--add-modules javafx.fxml,javafx.web ^
--type app-image ^
--icon RR-icon-128.ico ^
--description "A JavaFX GUI to run the REDUCE CAS" ^
--vendor "Francis Wright -- https://github.com/fjwright/Run-REDUCE" ^
--copyright "2020, Francis Wright, All rights reserved" ^
--verbose --temp temp
