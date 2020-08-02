# Run-REDUCE-FX

## A JavaFX GUI to run any CLI version of the REDUCE Computer Algebra System

### Francis Wright, August 2020

Run-REDUCE-FX is a re-implementation of
[Run-REDUCE](https://fjwright.github.io/Run-REDUCE/) using JavaFX
instead of Swing.  It does **not** (yet) provide typeset-quality
display of mathematical notation.  The latest version of Run-REDUCE-FX
requires Java 11 or later plus JavaFX 11 or later.

For further general background information please see the
[Run-REDUCE-FX web page](https://fjwright.github.io/Run-REDUCE-FX/).
For information about how to install and run Run-REDUCE-FX please see
the [Install and Run
Guide](https://fjwright.github.io/Run-REDUCE-FX/InstallAndRun.html).
For information about how to use Run-REDUCE-FX please see the [User
Guide](https://fjwright.github.io/Run-REDUCE-FX/UserGuide.html) (which
is also included in Run-REDUCE-FX and easily accessible via the Help
menu).

Run-REDUCE-FX should run on any platform that supports JavaFX 11 (or
later), but I can only test it on Microsoft Windows and 64-bit Ubuntu.
(Whilst Java is portable, filesystem structures and installation
conventions are not!)

REDUCE itself is an open source project available from
[SourceForge](https://sourceforge.net/projects/reduce-algebra/).  I'm
releasing Run-REDUCE-FX under the [BSD 2-Clause License](LICENSE),
mainly because it's the license used by REDUCE.

This project is set up for development using [IntelliJ
IDEA](https://www.jetbrains.com/idea/) with Run-REDUCE-FX as the
top-level directory.

## Release Notes

### Version 1.1

* Avoid warning messages and hanging Help menu items on Ubuntu, but
  suppress display of PDF files via the Help menu on non-Windows
  platforms since it's probably not currently very helpful!
* Add instructions for installing and running using Java 11 on Linux
  and improve the appearance of the User Guide.
* Use CSS to set the REDUCE font, size, weight and colour, and use the
  same font and size for input as well as output.
* Validate direct input to the FontSizeDialog via the editor.
* Validate generic root directories in REDUCEConfigDialog.  Rebuild
  the Run REDUCE submenus on saving REDUCEConfigDialog.  Improve error
  messages.

### Version 1.2

* Update the build environment to Java 11 and JavaFX 11, which are now
  also required to run Run-REDUCE-FX.
* Provide batch files to run Run-REDUCE-FX more easily.
* Re-instate display of PDF files via the Help menu on non-Windows
  platforms.
* Fix truncated text in the About dialogue.

### Version 1.3

* Provide templates for *df*, *int* and *mat* expressions and *for*
  statements.
* Add a hyperlinked Contents section to the User Guide.

### Version 1.4

* Provide templates for multiple integrals, finite sums and products,
  double- and single-sided limits, and solve.
* Provide a Functions menu offering dialogues that facilitate access
  to key elementary functions, Gamma, Beta and related functions,
  integral functions, Airy, Bessel and related functions, Struve,
  Lommel, Kummer, Whittaker and spherical harmonic functions, and
  classical orthogonal polynomials.  Special function names link to
  the online NIST Digital Library of Mathematical Functions.
* Appropriate templates include a symbolic/numeric option and access
  to relevant REDUCE switches and packages.
* Provide a pop-up keyboard (currently only on template and function
  dialogues) for symbolic constants, Greek letters, elementary
  functions, trigonometric functions (using radians or degrees) and
  hyperbolic functions.  **Still need to arrange that the pop-up
  keyboard loads the trigd package as appropriate.**
* The function dialogues and pop-up keyboard together offer all the
  functions listed in section 7.2 Mathematical Functions of the REDUCE
  Manual plus degree versions of all trigonometric functions.

### Version 1.5

* Add REDUCE Manual hyperlinks to all template and function dialogues.
* Add Help menu items that open the REDUCE Web Site and SourceForge
  Project Site in the default browser.

### Updates since last release

* Use WebView to display REDUCE output, and update instructions and batch files
  for running Run-REDUCE-FX as necessary.
* Turning bold prompts on and off now works retrospectively.
* Set the default Session Log filename to `session.log`.
* Turning I/O colouring off now works retrospectively.
