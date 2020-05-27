# Run-REDUCE-FX Installation Guide

### Francis Wright, May 2020

Run-REDUCE-FX is an open-source JavaFX GUI to run the REDUCE Computer
Algebra System.  REDUCE must be obtained from
[SourceForge](https://sourceforge.net/projects/reduce-algebra/) and
installed separately.  Run-REDUCE-FX should find a standard REDUCE
installation automatically and not **require** any initial
configuration, at least on Microsoft Windows and Ubuntu.  With
suitable configuration it **should** run on any platform that supports
JavaFX 8 or later, but I can only test on Windows 10 and 64-bit Ubuntu
18.

You also need a suitable Java environment and I recommend installing
Java from [java.com](https://www.java.com/), which in my experience seems to be the easiest to
install and to work best.  (To be more specific, you need to have a
Java Runtime Environment (JRE) and JavaFX libraries installed, both
version 8 or later.  The version of Java available from [java.com](https://www.java.com/) is
Oracle JRE 8, which includes JavaFX 8, whereas other JRE versions
normally do not, in which case you need to install JavaFX separately.)

You also need to download the file *Run-REDUCE-FX.jar*.  The easiest
way to do this is to click on the download link at the top of the
[Run-REDUCE-FX web page](https://fjwright.github.io/Run-REDUCE-FX/).
(Alternatively, you can click on the *release* tab on the [GitHub
project page](https://github.com/fjwright/Run-REDUCE-FX), then click
on *Run-REDUCE-FX.jar* under *Assets*.)  Save *Run-REDUCE-FX.jar*
somewhere convenient, such as your home directory or the directory in
which you store your REDUCE projects.  You can then run Run-REDUCE-FX
as an executable file by double-clicking on it or by executing the
shell command

    java -jar Run-REDUCE-FX.jar

in the directory containing the file.  The latter approach has the
advantage that any error messages will be displayed in the shell
window.

Here are some specific recommendations that I have tested.


## Microsoft Windows

Run-REDUCE-FX behaves very much like a drop-in replacement for
Run-REDUCE.  So if you ran the latter on your computer then you can
probably run the former in the same way.  Installing Java from
[java.com](https://www.java.com/) sets it up so that
*Run-REDUCE-FX.jar* runs automatically when you open it, e.g. by
double-clicking on it.

An easy way to run Run-REDUCE-FX using a shell command is first to
open *File Explorer* and navigate to the folder to which you
downloaded *Run-REDUCE-FX.jar*.  In the address bar, type `cmd` and
then press the *Return* key.  This will open a Command Prompt window
in the current folder.


## Ubuntu and other Platforms

The following details apply to Ubuntu.  They probably also apply to
other versions of Debian Linux, on which Ubuntu is based, and to a
lesser extent to other flavours of Linux and other platforms, such as
macOS.

Visit java.com, click on *Java Download*, then select *Linux* or
*Linux x64* as appropriate.  There are installation instructions on
this web site, which you may find useful.  (On versions of Linux based
on Red Hat, you may prefer to select the appropriate RPM distribution
and install that.)  Download the appropriate *tar.gz* archive file and
move it to an appropriate installation directory, which I will assume
is your home directory *~*.  Extract the contents of the archive,
which you can do easily by double-clicking on it to run *Archive
Manager* (or see the instructions on the web).

An easy way to run Run-REDUCE-FX using a shell command is first to
open *Files* and navigate to the directory to which you downloaded
*Run-REDUCE-FX.jar*.  Right-click on this directory and select *Open
in Terminal*.  You can now run *Run-REDUCE-FX.jar* by executing the
shell command

    ~/jre1.8.0_251/bin/java -jar Run-REDUCE-FX.jar

where `jre1.8.0_251` is the name of the Java directory you just
installed.  Alternatively, if you add the directory
`~/jre1.8.0_251/bin` to your PATH, you can run Run-REDUCE-FX by
executing the simpler shell command

    java -jar Run-REDUCE-FX.jar

## Known Issues

On my main Windows computer, which has an HD display, I find that I
need to use a larger REDUCE I/O font size than I would expect.  (A
font with the same numerical size appears smaller than in Run-REDUCE.)
This may be because JavaFX font sizes don't reflect display scaling.

On my Linux computer, which currently runs Ubuntu 18.04.4 LTS, I
notice the following issues, which may arise more generally.

I find that any attempt to access documentation via the Help menu just
hangs. (This seems to happen in the *java.awt.Desktop.open* and
*java.awt.Desktop.browse* methods, which ultimately hang in native
methods in *sun.awt.X11.XDesktopPeer*.)

I also get a warning when the application starts.  Adding
 
    -Djdk.gtk.version=2

before `-jar` in the command used to run Run-REDUCE-FX avoids this
warning, but you may then find that dialogues do not appear where you
expect them.
