# p4convert

The `p4convert` is a Java based conversion tool. It imports data from Subversion or CVS
and reconstructs the file revision history in Perforce. 

CVS data is read from the CVSROOT and Subversion data from a dump file.  The converted 
history is added to Perforce using our client interface (`IMPORT` mode / front-door) or 
directly to a journal and versioned files (`CONVERT` mode / back-door).

The `IMPORT` mode allows for incremental conversions and although slower, it generally 
requires less memory and CPU resources.


## Distribution

Stable releases will be available from the Perforce Website and [`FTP`](ftp://ftp.perforce.com/perforce/tools/p4convert/) server.

Latest builds are available under the [`release/`](https://swarm.workshop.perforce.com/projects/perforce-software-p4convert/files/main/release) directory on the Workshop.


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

Documentation is available as a PDF document or in HTML. The HTML documention is
packaged with each release under the `docs` directory and [online](https://swarm.workshop.perforce.com/projects/perforce-software-p4convert/view/main/docs/p4convert/publicsite-generated/index.html).

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

* SVN property `svn:needs-lock` cannot be imported to a distributed Perforce server and may raise the error:
	`can't change +l type with reopen; use revert -k and then edit -t to change type.`


## Support

Customers with a support agreement can get support from the normal channels.  For everyone else (or to help with your own investigation) the following sets out what to look for.
 
### Logs and tools

All log files are located in the converters working directroy (the location you execute the converter).

#### converter.log

* Contains a detailed listing of the console output from the converter.  Each subsequent execution appends to the log, providing a record of previous runs (essential for debugging incremental conversions).

* If after a migration reports warnings, start with the `converter.log` log and grep for `WARN`.  In some situations you may choose to ignore warnings, if you are satisfied with the consequences e.g. Unicode traslation warnings or Case sensitive rename issues.

#### audit.log

* Contains a list of every revision imported into Perforce, reporting the original SCM path, revision, Perforce change number and MD5 sum.

    ``` 
#<SCM path>, <SCM id>, <P4 change>, <MD5 sum>
trunk/file.txt, 1, 1, 16FED0121505838F492D0295BA547558
trunk/file.txt, 3, 3, 0C5A8546AAB8197C98CA37805482955C
    ```
    
#### log levels

* By default the logging is set to `INFO` level, however more details logging can be achieved using the `FINE` and `TRACE` options.  To turn up the logging level for the whole tool or a specific class edit the `debug.log4j.properties` file and execute the JAR using the `-Dlog4j.configuration=file:debug.log4j.properties` JVM paramiter.

* Please refer to Oracle's [documentation](http://docs.oracle.com/cd/E19717-01/819-7753/gcblo/index.html) for further details.


### Issues

Issues are tracked on the Workshop under the [Jobs](https://swarm.workshop.perforce.com/projects/perforce-software-p4convert/jobs/)
tab.  

Please check (using the search field) that no similar jobs exists before reporting 
an issue.  Jobs are publicly visible, so please do not include any confidential 
information in the report.


## Quick Start

### Version Check

````
java -jar p4convert.jar --version

PUBLIC.Main.11901
````

### Generate a Configuration

For Subversion set the type to `SVN`

````
java -jar p4convert.jar --type=SVN --default

(creates a 'default.cfg' file)
````

For CVS set the type to `CVS`

````
java -jar p4convert.jar --type=CVS --default

(creates a 'default.cfg' file)
````

### Setting the Configuration

The latest version requires no modification for a basic conversion.  The only paramiter unset is the source location for the CVSROOT or SVN dump file, but the `--repo=` paramiter (if specified) will take precedence.

The default configuration sets the converter up for `IMPORT` mode on port `localhost:4444`. The user is set to `p4-user` and client to `p4-client` with a new import depot called `import`.  The client root is set to `'ws/'` under your current working directory.

### Import mode

1. Start a Perforce Server on port `localhost:4444`
2. Run `java -jar p4convert.jar --config=default.cfg --repo=my_repo_location`

### Convert mode

1. Edit the default.cfg and set the `com.p4convert.p4.mode=IMPORT` line to `CONVERT`
2. Run `java -jar p4convert.jar --config=default.cfg --repo=my_repo_location`
3. Change directory to `p4_root`
4. Run `p4d -jr jnl.0` to restore the journal
5. Run `p4d -xu` to correctly update the tables



## Source

The source code is available [here](https://swarm.workshop.perforce.com/projects/perforce-software-p4convert/files/main)
to sync the code with Perforce, sign up to the Workshop and use the following settings:

* Perforce Server: `public.perforce.com:1666`
* Main code-line: `//guest/perforce_software/p4convert/...`

### Build

Building from source requires [Gradle](https://gradle.org/) version 2.2.1 or greater and a Java JDK 7.
To build, change to the project's rood directory (the location of `build.gradle) and run:

    gradle jar
    
If the build succeeds then the jar is written to `dist/p4convert.jar`.

To build the HTML web document pages:

    gradle docWeb
    
Finally if all looks good to build a release tar-ball package:

    gradle clean
    gradle -Pver=PUBLIC.Main.nnnnnn rel


### Testing

The tests rely on the Perforce broker and server, please insure you have the correct
versions for your tests.  The broker `p4broker` and server `p4d` must be in your PATH.

The tests use ports `4444` and `4445` please insure these are free and not used by any 
other services.  Please check that these resources have been freed from earlier runs.

To run all tests:

    gradle test
    
    
### Contributing

__TBC__