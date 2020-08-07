# Run-REDUCE-FX Install and Run Guide

### Francis Wright, August 2020

Run-REDUCE-FX is an open-source JavaFX GUI to run the REDUCE Computer
Algebra System.  REDUCE must be obtained from
[SourceForge](https://sourceforge.net/projects/reduce-algebra/) and
installed separately.  Run-REDUCE-FX should find a standard REDUCE
installation automatically and not **require** any initial
configuration, at least on Microsoft Windows and Ubuntu.  With
[suitable
configuration](https://fjwright.github.io/Run-REDUCE-FX/UserGuide.html#Configure)
it **should** run on any platform that supports JavaFX 11 or later,
but I can only test on Windows 10 and 64-bit Ubuntu 18.

You need to have a Java Runtime Environment (JRE) and JavaFX libraries
installed, both version 11 or later; see below for details.

You also need to download the file
[Run-REDUCE-FX.jar](https://github.com/fjwright/Run-REDUCE-FX/releases/latest/download/Run-REDUCE-FX.jar)
and save it somewhere convenient, such as your home directory or the
directory in which you store your REDUCE projects.  You can then run
Run-REDUCE-FX as an executable file by executing the shell command

    java --module-path JavaFXlibrary --add-modules javafx.controls,javafx.fxml,javafx.web -jar Run-REDUCE-FX.jar

in the directory containing the file, where *JavaFXlibrary* represents
the full pathname of your JavaFX library.

Because this is a little tedious, I provide two batch files on the
[GitHub project page](https://github.com/fjwright/Run-REDUCE-FX) that
make it easier to run Run-REDUCE-FX.  The file *Run-REDUCE-FX.bat* is
for use on Microsoft Windows and the file *Run-REDUCE-FX* (no
extension) is for use on non-Windows platforms that provide the *bash*
shell (although I have only tested it on Ubuntu).  If you want to use
one of these batch files, download it to the same directory to which
you downloaded *Run-REDUCE-FX.jar*.  Then you can run Run-REDUCE-FX by
executing the simpler shell command

    Run-REDUCE-FX

provided you set up the `PATH_TO_FX` environment variable, as I
explain below.

There are many ways to run JavaFX applications.  Here are some
specific recommendations that I have tested.


## Install OpenJDK JRE 11 plus OpenJFX 11...

### From the Web

This applies to all platforms, although Linux users may prefer to use
a package manager as I describe below.

Visit [AdoptOpenJDK](https://adoptopenjdk.net/), click on the *Other
platforms* button (immediately below the *Latest release* button) and
check that OpenJDK 11 (LTS) is selected.  (The default JVM, HotSpot,
should be fine.)  Select your Operating System and Architecture or
just scroll down to find your platform.  On the right, choose a JRE
distribution (a JDK distribution will also work, but is much bigger
and irrelevant unless you do Java development), download and install
it.  (On Windows, I recommend the *.msi* file.)  The default
installation settings are fine although you can remove the *Associate
.jar* option unless you want it for running other Java applications
(but keep the *Add to PATH* option).

Visit [OpenJFX](https://openjfx.io/), scroll down and click on the
*Download* button.  For 64-bit platforms, scroll down, download the
appropriate *JavaFX 11 SDK* file (not the *jmods* file) and save it.
Move it somewhere appropriate and unzip it.  (On Windows, I use the
same installation folder as the JRE, so that I end up with the folder
*Program Files\AdoptOpenJDK\javafx-sdk-11.0.2*.)  Make a note of the
full pathname of the *lib* sub-directory or keep it open, since you
need its location to set up the `PATH_TO_FX` environment variable.

Beware that the JavaFX 11 releases are all for 64-bit architectures.
For 32-bit Windows, scroll down further and use the JavaFX 14 Windows
x86 SDK distribution.  This is claimed to run with JRE 11, and indeed
32-bit JRE 11 and 32-bit JavaFX 14 seem to run well together on my
32-bit Windows computer.  Note also that the latest release of REDUCE
that fully supports 32-bit Windows in version 5286 released on 1 March
2020; later releases do not install correctly.

### Using a Package Manager

The following details apply to Ubuntu.  They probably also apply to
other versions of Debian Linux, on which Ubuntu is based, and to a
lesser extent to other flavours of Linux and other platforms, such as
macOS.

Open the *Synaptic Package Manager* via *Show Applications*, search
for `jdk`, mark `openjdk-11-jre` for installation and accept its
dependencies.  Then search for `jfx`, mark `openjfx` for installation
and accept its dependencies (but you can deselect `openjfx-source`).
Then click *Apply*.  Make a note of the full pathname of the lib
directory.


## Run Run-REDUCE-FX using a Batch File on...

### Microsoft Windows

To create the `PATH_TO_FX` environment variable, open the *Start*
menu, type `env` and click on *Edit the system environment variables*.
Click on the *Environment Variables...* button towards the bottom of
the dialogue, then create a new user or system variable by clicking on
the appropriate *New...* button.  (User variables only affect you,
whereas system variables affect all users.)  Enter the variable name
`PATH_TO_FX`.  To enter the value, either use the *Browse
Directory...* button, or open the JavaFX lib folder in *File
Explorer*, click on the address bar, and copy and paste the folder
pathname.  **Enclose the folder name in double quotes.**

An easy way to run Run-REDUCE-FX using a shell command is first to
open *File Explorer* and navigate to the folder to which you
downloaded *Run-REDUCE-FX.jar*.  In the address bar, type `cmd` and
then press the *Enter* key.  This will open a Command Prompt window in
the current folder; type `Run-REDUCE-FX` and press the *Enter* key.
Or you can just double-click on the *Run-REDUCE-FX* Windows Batch File
(which is actually called *Run-REDUCE-FX.bat* but the extension is
suppressed by default). This will first open a Command Prompt window
and then run *Run-REDUCE-FX.jar*.  (I can't find a way to suppress the
Command Prompt window.)

### Linux

To create the environment variable, open your *Home* directory in
*Files*, open your *.profile* file in a text editor, and add the line

    export PATH_TO_FX=/usr/share/openjfx/lib

where */usr/share/openjfx/lib* represents the full pathname of the
JavaFX lib directory that you have just installed.  Log out and then
log back in or, until you have done so, prefix the *Run-REDUCE-FX*
command by

    PATH_TO_FX=/usr/share/openjfx/lib

followed by a space.

An easy way to run Run-REDUCE-FX using a shell command is first to
open *Files* and navigate to the directory to which you downloaded
*Run-REDUCE-FX.jar*.  Right-click in this directory and select *Open
in Terminal*.  You can now run Run-REDUCE-FX as described above by
executing the shell command

    . Run-REDUCE-FX

(Note that the above command has the form dot space filename, where
dot is a short name for the *source* command.  If you set the file
permissions to make Run-REDUCE-FX executable, you can run it as
`./Run-REDUCE-FX`, but this doesn't gain much!)


## Known Issues

On my main Windows computer, which has an HD display, I find that I
need to use a larger REDUCE I/O font size than I would expect.  (A
font with the same numerical size appears smaller than in Run-REDUCE.)
This may be because JavaFX font sizes don't reflect display scaling.

On my Linux computer, which currently runs Ubuntu 18.04.4 LTS, by
default a warning message appears and some dialogues jump when they
first appear.  In order to avoid these problems, I have included the
option

    -Djdk.gtk.version=2

in the *Run-REDUCE-FX* batch file.  This may not be necessary on all
platforms and will, I hope, cease to be necessary at some future date,
so you might like to experiment with removing it.

I have been advised that on Kubuntu 18.04.4 it may be necessary to
install the Gnome 2 theme Adwaita.  You may also find that the About
and error dialogue boxes are too small by default.
