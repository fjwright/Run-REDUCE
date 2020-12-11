@echo off

rem This batch file and Run-REDUCE.jar must be in the same folder.
rem The Java bin directory must be in PATH and the environment
rem variable PATH_TO_FX must be set to the JavaFX lib folder.
rem Then Run-REDUCE.jar can be run from anywhere by running this batch file.
rem Any command-line arguments are passed on to Run-REDUCE.jar.

rem Note that %~p0 expands to the path component (with a final \) of this filename.

java --module-path %PATH_TO_FX% --add-modules javafx.fxml,javafx.web -jar "%~p0Run-REDUCE.jar" %*
