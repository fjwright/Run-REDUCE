# Run-REDUCE-FX Install and Run Guide

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
Java from [java.com](https://www.java.com/), which in my experience
seems to be the easiest to install and to work best.  (To be more
specific, you need to have a Java Runtime Environment (JRE) and JavaFX
libraries installed, both version 8 or later.  The version of Java
available from [java.com](https://www.java.com/) is Oracle JRE 8,
which has JavaFX 8 built in, whereas other JRE versions normally do
not, in which case you need to install JavaFX separately.  See below
for more specific details.)

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

There are many ways to run JavaFX applications.  Here are some
specific recommendations that I have tested.


## Microsoft Windows

Run-REDUCE-FX behaves very much like a drop-in replacement for
Run-REDUCE.  So if you ran the latter on your computer then you can
probably run the former in the same way.  Installing Java from
[java.com](https://www.java.com/) sets it up so that
*Run-REDUCE-FX.jar* runs automatically when you open it, e.g. by
double-clicking on it.

Alternatively, an easy way to run Run-REDUCE-FX using a shell command
is first to open *File Explorer* and navigate to the folder to which
you downloaded *Run-REDUCE-FX.jar*.  In the address bar, type `cmd`
and then press the *Return* key.  This will open a Command Prompt
window in the current folder.


## Ubuntu and other Platforms

The following details apply to Ubuntu.  They probably also apply to
other versions of Debian Linux, on which Ubuntu is based, and to a
lesser extent to other flavours of Linux and other platforms, such as
macOS.

### Install Oracle JRE 8 from the Web

Visit java.com, click on *Java Download*, then select *Linux* or
*Linux x64* as appropriate.  There are installation instructions on
this web site, which you may find useful.  (On versions of Linux based
on Red Hat, you may prefer to select the appropriate RPM distribution
and install that.)  Download the appropriate *tar.gz* archive file and
move it to an appropriate installation directory, which I will assume
is your home directory *~*.  Extract the contents of the archive,
which you can do easily by double-clicking on it to run *Archive
Manager* (or see the instructions at
[java.com](https://www.java.com/)).

An easy way to run Run-REDUCE-FX using a shell command is first to
open *Files* and navigate to the directory to which you downloaded
*Run-REDUCE-FX.jar*.  Right-click on this directory and select *Open
in Terminal*.  You can now run *Run-REDUCE-FX.jar* by executing the
shell command

    ~/jre1.8.0_251/bin/java -jar Run-REDUCE-FX.jar

where `jre1.8.0_251` is the name of the Java directory you just
installed.  Alternatively, if you add the directory
`~/jre1.8.0_251/bin` to your `PATH`, you can run Run-REDUCE-FX by
executing the simpler shell command

    java -jar Run-REDUCE-FX.jar

### Install OpenJDK JRE 11 plus OpenJFX 11 using a Package Manager

Open the *Synaptic Package Manager* via *Show Applications*, search
for `jdk`, mark `openjdk-11-jre` for installation and accept its
dependencies.  Then search for `jfx`, mark `openjfx` for installation
and accept its dependencies (but you can deselect `openjfx-source`).
Then click *Apply*.

You can now run Run-REDUCE-FX as described above, except that the
shell command is

    java --module-path /usr/share/openjfx/lib --add-modules javafx.controls,javafx.fxml -jar Run-REDUCE-FX.jar

You could simplify this command by putting it into a shell script file
or making it into a shell alias.

### Install OpenJDK JRE 11 plus OpenJFX 11 from the Web

Visit [AdoptOpenJDK](https://adoptopenjdk.net/), click on the *Other
platforms* button (immediately below the *Latest release* button) and
select OpenJDK 11 (LTS).  (The default JVM, HotSpot, should be fine.)
Select and/or scroll down to find your platform.  On the right, choose
the JRE version (the JDK version will also work, but is much bigger
and irrelevant unless you do Java development), download an
appropriate installation file and install it.  (The default
installation settings should be fine.)

Visit [OpenJFX](https://openjfx.io/), scroll down and click on the
*Download* button.  Scroll down and download JavaFX Linux SDK, then
set it up by [following these
instructions](https://openjfx.io/openjfx-docs/#install-javafx).  An
easy way to run Run-REDUCE-FX using a shell command is first to open
*Files* and navigate to the directory to which you downloaded
`Run-REDUCE-FX.jar`.  Right-click on this directory and select *Open
in Terminal*.  You can now run Run-REDUCE-FX as described above by
executing the shell command

    java --module-path /usr/share/openjfx/lib --add-modules javafx.controls,javafx.fxml -jar Run-REDUCE-FX.jar

## Known Issues

On my main Windows computer, which has an HD display, I find that I
need to use a larger REDUCE I/O font size than I would expect.  (A
font with the same numerical size appears smaller than in Run-REDUCE.)
This may be because JavaFX font sizes don't reflect display scaling.

On my Linux computer, which currently runs Ubuntu 18.04.4 LTS, access
to PDF files via the Help menu does not work well, so I currently
suppress the menu items affected on all platforms other than Windows.
Using Java 8, alerts are truncated; using Java 11 alerts and other
dialogues jump unpleasantly when they are first opened although they
are not truncated.
