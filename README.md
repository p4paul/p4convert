# p4convert

The `p4convert` is a Java based conversion tool. It imports data from Subversion or CVS
and reconstructs the file revision history in Perforce. For CVS the data is read from the
CVSROOT and for Subversion data is read from a dump file.


## Distribution

Stable releases will be available from the Perforce Website and FTP server:

* ftp://ftp.perforce.com/perforce/tools/p4convert/
* TBC

Latest builds are available under the `release/` directory on the Workshop:

* https://swarm.workshop.perforce.com/projects/perforce-software-p4convert/files/main/release


## Packaging

The conversion tool ships as a tar-ball `p4convert.tgz` and when unpacked consists of the 
following files:

    PUBLIC.Main.11901/                           (Release directory)
    PUBLIC.Main.11901/debug.log4j.properties     (Sample dubug logging template)
    PUBLIC.Main.11901/default.cfg                (Sample configuration file)
    PUBLIC.Main.11901/p4convert-notes.txt        (Release notes)
    PUBLIC.Main.11901/p4convert.jar              (Core JAR to execute)
    PUBLIC.Main.11901/p4convert.pdf              (Documentation)
    PUBLIC.Main.11901/types.map                  (Standard Typemaps to help identify binary files)

The directory `PUBLIC.Main.11901` will always contain the last submitted Workshop 
changelist number for the project. Releases will be made available as soon as a feature
or fix is deemed stable.


## Documentation

Documentation is available as a PDF document or in HTML. The PDF document `p4convert.pdf`
is packaged with each release and the HTML documentation available on our website:

* TBC

The markdown file `README.md` (this document) is located in the project's root directory
on the Workshop. 


## Requirements

* JRE Java 7 or above
* P4D 12.2 or greater (10.2 is supported with a reduction in features)
* Admin level access to the Perforce server (`IMPORT` mode)

### Recommendations

* A server grade machine with at least 64GB RAM and fast local disks
* Free diskspace of 4 times the size of the repository
* Linux (Ubuntu/Debian) operating system


## Limitations

* Symbolic links are not supported in `IMPORT` mode on Windows.
* CVS keyword expansion is not supported.
* SVN keyword expansion may yield different results when synced. 


## Support

`TODO`

### Issues

`TODO`

## Quick Start

`TODO`

## Source

The source code is available [here](https://swarm.workshop.perforce.com/projects/perforce-software-p4convert/files/main)
to sync the code with Perforce, sign up to the Workshop and use the following settings:

* Perforce Server: `public.perforce.com:1666`
* Main code-line: `//guest/perforce_software/p4convert/...`

### Build

Building from source requires Apache `ant` version 1.8.x or greater and a Java JDK 7.
To build, change to the project's rood directory (the location of `build.xml`) and run:

    ant
    
If the build succeeds then the jar is written to `dist/p4convert.jar`.

To build the PDF user guide:

    ant docs
    
or to build the HTML web pages:

    ant web
    
Finally if all looks good to build a release tar-ball package:

    ant clean
    ant -Dversion=PUBLIC.Main.nnnnnn release


### Testing

The tests rely on the Perforce broker and server, please insure you have the correct
versions for your tests.  The broker `p4broker` and server `p4d` must be in your PATH.

The tests use ports `4444` and `4445` please insure these are free and not used by any 
other services.  Please check that these resources are freed from earlier test runs.

To run all tests:

    ant junit
    
To run just the CVS tests or SVN tests use:

    ant cvs
    ant svn

To run just one test case use the following, for example test case 003:

    ant -Dcase=003 cvs-back           (CVS in CONVERT mode)
    ant -Dcase=003 cvs-front          (CVS in IMPORT mode)
    ant -Dcase=003 back               (SVN in CONVERT mode)
    ant -Dcase=003 front              (SVN in IMPORT mode)


### Contributing

`TODO`