This directory contains dependencies which are not migrated to maven dependency
mechanism yet. It's subdirectoy structure contains a file based Maven repository.

To run a successful maven test/compile/package/... you first have to install the
libs into your local repository (normally located at ~/.m2). Then Maven will find
the libs on its path during upcoming builds. This installation is done
automatically by installing the parent project, which references the file
repository in this directory.

For updates on this procedure take a look at https://wiki.52north.org/bin/view/Documentation/BestPracticeJarsInMaven#Adding_a_JAR_Dependency