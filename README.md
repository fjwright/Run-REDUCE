# Run-REDUCE

## A JavaFX GUI to run CLI versions of the REDUCE Computer Algebra System

### Francis Wright, November 2020

Run-REDUCE was originally implemented using Swing as the obsolete
project now called
[Run-REDUCE-0](https://fjwright.github.io/Run-REDUCE-0/).  The current
JavaFX version was originally called Run-REDUCE-FX but I am in the
process of dropping the "-FX" suffix.  It requires Java 11 or later
plus JavaFX 11 or later.

For further general background information please see the [Run-REDUCE
web page](https://fjwright.github.io/Run-REDUCE/).  For information
about how to install and run Run-REDUCE please see the [Install and
Run Guide](https://fjwright.github.io/Run-REDUCE/InstallAndRun.html).
For information about how to use Run-REDUCE please see the [User
Guide](https://fjwright.github.io/Run-REDUCE/UserGuide.html) (which is
also included in Run-REDUCE and easily accessible via the Help menu).

See also the [release notes for all recent
releases](https://github.com/fjwright/Run-REDUCE/releases).

Run-REDUCE should run on any platform that supports JavaFX 11 (or
later), but I can only test it on Microsoft Windows and Linux.
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

Run-REDUCE currently uses:

* a bundled copy of [KaTeX](https://katex.org) to render LaTeX output
  by code based on the REDUCE *tmprint* package when the *Typeset
  Maths* option is selected;
* an icon by [Icons8](https://icons8.com).
