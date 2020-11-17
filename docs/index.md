### Key links: [Run-REDUCE.jar](https://github.com/fjwright/Run-REDUCE/releases/latest/download/Run-REDUCE.jar) | [Install and Run](InstallAndRun.md) | [User Guide](UserGuide.html)

Run-REDUCE is a Graphical User Interface for running the
[REDUCE](https://reduce-algebra.sourceforge.io/) Computer Algebra
System that should provide a consistent user experience across all
implementations of REDUCE and all platforms.  It is implemented in
JavaFX and the executable application takes the form of the JAR file
[Run-REDUCE.jar](https://github.com/fjwright/Run-REDUCE/releases/latest/download/Run-REDUCE.jar).
Click on the link here or above to download the latest release.  (The
image below relates to an earlier release and is now a little out of
date!)

![Run-REDUCE screen shot](Run-REDUCE.png "Run-REDUCE screen shot")

REDUCE itself is an open source project available from
[SourceForge](https://sourceforge.net/projects/reduce-algebra/), which
you need to install separately.  Run-REDUCE is designed to run a
standard installation of REDUCE; it does not include REDUCE.

Full information about how to install and run Run-REDUCE is
available in the [Install and Run Guide](InstallAndRun.md).  For
information about how to use Run-REDUCE please see the [User
Guide](UserGuide.html) (which is also included in Run-REDUCE and
easily accessible via the Help menu).  Known issues are listed at end
of the [Installation Guide](InstallationGuide.md).

Run-REDUCE is still under development but here are the key features
that it currently provides:

* Commands to run REDUCE that are fully configurable but default to
  running CSL and PSL REDUCE as appropriate for the standard
  distributions.
* A REDUCE input/output display pane that scrolls in both directions
  as necessary.  Its contents can be saved to a file and/or erased.
* Optional typeset-style display of mathematical output.
* A multi-line input editing pane that also scrolls in both directions
  as necessary.  Previous input is remembered and can be scrolled
  through, edited and re-input.  A final terminator is normally added
  automatically if appropriate when input is sent to REDUCE.
* Optional split or tabbed panes running independent REDUCE processes.
* Menu items to handle REDUCE file input/output and load standard
  REDUCE packages, similar to the facilities provided by the CSL
  REDUCE GUI.
* Optional bold prompts and coloured input/output text similar to
  redfront but based on the input mode (algebraic or symbolic).
* Templates to construct structured expressions and statements.
* A popup keyboard to access symbolic constants, Greek letters and
  elementary functions.
* Dialogues to access standard mathematical functions using
  conventional notation.
* Easy access to the Run-REDUCE User Guide, the standard
  documentation distributed with REDUCE as HTML or PDF files, the
  REDUCE Web Site and the SourceForge Project Site.

Francis Wright, November 2020
