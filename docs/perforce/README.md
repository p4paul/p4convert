# Perforce

This directory contains the Apache ANT-based build infrastructure for
transforming Perforce guides authored in DocBook XML into PDF and HTML.
The following provides a brief overview of the structure and content
of this directory.

| File/Folder           | Description |
| --------------------- | ----------- |
| `assets/`               | directory contains one directory for each available [transformation target](#transformation_targets). |
| `common_adoc/`          | directory contains common AsciiDoc files that many guides require. |
| `common_xml/`           | directory contains common DocBook XML files that many guides require. |
| `ad_generator.rb`       | Ruby post-processor for AsciiDoctor-generated DocBook. |
| `build.properties`      | declares properties that are transformation-independent, e.g. specifying which XSLT processor to use. |
| `build.xml`             | contains the ANT targets used to transform guides into various forms. |
| `global.properties`     | declares a mix of ANT and DocBook XML properties that provide defaults for all transformations. |
| `indexer.py`            | a Python script that indexes the generated HTML, to facilitate live searching within a guide. |
| `sample_oxygen.catalog` | See catalogs, [below](#catalogs). |

<a name="transformation_targets"></a>
## Transformation Targets

A transformation target is a combination of an ANT build target and the
assets in the identically named folder. For example, to produce a PDF
version of a guide, within the guide's directory you would invoke `ant
pdf`, which executes the `pdf` target in `build.xml`, which uses the asset
files within `assets/pdf`.

<a name="catalogs"></a>
## Catalogs

If you use the oXygen XML editor, customize the `sample_oxygen.catalog`
file to specify where the `common_xml` and manuals directories exist on
your system. Then configure oXygen to use this catalog file to allow
oXygen to validate our DocBook XML guides. This is necessary because the
paths to each directory can vary depending on how you have installed them,
and oXygen won't be able to find them without this configuration.

<a name="ant"></a>
## ANT Processing Notes

A word on ANT properties, for the unintiated: ANT properties are mostly
immutable; the first definition wins. This makes the handling of per-guide
property settings, combined with reasonable defaults for per-format guide
transformations, combined with common defaults for any transformation,
appear rather convoluted.

Using the suggested [sample guide](../sample_guide/README.md) directory
structure, properties are read in the following order:

1. _guide_/build.properties
1. perforce/build.properties
1. _guide_/assets/_build target_/build.properties
1. perforce/global.properties

<a name="params"></a>
## DocBook Parameter Notes

Many, but not all, DocBook parameters can be specified within a guide's
`build.properties` file (or more likely, the guide's
assets/_build target_/build.properties file. If you need to set a specific
parameter but it does not seem to take effect, add it to the guide's
assets/_build target_/_build target_.xsl file.

<!--- vim: set ts=2 sw=2 tw=74 ai si: -->