# Run-REDUCE-FX

## A JavaFX GUI to run any CLI version of the REDUCE Computer Algebra System

### Francis Wright, May 2020

Run-REDUCE-FX is a re-implementation of
[Run-REDUCE](https://fjwright.github.io/Run-REDUCE/) using JavaFX
instead of Swing.  It does **not** (yet) provide typeset-quality
display of mathematical notation.

For information about how to install and run Run-REDUCE-FX please see
the [Installation Guide](https://fjwright.github.io/Run-REDUCE-FX/InstallationGuide.html).
For information about how to use Run-REDUCE-FX please see the [User
Guide](https://fjwright.github.io/Run-REDUCE-FX/UserGuide.html) (which is also included
in Run-REDUCE-FX and easily accessible via the Help menu).

Run-REDUCE-FX should run on any platform that supports JavaFX 8 (or
later), but I can only test it on Microsoft Windows and 64-bit Ubuntu.
(Whilst Java is portable, filesystem structures and
installation conventions are not!)

REDUCE itself is an open source project available from
[SourceForge](https://sourceforge.net/projects/reduce-algebra/).  I'm
releasing Run-REDUCE-FX under the [BSD 2-Clause License](LICENSE), mainly
because it's the license used by REDUCE.

This project is set up for development using [IntelliJ
IDEA](https://www.jetbrains.com/idea/) with Run-REDUCE-FX as the
top-level directory.
