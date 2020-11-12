# Run-REDUCE Install and Run Guide

### Francis Wright, November 2020

Run-REDUCE is an open-source JavaFX GUI to run the REDUCE Computer
Algebra System.  REDUCE must be obtained from
[SourceForge](https://sourceforge.net/projects/reduce-algebra/) and
installed separately.  Run-REDUCE should find a standard REDUCE
installation automatically and not **require** any initial
configuration, at least on Microsoft Windows and Linux.  With
[suitable
configuration](https://fjwright.github.io/Run-REDUCE/UserGuide.html#Configure)
it **should** run on any platform that supports JavaFX 11 or later,
but I can only test on 64-bit Windows 10, Ubuntu 18/20 and Fedora 32.

You need to have a Java Runtime Environment (JRE) and JavaFX libraries
installed, both version 11 or later (preferably the latest release);
see below for details.

You also need to download the file
[Run-REDUCE-FX.jar](https://github.com/fjwright/Run-REDUCE/releases/latest/download/Run-REDUCE-FX.jar)
(by clicking on the link) and save it somewhere convenient, such as
your home directory or the directory in which you store your REDUCE
projects.  You can then run Run-REDUCE by executing the shell command

``` shell
java --module-path JavaFXlibrary --add-modules javafx.controls,javafx.fxml,javafx.web -jar Run-REDUCE-FX.jar
```

in the directory containing the file, where *JavaFXlibrary* represents
the full pathname of your JavaFX library.

Because this is a little tedious, I provide two batch files that make
it easier to run Run-REDUCE.  The file
[Run-REDUCE-FX.bat](https://github.com/fjwright/Run-REDUCE/releases/download/v1.6/Run-REDUCE-FX.bat)
is for use on Microsoft Windows and the file
[Run-REDUCE-FX](https://github.com/fjwright/Run-REDUCE/releases/download/v1.6/Run-REDUCE-FX)
(no extension) is for use on Linux.  If you want to use one of these
batch files, download it (by clicking on the link) to the same
directory to which you downloaded *Run-REDUCE-FX.jar*.  Then you can
run Run-REDUCE by executing the simpler shell command

``` shell
Run-REDUCE-FX
```

provided you set up the `PATH_TO_FX` environment variable, as I
explain below.

There are many ways to run JavaFX applications.  Here are some
specific recommendations that I have tested.


## Install OpenJDK JRE ...

### From the Web

This applies to all platforms, although Linux users may prefer to use
a package manager as I describe below.

Visit [AdoptOpenJDK](https://adoptopenjdk.net/), click on the *Other
platforms* button (immediately below the *Latest release* button) and
select *OpenJDK nn (Latest)* with the highest number *nn* (or you can
currently use Java 11 if you prefer).  The default JVM, HotSpot,
should be fine.  Select your Operating System and Architecture or just
scroll down to find your platform.  On the right, choose a JRE
distribution (a JDK distribution will also work, but is much bigger
and irrelevant unless you do Java development), download and install
it.  (On Windows, I recommend the *.msi* file.)  The default
installation settings are fine although you can remove the *Associate
.jar* option unless you want it for running other Java applications
(but keep the *Add to PATH* option).

### Using a Package Manager on Ubuntu

The following details probably apply to all versions of Debian Linux,
on which Ubuntu is based.

Open the *Synaptic Package Manager* via *Show Applications*.  (If it
is not there, open *Help*, click on *Install applications* under
*Install & remove software*, click on *Synaptic* and then click on the
*install* link.)  Search for `openjdk`, find the version of
*openjdk-nn-jre* with the highest number *nn* (or you can currently
use *openjdk-11-jre* if you prefer), mark it for installation and
accept its dependencies.

### Using a Package Manager on Fedora

The following details probably apply to all versions of Red Hat Linux,
on which Fedora is based.

Fedora 32 has Java 8 installed by default but you can install and use
the latest release of Java by executing the following commands in a
terminal (assuming *x86_64* architecture):

``` shell
sudo dnf install java-latest-openjdk.x86_64
sudo alternatives --config java
```

and select the new version.

Alternatively, you might prefer to use *dnfdragora*, which is a
graphical version of *dnf*.  (You may first need to install it, using
either the *Software* app available in *Activities* or the *dnf*
command).


## Install OpenJFX ...

This is my recommendation for all platforms because appropriate
versions of OpenJFX for some recent versions of Linux via package
managers are either broken (e.g. for Ubuntu 20.04) or unavailable
(e.g. for Fedora 32)!

Visit [OpenJFX](https://openjfx.io/), scroll down and click on the
*Download* button.  Scroll down to *Latest Release* (or you can
currently use JavaFX 11 if you prefer), download the appropriate
*JavaFX SDK* file (not the *jmods* file) and save it.  Move it
somewhere appropriate (anywhere should work) and unzip it.  Make a
note of the full pathname of the *lib* sub-directory or copy it, since
you need it to set up the `PATH_TO_FX` environment variable; see
below.


## Run Run-REDUCE using a Batch File on...

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

An easy way to run Run-REDUCE using a shell command is first to open
*File Explorer* and navigate to the folder to which you downloaded
*Run-REDUCE-FX.jar*.  In the address bar, type `cmd` and then press
the *Enter* key.  This will open a Command Prompt window in the
current folder; type `Run-REDUCE-FX` and press the *Enter* key.  Or
you can just double-click on the *Run-REDUCE-FX* Windows Batch File
(which is actually called *Run-REDUCE-FX.bat* but the extension is
suppressed by default). This will first open a Command Prompt window
and then run *Run-REDUCE-FX.jar*.  (I can't find a way to suppress the
Command Prompt window.)

### Linux

To create the environment variable, open your profile (or shell
configuration) file (e.g. *~/.profile* in Ubuntu or *~/.bash_profile*
in Fedora) in a text editor and add the line

``` shell
export PATH_TO_FX=path-to-openjfx/lib
```

where *path-to-openjfx* represents the full pathname of the JavaFX
directory that you have just installed.  Log out and then log back in
or, until you have done so, prefix the *Run-REDUCE-FX* command by

``` shell
PATH_TO_FX=path-to-openjfx/lib
```

followed by a space.

An easy way to run Run-REDUCE using a shell command is first to open
*Files* and navigate to the directory to which you downloaded
*Run-REDUCE-FX.jar*.  Right-click in this directory and select *Open
in Terminal*.  You can now run Run-REDUCE as described above by
executing the shell command

``` shell
. Run-REDUCE-FX
```

(Note that the above command has the form dot space filename, where
dot is a short name for the *source* command.  If you set the file
permissions to make Run-REDUCE-FX executable, you can run it as
`./Run-REDUCE-FX`, but this doesn't gain much!)


## Known Issues

On my main Windows computer, which has an HD display, I find that I
need to use a larger REDUCE I/O font size than I would expect.  (A
font with the same numerical size appears smaller than in Run-REDUCE.)
This may be because JavaFX font sizes don't reflect display scaling.

On Ubuntu 18 and 20, by default, two warning message appears and on
Ubuntu 18 some dialogues jump when they first appear.  The warning
about *libcanberra-gtk-module* can be avoided by using *Synaptic* to
install *libcanberra-gtk-module* (or you can just ignore it).  In
order to avoid the other problems, I have included the option

``` shell
-Djdk.gtk.version=2
```

in the *Run-REDUCE-FX* batch file.  This may not be necessary on all
platforms and will, I hope, cease to be necessary at all at some
future date, so you might like to experiment with removing it.

I have been advised that on Kubuntu 18.04.4 it may be necessary to
install the Gnome 2 theme Adwaita.  You may also find that the About
and error dialogue boxes are too small by default.

If *Run-REDUCE* misbehaves or crashes, try including the option

``` shell
-Dprism.order=sw
```

which tells Java to use software display rendering.  On Fedora, you
may find that the *Gnome Classic* desktop environment works better
than *Gnome*.  (One way to select your desktop environment is by using
the settings icon on the login screen.)
