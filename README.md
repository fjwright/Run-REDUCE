# Run-REDUCE

## A JavaFX GUI to run any CLI version of the REDUCE Computer Algebra System

### Francis Wright, April 2021

![GitHub](https://img.shields.io/github/license/fjwright/Run-REDUCE)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/fjwright/Run-REDUCE)
![GitHub Release Date](https://img.shields.io/github/release-date/fjwright/Run-REDUCE)

I originally implemented Run-REDUCE using Swing as the obsolete
project now called
[Run-REDUCE-0](https://fjwright.github.io/Run-REDUCE-0/) and I called
the current JavaFX version Run-REDUCE-FX, but I have since dropped the
"-FX" suffix.  Run-REDUCE requires Java and JavaFX version 11 or
later, although I recommend using version 16 since I currently build
Run-REDUCE using Java 16, set to language level 11, plus JavaFX 16.
Future versions may require Java 16 or later.

However, if possible, I recommend using the self-contained installers
available for 64-bit Windows 10 and Gnu/Linux distributions based on
Debian (e.g. Ubuntu) or Red Hat (e.g. Fedora), which include all
required Java and JavaFX support and hide the implementation details.

For further general background information please see the [Run-REDUCE
web page](https://fjwright.github.io/Run-REDUCE/).  For information
about how to install and run Run-REDUCE please see the [Install and
Run Guide](https://fjwright.github.io/Run-REDUCE/InstallAndRun.html).
For information about how to use Run-REDUCE please see the [User
Guide](https://fjwright.github.io/Run-REDUCE/UserGuide.html) (which is
also included in Run-REDUCE and easily accessible via the Help menu).

See also the [release notes for all recent
version](https://github.com/fjwright/Run-REDUCE/releases).

Run-REDUCE should run on any platform that supports JavaFX 11 (or
later), but I can only test it on Microsoft Windows and Gnu/Linux.
(Whilst Java is portable, filesystem structures, installation
conventions and display systems are not!)

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
