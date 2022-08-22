Build from Source
=================

If you just want to use Run-REDUCE then I recommend the [latest
pre-built
release](https://github.com/fjwright/Run-REDUCE/releases/latest).
Otherwise, I recommend that you build the source files corresponding
to the latest release, which will be tagged with the latest version.
Beware that non-release source files may be inconsistent and may not
build and/or run correctly.

This project is set up for development using [IntelliJ
IDEA](https://www.jetbrains.com/idea/) with `Run-REDUCE` as the root
directory and the first set of build instructions below uses IntelliJ
IDEA. The second set uses the command line, and the same directory
structure as used by IntelliJ IDEA, but you could change this provided
you keep everything self-consistent.  However you build, the following
initial steps are required:

* Install Java and JavaFX and set up the `PATH_TO_FX` environment
variable, as explained in the [Install and Run
Guide](https://fjwright.github.io/Run-REDUCE/InstallAndRun.html).  You
need a Java Development Kit (JDK) to build a Java program (whereas a
Java Runtime Environment (JRE) is sufficient to run it once built.)  I
recommend using Java 16 or later, and the same version of JavaFX; note
that building source later than version 3.0 **requires** Java 16 or
later.

* Download and unpack a Run-REDUCE release source file archive or
clone this repository.

Note that this project uses [modular
Java](https://www.oracle.com/uk/corporate/features/understanding-java-9-modules.html),
primarily in order to minimize the size of the pre-built release
files.  See also the [JavaFX website](https://openjfx.io/) for useful
guidance on building projects using JavaFX.

The following instructions work for me using Java 16 and JavaFX 16 on
Microsoft Windows 10 and Ubuntu 20.

Build and Run using IntelliJ IDEA
---------------------------------

Open the project based on the directory `Run-REDUCE` that you have
just created.

### Configure IntelliJ IDEA

You will need to configure IntelliJ IDEA for the JDK and JavaFX
libraries you have installed, and other details of your platform.

To configure building, open the *Project Structure* dialogue.

* Under *Project*, set the Java SDK you are using.

* Under *Modules*, select *Sources*, mark directory `resources` as
*Resources* with relative output path `fjwright/runreduce`, and mark
directories `installers` and `docs` as *Excluded*.

* Under *Libraries*, click + then select *Java*, your JavaFX SDK root
directory, and then `lib`.  Agree to having this library added to
module `Run-REDUCE`.  Optionally, rename `lib` to something more
informative, such as `libJFX`.

* Under *Artifacts*, click + then select *JAR* and then *From modules
with dependencies...*.  Finally, remove all the *Extracted...* items
under *Run-REDUCE.jar*.

Note that I have checked in the run configurations that I use on
Windows, which will probably be more or less inappropriate for you.
Therefore, to configure running, edit the *Run/Debug Configurations*
to use the correct path and directory separators, JavaFX path and
project home path for your platform.

### Build and Run the Project

To build the project, click on the *Build Project* button or menu
item.  This will create the directory `out/production` if necessary
and populate it by compiling the `.java` files and copying the other
required files in the `src` directory.

To run the project using the files in the directory `out/production`,
select the *Run/Debug Configuration* `RunREDUCE` and then click on the
*Run 'RunREDUCE'* button or menu item.  This is a useful check before
building a JAR or installer.  This should pop up Run-REDUCE in its own
window.

### Build and Run the JAR

To build the JAR, open the *Build Artifacts...* item in the *Build*
menu and then click on *Build* in the *Action* pop-up menu.  This will
build the project if necessary, create the directory `out/artifacts`
if necessary and populate it by building the file `Run-REDUCE.jar`.

To run the JAR, select the *Run/Debug Configuration* `Run-REDUCE.jar`
and then click on the *Run 'Run-REDUCE.jar'* button or menu item.
This should pop up Run-REDUCE in its own window.

Build and Run using the Command Line
------------------------------------

The commands below assume that the JDK bin directory is in your search
path and are intended to be run in `bash`.  (On Windows I use Cygwin.
However, on Windows you need either to remove the quotes from the
value of the environment variable `PATH_TO_FX` and instead use
`"$PATH_TO_FX"` in the commands below, or to use the Windows command
prompt and use `%PATH_TO_FX%` instead of `$PATH_TO_FX` and a semicolon
instead of a colon in the commands below.)

Make the directory `Run-REDUCE` that you have just created your
current working directory.

### Build and Run the Project

To build the project, execute the following command, where the
`-encoding` argument is required on Windows but not on Linux, and
probably not on other platforms.

```shell
javac --module-path=$PATH_TO_FX --add-modules=javafx.fxml,javafx.web \
    -d out/production/Run-REDUCE -encoding UTF-8 \
    src/{,fjwright/runreduce/{,*/}}*.java
```

Then copy the other files required using the following commands.

```shell
cp -r src/META-INF out/production/Run-REDUCE
cp src/fjwright/runreduce/*.fxml out/production/Run-REDUCE/fjwright/runreduce
cp src/fjwright/runreduce/functions/*.fxml out/production/Run-REDUCE/fjwright/runreduce/functions
cp src/fjwright/runreduce/templates/*.fxml out/production/Run-REDUCE/fjwright/runreduce/templates
cp -r resources/* out/production/Run-REDUCE/fjwright/runreduce
```

To run the project using the files in the directory `out/production`,
execute the following command.  This is a useful check before building
a JAR or installer.  (On Windows, replace the colon with an escaped
semicolon.)

```shell
java --module-path=$PATH_TO_FX:out/production -m Run.REDUCE/fjwright.runreduce.RunREDUCE
```

### Build and Run the JAR

To build a JAR, build the project as described above, then execute the
following commands.  (Note the trailing dot.)

```shell
mkdir -p out/artifacts/Run_REDUCE_jar
jar --create --file=out/artifacts/Run_REDUCE_jar/Run-REDUCE.jar \
    --manifest=out/production/Run-REDUCE/META-INF/MANIFEST.MF \
    -C out/production/Run-REDUCE .
```

To run the JAR, execute the following command.

```shell
java --module-path=$PATH_TO_FX --add-modules=javafx.fxml,javafx.web \
    -jar out/artifacts/Run_REDUCE_jar/Run-REDUCE.jar
```

Building Installers
-------------------

This is a command-line process and requires that the project first be
built, as described above.  (It does not use a JAR.)  An installer can
be built only on the platform that it targets, and the build process
uses the standard installer-builder for that platform, which must also
be installed.  Java (since JDK 14) provides a program called
`jpackage` that interfaces between a Java application build and an
installer-builder.  The [JDK Enhancement
Proposal](https://openjdk.org/jeps/392) provides useful background,
but see also the jpackage manual entry for the version of Java you are
using,
e.g. https://docs.oracle.com/en/java/javase/18/docs/specs/man/jpackage.html.

I provide directories under `installers` called `Linux` and `Windows`,
which contain native batch files.  The Linux batch file should
automatically build an `.rpm` or `.deb` installer when run on an
appropiate version of Linux, e.g. Fedora or Ubuntu, respectively.  The
Windows batch file should automatically build an `.msi` installer when
run using the Windows command prompt.  See the comments in the batch
files for further details and setup requirements.  These batch files
should provide a model for creating installers on other platforms.
Note that different platforms seem to require image files in different
formats to serve as installer icons.
