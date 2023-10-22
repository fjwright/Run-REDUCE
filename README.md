# Run-REDUCE

## A JavaFX GUI to run any CLI version of the REDUCE Computer Algebra System

### Francis Wright, October 2023

![GitHub](https://img.shields.io/github/license/fjwright/Run-REDUCE)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/fjwright/Run-REDUCE)
![GitHub Release Date](https://img.shields.io/github/release-date/fjwright/Run-REDUCE)

I recommend using the self-contained installers available for 64-bit
Windows and Gnu/Linux distributions based on Debian (e.g. Ubuntu) or
Red Hat (e.g. Fedora), which include all required Java and JavaFX
support.  Alternatively, a JAR file is available that should run on
any platform that supports Java and JavaFX.

This release was built using Java 16 and JavaFX 16, and these or later
versions are required to run the Run-REDUCE JAR.  I recommend using
versions 17 or 21, which have long term support, and I will probably
move to using versions 17 for the next release.

For further general background information please see the [Run-REDUCE
web page](https://fjwright.github.io/Run-REDUCE/).  For information
about how to install and run Run-REDUCE please see the [Install and
Run Guide](https://fjwright.github.io/Run-REDUCE/InstallAndRun.html).
For information about how to use Run-REDUCE please see the [User
Guide](https://fjwright.github.io/Run-REDUCE/UserGuide.html) (which is
also included in Run-REDUCE and easily accessible via the Help menu).

See also the [release notes for all recent
versions](https://github.com/fjwright/Run-REDUCE/releases).

Run-REDUCE should run on any platform that supports JavaFX 16 (or
later), but I can currently only test it on Microsoft Windows 10 and
Gnu/Linux.

REDUCE itself is an open source project available from
[SourceForge](https://sourceforge.net/projects/reduce-algebra/).  I'm
releasing Run-REDUCE under the [BSD 2-Clause License](LICENSE), mainly
because it's the license used by REDUCE.

This project is set up for development using [IntelliJ
IDEA](https://www.jetbrains.com/idea/) with Run-REDUCE as the
top-level directory.


### Acknowledgements

Run-REDUCE uses a bundled copy of [KaTeX](https://katex.org) to render
LaTeX output by code based on the REDUCE *tmprint* package when the
*Typeset Maths* option is selected.
