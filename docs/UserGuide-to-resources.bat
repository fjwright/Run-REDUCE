@echo off
rem Copy file only if source time newer than destination time.
rem No prompting to overwrite an existing destination file.
xcopy docs\UserGuide.html resources\ /D /Y
