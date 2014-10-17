<?xml version="1.0" encoding="UTF-8"?>
<!-- vim: set ts=2 sw=2 tw=80 ai si: -->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://docbook.org/ns/docbook"
  xmlns:exsl="http://exslt.org/common"
  xmlns:stbl="http://nwalsh.com/xslt/ext/com.nwalsh.saxon.Table"
  xmlns:xtbl="xalan://com.nwalsh.xalan.Table"
  xmlns:lxslt="http://xml.apache.org/xslt"
  xmlns:ptbl="http://nwalsh.com/xslt/ext/xsltproc/python/Table"
  exclude-result-prefixes="d xsl exsl stbl xtbl lxslt ptbl"
  version="1.1">

  <!-- Perforce WebHelp customizations -->

  <!-- ============================================================= -->
  <!-- Import docbook styles                                         -->
  <!-- ============================================================= -->

  <xsl:import href="../../../docbook-xsl-ns-1.78.1/xhtml/chunk.xsl"/>
  <xsl:import href="../../../docbook-xsl-ns-1.78.1/webhelp/xsl/webhelp-common.xsl"/>
  <xsl:include href="../../../docbook-xsl-ns-1.78.1/webhelp/xsl/titlepage.templates.xsl"/>

  <!-- ============================================================= -->
  <!-- parameters and styles                                         -->
  <!-- ============================================================= -->

  <!-- remove style attribute from admon markup, so we can adjust with CSS -->
  <xsl:param name="admon.style"></xsl:param>

  <!-- turn off table borders by default -->
  <xsl:param name="default.table.frame">none</xsl:param>

  <!-- avoid emitting the XML declaration on each chunked file.
       Note that both values must be set to achieve this. -->
  <xsl:param name="chunker.output.encoding" select="'UTF-8'"/>
  <xsl:param name="chunker.output.omit-xml-declaration" select="'yes'"/>
  <xsl:param name="chunker.output.doctype-public" select="''"/>
  <xsl:param name="chunker.output.doctype-system" select="''"/>

  <!-- customize TOC generation -->
  <xsl:param name="generate.toc">
/appendix toc,title
article/appendix  nop
/article  toc,title
book      toc,title
/chapter  toc,title
part      toc,title
/preface  toc,title
reference toc,title
/sect1    toc
/sect2    toc
/sect3    toc
/sect4    toc
/sect5    toc
/section  toc
set       toc,title
  </xsl:param>

  <!-- ============================================================= -->
  <!-- Customizations                                                -->
  <!-- ============================================================= -->

  <!-- customize d:title's title.markup mode to apply normalize-space
       to correct whitespace for titles in desired markup -->
  <xsl:template match="d:title" mode="title.markup">
    <xsl:param name="allow-anchors" select="0"/>

    <xsl:variable name="content">
      <xsl:choose>
        <xsl:when test="$allow-anchors != 0">
          <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates mode="no.anchor.mode"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:copy-of select="normalize-space($content)"/>
  </xsl:template>

  <!-- system HTML <head> section customizations -->
  <xsl:template name="system.head.content">
    <xsl:param name="node" select="."/>
    <!--  The meta tag tells the IE rendering engine that it should use
          the latest, or edge, version of the IE rendering environment;It
          prevents IE from entring compatibility mode.
    -->
    <meta http-equiv="X-UA-Compatible" content="IE=edge" />
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
  </xsl:template>

  <!-- customize the title tag in the head section -->
  <xsl:template name="user.head.title">
    <xsl:param name="node" select="."/>
    <xsl:param name="title"/>
    <title>
      <xsl:copy-of select="$title"/>
      <xsl:text> // </xsl:text>
      <xsl:value-of select="//d:book/d:title"/>
    </title>
  </xsl:template>

  <!-- user HTML <head> section customizations --> 
  <xsl:template name="user.head.content">
    <xsl:param name="title">
      <xsl:apply-templates select="." mode="object.title.markup.textonly"/>
    </xsl:param>
<meta name="Section-title" content="{$title}"/>   
<meta name="viewport" content="width=device-width, initial-scale=1.0"/>
<link rel="stylesheet" href="vendor/bootstrap/css/bootstrap.css"/>
<link rel="stylesheet" href="vendor/prettify/prettify.css"/>
<link rel="stylesheet" href="css/perforce.css"/>
<link rel="stylesheet" href="css/print.css" media="print"/>
<link rel="shortcut icon" href="images/favicon.ico"/>

<xsl:comment><xsl:text>[if lt IE 9]>
  &lt;script type="text/javascript" src="vendor/respond/respond.min.js"&gt;&lt;/script&gt;
  &lt;link rel="stylesheet" type="text/css" href="css/ie.css"/&gt;
&lt;![endif]</xsl:text></xsl:comment>

<!-- 
  browserDetect is an Oxygen addition to warn the user if they're using
  chrome from the file system.  This breaks the Oxygen search highlighting.
-->
<!-- script type="text/javascript" src="{$webhelp.common.dir}browserDetect.js"/ -->
<xsl:if test="$webhelp.include.search.tab != '0'">
<!--
  NOTE: Stemmer javascript files should be in format <language>_stemmer.js.
        For example, for English(en), source should be: "search/stemmers/en_stemmer.js"
        For country codes, see: http://www.uspto.gov/patft/help/helpctry.htm
-->
<!--<xsl:message><xsl:value-of select="concat('search/stemmers/',$webhelp.indexer.language,'_stemmer.js')"/></xsl:message>-->
<!-- script type="text/javascript" src="{concat('search/stemmers/',$webhelp.indexer.language,'_stemmer.js')}">
<xsl:comment>//make this scalable to other languages as well.</xsl:comment>
</script -->
</xsl:if>
<xsl:call-template name="user.webhelp.head.content"/>
  </xsl:template>

  <!-- disable generation of the l10n.js file; we don't need it -->
  <xsl:template name="l10n.js"/>

  <!-- custom chunk-element-content to provide the page structure we need -->
  <xsl:template name="chunk-element-content">
    <xsl:param name="prev"/>
    <xsl:param name="next"/>
    <xsl:param name="nav.context"/>
    <xsl:param name="content">
      <xsl:apply-imports/>
    </xsl:param>
<xsl:call-template name="user.preroot"/>
<xsl:text disable-output-escaping="yes"><![CDATA[<!DOCTYPE html>]]></xsl:text>
<html>
<xsl:call-template name="html.head">
  <xsl:with-param name="prev" select="$prev"/>
  <xsl:with-param name="next" select="$next"/>
</xsl:call-template>
  <body>
    <a id="page-top"></a>
    <noscript>
      <div id="noscript">
        <xsl:call-template name="gentext.template">
          <xsl:with-param name="name" select="'txt_browser_not_supported'"/>
          <xsl:with-param name="context" select="'webhelp'"/>
        </xsl:call-template>
      </div>
    </noscript>
    <xsl:call-template name="user.header.navigation">
      <xsl:with-param name="prev" select="$prev"/>
      <xsl:with-param name="next" select="$next"/>
      <xsl:with-param name="nav.context" select="$nav.context"/>
    </xsl:call-template>

    <div id="content" class="content" tabindex="-1">
      <div class="container">
        <xsl:call-template name="user.header.content"/>
        <xsl:copy-of select="$content"/>
        <xsl:call-template name="user.footer.content"/>
      </div>
    </div>

    <xsl:call-template name="webhelptoc">
      <xsl:with-param name="currentid" select="generate-id(.)"/>
    </xsl:call-template>

    <xsl:if test="$webhelp.include.search.tab != '0'">
    <div id="search">
      <div class="input">
        <input id="search-text" type="search" placeholder="Search this guide"/>

        <button name="clear" type="button" class="clear">
          <span class="glyphicon glyphicon-remove-sign"></span>
        </button>
      </div>

      <div class="controls">
        <div class="substring">
          <input type="checkbox" class="substring" name="substring" value="hide" checked="1"/>
          <span class="description">Hide partial matches</span>
        </div>

        <div class="highlighter">
          <input type="checkbox" class="highlight" name="highlight" value="show" checked="1"/>
          <span class="description">Highlight matches</span>
        </div>
      </div>

      <div class="count">
        <span class="number">0</span> matching pages
      </div>

      <ul class="results"></ul>
    </div>
    </xsl:if>

    <xsl:call-template name="webhelpfooter">
      <xsl:with-param name="prev" select="$prev"/>
      <xsl:with-param name="next" select="$next"/>
      <xsl:with-param name="nav.context" select="$nav.context"/>
    </xsl:call-template>
  </body>
</html>
      <xsl:value-of select="$chunk.append"/>
  </xsl:template>

  <!-- customized webhelptoc to provide desired navigation -->
  <xsl:template name="webhelptoc">
    <xsl:param name="currentid"/>
    <xsl:choose>
      <xsl:when test="$rootid != ''">
        <xsl:variable name="title">
          <xsl:if test="$webhelp.autolabel=1">
            <xsl:variable name="label.markup">
              <xsl:apply-templates select="key('id',$rootid)" mode="label.markup"/>
            </xsl:variable>
            <xsl:if test="normalize-space($label.markup)">
              <xsl:value-of select="concat($label.markup,$autotoc.label.separator)"/>
            </xsl:if>
          </xsl:if>
          <xsl:apply-templates select="key('id',$rootid)" mode="titleabbrev.markup"/>
        </xsl:variable>
        <xsl:variable name="href">
          <xsl:choose>
            <xsl:when test="$manifest.in.base.dir != 0">
              <xsl:call-template name="href.target">
                <xsl:with-param name="object" select="key('id',$rootid)"/>
              </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="href.target.with.base.dir">
                <xsl:with-param name="object" select="key('id',$rootid)"/>
              </xsl:call-template>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
      </xsl:when>

      <xsl:otherwise>
        <xsl:variable name="title">
          <xsl:if test="$webhelp.autolabel=1">
            <xsl:variable name="label.markup">
              <xsl:apply-templates select="/*" mode="label.markup"/>
            </xsl:variable>
            <xsl:if test="normalize-space($label.markup)">
              <xsl:value-of select="concat($label.markup,$autotoc.label.separator)"/>
            </xsl:if>
          </xsl:if>
          <xsl:apply-templates select="/*" mode="titleabbrev.markup"/>
        </xsl:variable>
        <xsl:variable name="href">
          <xsl:choose>
            <xsl:when test="$manifest.in.base.dir != 0">
              <xsl:call-template name="href.target">
                <xsl:with-param name="object" select="/"/>
              </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="href.target.with.base.dir">
                <xsl:with-param name="object" select="/"/>
              </xsl:call-template>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>

        <div id="nav" class="toc">
          <div class="cover"></div>
          <ul class="toc nav">
            <xsl:apply-templates select="/*/*" mode="webhelptoc">
              <xsl:with-param name="currentid" select="$currentid"/>
            </xsl:apply-templates>
          </ul>
        </div>
        <xsl:call-template name="user.webhelp.tabs.content"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- Generates the webhelp table-of-contents (TOC). -->
  <xsl:template match="d:book|d:part|d:reference|d:preface|d:chapter|d:bibliography|d:appendix|d:article|d:topic|d:glossary|d:section|d:simplesect|d:sect1|d:sect2|d:sect3|d:sect4|d:sect5|d:refentry|d:colophon|d:bibliodiv|d:index|d:setindex"
    mode="webhelptoc">
    <xsl:param name="currentid"/>
    
    <xsl:variable name="prefix">
      <xsl:if test="$webhelp.autolabel = 1">
        <xsl:variable name="label.markup">
          <xsl:apply-templates select="." mode="label.markup"/>
        </xsl:variable>
        <xsl:choose>
          <xsl:when test="normalize-space($label.markup)">
            <xsl:value-of select="concat($label.markup,$autotoc.label.separator)"/>
          </xsl:when>
          <xsl:when test="local-name(.) = 'chapter'">
            <xsl:number from="d:book" count="d:chapter" format="{'1'}"/>
            <xsl:value-of select="$autotoc.label.separator"/>
          </xsl:when>
          <xsl:when test="local-name(.) = 'appendix'">
            <xsl:number from="d:book" count="d:appendix" format="{'A'}"/>
            <xsl:value-of select="$autotoc.label.separator"/>
          </xsl:when>
          <xsl:when test="local-name(.) = 'glossary'">
            <xsl:number from="d:appendix" count="d:appendix|d:glossary" format="{'A'}"/>
            <xsl:value-of select="$autotoc.label.separator"/>
          </xsl:when>
        </xsl:choose>
      </xsl:if>
    </xsl:variable>

    <xsl:variable name="title">
      <xsl:apply-templates select="." mode="title.markup"/>
    </xsl:variable>

    <xsl:variable name="href">
      <xsl:choose>
        <xsl:when test="$manifest.in.base.dir != 0">
          <xsl:call-template name="href.target"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="href.target.with.base.dir"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="id" select="generate-id(.)"/>

    <xsl:if test="not(self::index) or (self::index and not($generate.index = 0))">
      <xsl:variable name="subcontent">
        <xsl:if test="d:part|d:reference|d:preface|d:chapter|d:bibliography|d:appendix|d:article|d:topic|d:glossary|d:section|d:simplesect|d:sect1|d:sect2|d:sect3|d:sect4|d:sect5|d:refentry|d:colophon|d:bibliodiv">
        <a class="expander"><span class="glyphicon glyphicon-chevron-down"/></a>
        <ul class="nav">
          <xsl:apply-templates select="d:part|d:reference|d:preface|d:chapter|d:bibliography|d:appendix|d:article|d:topic|d:glossary|d:section|d:simplesect|d:sect1|d:sect2|d:sect3|d:sect4|d:sect5|d:refentry|d:colophon|d:bibliodiv"
            mode="webhelptoc">
            <xsl:with-param name="currentid" select="$currentid"/>
          </xsl:apply-templates>
        </ul>
        </xsl:if>
      </xsl:variable>
      <li>
        <xsl:if test="$id = $currentid or $subcontent//li[@class='active']">
          <xsl:attribute name="class">active</xsl:attribute>
        </xsl:if>
        <a href="{substring-after($href, $base.dir)}">
          <xsl:if test="$prefix">
            <span class="prefix"><xsl:value-of select="$prefix"/></span>
          </xsl:if>
          <xsl:value-of select="$title"/>
        </a>
        <xsl:copy-of select="$subcontent"/>
      </li>
    </xsl:if>
  </xsl:template>

  <!-- The Header with the company logo -->
  <xsl:template name="webhelpheader">
    <xsl:param name="prev"/>
    <xsl:param name="next"/>
    <xsl:param name="nav.context"/>
    <xsl:param name="title">
      <xsl:apply-templates select="." mode="object.title.markup.textonly"/>
    </xsl:param>
    <xsl:variable name="home" select="/*[1]"/>
    <xsl:variable name="up" select="parent::*"/>

    <div id="header">
        <button name="toc" type="button" class="toc">
          <span class="glyphicon glyphicon-list"></span>
        </button>
        <span class="logo">
          <a href="http://www.perforce.com/documentation"></a>
        </span>
        <h1>
          <a href="index.html" class="title">
            <span class="brand"/>
            <span class="guide-title">
              <xsl:apply-templates select="/*[1]" mode="title.markup"/>
            </span>
            <span class="guide-subtitle">
              (<xsl:apply-templates select="/*[1]"
              mode="subtitle.markup"/>)
            </span>

          </a>
        </h1>
        <xsl:if test="$webhelp.include.search.tab != '0'">
          <button name="search" type="button" class="search">
            <span class="glyphicon glyphicon-search"></span>
          </button>
        </xsl:if>
    </div>
  </xsl:template>

  <!-- customized footer -->
  <xsl:template name="webhelpfooter">
    <xsl:param name="prev"/>
    <xsl:param name="next"/>
    <xsl:param name="nav.context"/>
    <xsl:param name="title">
      <xsl:apply-templates select="." mode="object.title.markup.textonly"/>
    </xsl:param>

    <xsl:variable name="home" select="/*[1]"/>
    <xsl:variable name="up" select="parent::*"/>

    <div id="footer">
      <div class="container">
      <xsl:choose>
        <xsl:when test="count($prev)>0">
          <a accesskey="p" class="nav-prev" title="Press 'p', or left-arrow, to view the previous page">
            <xsl:attribute name="href">
              <xsl:call-template name="href.target">
                <xsl:with-param name="object" select="$prev"/>
              </xsl:call-template>
            </xsl:attribute>
            <span class="glyphicon glyphicon-chevron-left"></span>
            <div class="label">Previous</div>
            <div class="title">
              <xsl:apply-templates select="$prev" mode="object.title.markup.textonly"/>
            </div>
          </a>
        </xsl:when>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="count($next)>0">
          <a accesskey="n" class="nav-next" title="Press 'n', or right-arrow, to view the next page">
            <xsl:attribute name="href">
              <xsl:call-template name="href.target">
                <xsl:with-param name="object" select="$next"/>
              </xsl:call-template>
            </xsl:attribute>
            <span class="glyphicon glyphicon-chevron-right"></span>
            <div class="label">Next</div>
            <div class="title">
              <xsl:apply-templates select="$next" mode="object.title.markup.textonly"/>
            </div>
          </a>
        </xsl:when>
      </xsl:choose>
      </div>
    </div>

    <script type="text/javascript" src="vendor/jquery/jquery-1.10.2.min.js"></script>
    <script type="text/javascript" src="vendor/bootstrap/js/bootstrap.js"></script>
    <script type="text/javascript" src="vendor/cookie/jquery.cookie.js"></script>
    <script type="text/javascript" src="vendor/highlight/jquery.highlight.js"></script>
    <script type="text/javascript" src="vendor/jsrender/jsrender.js"></script>
    <script type="text/javascript" src="vendor/touchwipe/jquery.touchwipe.min.js"></script>
    <script type="text/javascript" src="vendor/prettify/prettify.js"></script>
    <xsl:if test="$webhelp.include.search.tab != '0'">
      <script type="text/javascript" src="js/index.js"></script>
    </xsl:if>
    <script type="text/javascript" src="js/perforce.js"></script>
    <xsl:call-template name="user.webhelp.foot.content"/>
  </xsl:template>

  <!-- customize the generator meta tag -->
  <xsl:template name="head.content.generator">
    <meta name="generator" content="DocBook {$DistroTitle} V{$VERSION} with Perforce customizations"/>
  </xsl:template>

  <!-- customized user.footer.content -->
  <xsl:template name="user.footer.content"/>

  <!-- customized user.webhelp.foot.content -->
  <xsl:template name="user.webhelp.foot.content"/>

  <xsl:template match="uri">
    <xsl:call-template name="inline.monoseq">
      <xsl:with-param name="content">
        <a>
          <xsl:apply-templates select="." mode="common.html.attributes"/>
          <xsl:call-template name="id.attribute"/>
          <xsl:attribute name="href">
            <xsl:value-of select="."/>
          </xsl:attribute>
          <xsl:apply-templates/>
        </a>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <!-- customized tgroup template to remove the incorrect assumption that
       tables must have border declarations built in. -->
  <xsl:template match="tgroup" name="tgroup">
    <xsl:if test="not(@cols) or @cols = '' or string(number(@cols)) = 'NaN'">
      <xsl:message terminate="yes">
        <xsl:text>Error: CALS tables must specify the number of columns.</xsl:text>
      </xsl:message>
    </xsl:if>

    <xsl:variable name="summary">
      <xsl:call-template name="pi.dbhtml_table-summary"/>
    </xsl:variable>

    <xsl:variable name="cellspacing">
      <xsl:call-template name="pi.dbhtml_cellspacing"/>
    </xsl:variable>

    <xsl:variable name="cellpadding">
      <xsl:call-template name="pi.dbhtml_cellpadding"/>
    </xsl:variable>

    <table>
      <xsl:choose>
        <!-- If there's a textobject/phrase for the table summary, use it -->
        <xsl:when test="../textobject/phrase">
          <xsl:attribute name="summary">
            <xsl:value-of select="../textobject/phrase"/>
          </xsl:attribute>
        </xsl:when>

        <!-- If there's a <?dbhtml table-summary="foo"?> PI, use it for
             the HTML table summary attribute -->
        <xsl:when test="$summary != ''">
          <xsl:attribute name="summary">
            <xsl:value-of select="$summary"/>
          </xsl:attribute>
        </xsl:when>

        <!-- Otherwise, if there's a title, use that -->
        <xsl:when test="../title">
          <xsl:attribute name="summary">
            <!-- This screws up on inline markup and footnotes, oh well... -->
            <xsl:value-of select="string(../title)"/>
          </xsl:attribute>
        </xsl:when>

        <!-- Otherwise, forget the whole idea -->
        <xsl:otherwise><!-- nevermind --></xsl:otherwise>
      </xsl:choose>

      <xsl:if test="$cellspacing != '' or $html.cellspacing != ''">
        <xsl:attribute name="cellspacing">
          <xsl:choose>
            <xsl:when test="$cellspacing != ''">
              <xsl:value-of select="$cellspacing"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$html.cellspacing"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>

      <xsl:if test="$cellpadding != '' or $html.cellpadding != ''">
        <xsl:attribute name="cellpadding">
          <xsl:choose>
            <xsl:when test="$cellpadding != ''">
              <xsl:value-of select="$cellpadding"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$html.cellpadding"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>

      <xsl:if test="../@pgwide=1 or local-name(.) = 'entrytbl'">
        <xsl:attribute name="width">100%</xsl:attribute>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="$table.borders.with.css != 0">
          <xsl:choose>
            <xsl:when test="../@frame='all' or (not(../@frame) and $default.table.frame='all')">
              <xsl:attribute name="style">
                <xsl:text>border-collapse: collapse;</xsl:text>
                <xsl:call-template name="border">
                  <xsl:with-param name="side" select="'top'"/>
                  <xsl:with-param name="style" select="$table.frame.border.style"/>
                  <xsl:with-param name="color" select="$table.frame.border.color"/>
                  <xsl:with-param name="thickness" select="$table.frame.border.thickness"/>
                </xsl:call-template>
                <xsl:call-template name="border">
                  <xsl:with-param name="side" select="'bottom'"/>
                  <xsl:with-param name="style" select="$table.frame.border.style"/>
                  <xsl:with-param name="color" select="$table.frame.border.color"/>
                  <xsl:with-param name="thickness" select="$table.frame.border.thickness"/>
                </xsl:call-template>
                <xsl:call-template name="border">
                  <xsl:with-param name="side" select="'left'"/>
                  <xsl:with-param name="style" select="$table.frame.border.style"/>
                  <xsl:with-param name="color" select="$table.frame.border.color"/>
                  <xsl:with-param name="thickness" select="$table.frame.border.thickness"/>
                </xsl:call-template>
                <xsl:call-template name="border">
                  <xsl:with-param name="side" select="'right'"/>
                  <xsl:with-param name="style" select="$table.frame.border.style"/>
                  <xsl:with-param name="color" select="$table.frame.border.color"/>
                  <xsl:with-param name="thickness" select="$table.frame.border.thickness"/>
                </xsl:call-template>
              </xsl:attribute>
            </xsl:when>
            <xsl:when test="../@frame='topbot' or (not(../@frame) and $default.table.frame='topbot')">
              <xsl:attribute name="style">
                <xsl:text>border-collapse: collapse;</xsl:text>
                <xsl:call-template name="border">
                  <xsl:with-param name="side" select="'top'"/>
                  <xsl:with-param name="style" select="$table.frame.border.style"/>
                  <xsl:with-param name="color" select="$table.frame.border.color"/>
                  <xsl:with-param name="thickness" select="$table.frame.border.thickness"/>
                </xsl:call-template>
                <xsl:call-template name="border">
                  <xsl:with-param name="side" select="'bottom'"/>
                  <xsl:with-param name="style" select="$table.frame.border.style"/>
                  <xsl:with-param name="color" select="$table.frame.border.color"/>
                  <xsl:with-param name="thickness" select="$table.frame.border.thickness"/>
                </xsl:call-template>
              </xsl:attribute>
            </xsl:when>
            <xsl:when test="../@frame='top' or (not(../@frame) and $default.table.frame='top')">
              <xsl:attribute name="style">
                <xsl:text>border-collapse: collapse;</xsl:text>
                <xsl:call-template name="border">
                  <xsl:with-param name="side" select="'top'"/>
                  <xsl:with-param name="style" select="$table.frame.border.style"/>
                  <xsl:with-param name="color" select="$table.frame.border.color"/>
                  <xsl:with-param name="thickness" select="$table.frame.border.thickness"/>
                </xsl:call-template>
              </xsl:attribute>
            </xsl:when>
            <xsl:when test="../@frame='bottom' or (not(../@frame) and $default.table.frame='bottom')">
              <xsl:attribute name="style">
                <xsl:text>border-collapse: collapse;</xsl:text>
                <xsl:call-template name="border">
                  <xsl:with-param name="side" select="'bottom'"/>
                  <xsl:with-param name="style" select="$table.frame.border.style"/>
                  <xsl:with-param name="color" select="$table.frame.border.color"/>
                  <xsl:with-param name="thickness" select="$table.frame.border.thickness"/>
                </xsl:call-template>
              </xsl:attribute>
            </xsl:when>
            <xsl:when test="../@frame='sides' or (not(../@frame) and $default.table.frame='sides')">
              <xsl:attribute name="style">
                <xsl:text>border-collapse: collapse;</xsl:text>
                <xsl:call-template name="border">
                  <xsl:with-param name="side" select="'left'"/>
                  <xsl:with-param name="style" select="$table.frame.border.style"/>
                  <xsl:with-param name="color" select="$table.frame.border.color"/>
                  <xsl:with-param name="thickness" select="$table.frame.border.thickness"/>
                </xsl:call-template>
                <xsl:call-template name="border">
                  <xsl:with-param name="side" select="'right'"/>
                  <xsl:with-param name="style" select="$table.frame.border.style"/>
                  <xsl:with-param name="color" select="$table.frame.border.color"/>
                  <xsl:with-param name="thickness" select="$table.frame.border.thickness"/>
                </xsl:call-template>
              </xsl:attribute>
            </xsl:when>
            <xsl:when test="../@frame='none'">
              <xsl:attribute name="style">
                <xsl:text>border: none;</xsl:text>
              </xsl:attribute>
            </xsl:when>
            <xsl:otherwise>
              <xsl:attribute name="style">
                <xsl:text>border-collapse: collapse;</xsl:text>
              </xsl:attribute>
            </xsl:otherwise>
          </xsl:choose>

        </xsl:when>
        <xsl:when test="../@frame='none' or (not(../@frame) and $default.table.frame='none') or local-name(.) = 'entrytbl'">
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="border">1</xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:variable name="colgroup">
        <colgroup>
          <xsl:call-template name="generate.colgroup">
            <xsl:with-param name="cols" select="@cols"/>
          </xsl:call-template>
        </colgroup>
      </xsl:variable>

      <xsl:variable name="explicit.table.width">
        <xsl:call-template name="pi.dbhtml_table-width">
          <xsl:with-param name="node" select=".."/>
        </xsl:call-template>
      </xsl:variable>

      <xsl:variable name="table.width">
        <xsl:choose>
          <xsl:when test="$explicit.table.width != ''">
            <xsl:value-of select="$explicit.table.width"/>
          </xsl:when>
          <xsl:when test="$default.table.width = ''">
            <xsl:text>100%</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$default.table.width"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:variable>

      <xsl:if test="$default.table.width != ''
                    or $explicit.table.width != ''">
        <xsl:attribute name="width">
          <xsl:choose>
            <xsl:when test="contains($table.width, '%')">
              <xsl:value-of select="$table.width"/>
            </xsl:when>
            <xsl:when test="$use.extensions != 0
                            and $tablecolumns.extension != 0">
              <xsl:choose>
                <xsl:when test="function-available('stbl:convertLength')">
                  <xsl:value-of select="stbl:convertLength($table.width)"/>
                </xsl:when>
                <xsl:when test="function-available('xtbl:convertLength')">
                  <xsl:value-of select="xtbl:convertLength($table.width)"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:message terminate="yes">
                    <xsl:text>No convertLength function available.</xsl:text>
                  </xsl:message>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="$table.width"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>

      <xsl:choose>
        <xsl:when test="$use.extensions != 0
                        and $tablecolumns.extension != 0">
          <xsl:choose>
            <xsl:when test="function-available('stbl:adjustColumnWidths')">
              <xsl:copy-of select="stbl:adjustColumnWidths($colgroup)"/>
            </xsl:when>
            <xsl:when test="function-available('xtbl:adjustColumnWidths')">
              <xsl:copy-of select="xtbl:adjustColumnWidths($colgroup)"/>
            </xsl:when>
            <xsl:when test="function-available('ptbl:adjustColumnWidths')">
              <xsl:copy-of select="ptbl:adjustColumnWidths($colgroup)"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:message terminate="yes">
                <xsl:text>No adjustColumnWidths function available.</xsl:text>
              </xsl:message>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:copy-of select="$colgroup"/>
        </xsl:otherwise>
      </xsl:choose>

      <xsl:apply-templates select="thead"/>
      <xsl:apply-templates select="tfoot"/>
      <xsl:apply-templates select="tbody"/>

      <xsl:if test=".//footnote|../title//footnote">
        <tbody class="footnotes">
          <tr>
            <td colspan="{@cols}">
              <xsl:apply-templates select=".//footnote|../title//footnote" mode="table.footnote.mode"/>
            </td>
          </tr>
        </tbody>
      </xsl:if>
    </table>
  </xsl:template>

  <!-- customize nongraphical.admonition to include an 'admonition' class,
       so we can use common CSS for important, caution, note, tip, and warning
       admonitions. -->
  <xsl:template name="nongraphical.admonition">
    <div>
      <xsl:call-template name="common.html.attributes">
        <xsl:with-param name="inherit" select="1"/>
        <xsl:with-param name="class" select="concat(local-name(.), ' admonition')"/>
      </xsl:call-template>
      <xsl:call-template name="id.attribute"/>
      <xsl:if test="$admon.style != '' and $make.clean.html = 0">
        <xsl:attribute name="style">
          <xsl:value-of select="$admon.style"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:if test="$admon.textlabel != 0 or title or info/title">
        <h3 class="title">
          <xsl:call-template name="anchor"/>
          <xsl:apply-templates select="." mode="object.title.markup"/>
        </h3>
      </xsl:if>

      <xsl:apply-templates/>
    </div>
  </xsl:template>

  <!-- customize handling of imageobject with condition attributes, which should
       be reflected as a CSS class on an image's container in HTML. -->
  <xsl:template match="d:imageobject">
    <xsl:choose>
      <xsl:when test="@condition">
        <span>
          <xsl:attribute name="class">
            <xsl:value-of select="@condition"/>
          </xsl:attribute>
          <xsl:apply-templates select="d:imagedata"/>
        </span>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="d:imagedata"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- customize handling of para tags with condition attributes, which should
       be reflected as a CSS class on the p tag in HTML. -->
  <xsl:template match="d:para">
    <xsl:call-template name="paragraph">
      <xsl:with-param name="class">
        <xsl:if test="@role and $para.propagates.style != 0">
          <xsl:value-of select="@role"/>
        </xsl:if>
        <xsl:if test="@condition">
          <xsl:value-of select="@condition"/>
        </xsl:if>
      </xsl:with-param>
      <xsl:with-param name="content">
        <xsl:if test="position() = 1 and parent::d:listitem">
          <xsl:call-template name="anchor">
            <xsl:with-param name="node" select="parent::d:listitem"/>
          </xsl:call-template>
        </xsl:if>

        <xsl:call-template name="anchor"/>
        <xsl:apply-templates/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <!-- customize xrefs to avoid emitting unwanted leader text -->
  <xsl:param name="local.l10n.xml" select="document('')"/> 
  <l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0"> 
    <l:l10n language="en"> 
      <l:context name="xref"> 
        <l:template name="chapter" text="&#8220;%t&#8221;"/>
        <l:template name="bridgehead" text="%t"/> 
        <l:template name="refsection" text="%t"/> 
        <l:template name="refsect1" text="%t"/> 
        <l:template name="refsect2" text="%t"/> 
        <l:template name="refsect3" text="%t"/> 
        <l:template name="sect1" text="%t"/> 
        <l:template name="sect2" text="%t"/> 
        <l:template name="sect3" text="%t"/> 
        <l:template name="sect4" text="%t"/> 
        <l:template name="sect5" text="%t"/> 
        <l:template name="section" text="%t"/> 
        <l:template name="simplesect" text="%t"/> 
      </l:context>    
    </l:l10n>
  </l:i18n>

  <!-- customize replaceable to add a bold version -->
  <xsl:template match="d:replaceable">
    <xsl:choose>
      <xsl:when test="@role='bold'">
        <xsl:call-template name="inline.bolditalicmonoseq"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="inline.italicmonoseq"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="inline.bolditalicmonoseq">
    <xsl:param name="content">
      <xsl:call-template name="anchor"/>
      <xsl:call-template name="simple.xlink">
        <xsl:with-param name="content">
          <xsl:apply-templates/>
        </xsl:with-param>
      </xsl:call-template>
    </xsl:param>
    <b><em>
      <xsl:call-template name="common.html.attributes"/>
      <xsl:call-template name="id.attribute"/>
      <code>
        <xsl:call-template name="generate.html.title"/>
        <xsl:call-template name="dir"/>
        <xsl:copy-of select="$content"/>
        <xsl:call-template name="apply-annotations"/>
      </code>
    </em></b>
  </xsl:template>

  <!-- customize programlisting to include language as a CSS class -->
  <xsl:template match="d:programlisting|d:screen|d:synopsis">
    <xsl:param name="suppress-numbers" select="'0'"/>

    <xsl:call-template name="anchor"/>

    <xsl:variable name="div.element">pre</xsl:variable>

    <xsl:if test="$shade.verbatim != 0">
      <xsl:message>
        <xsl:text>The shade.verbatim parameter is deprecated. </xsl:text>
        <xsl:text>Use CSS instead,</xsl:text>
      </xsl:message>
      <xsl:message>
        <xsl:text>for example: pre.</xsl:text>
        <xsl:value-of select="local-name(.)"/>
        <xsl:text> { background-color: #E0E0E0; }</xsl:text>
      </xsl:message>
    </xsl:if>

    <xsl:choose>
      <xsl:when test="$suppress-numbers = '0' and @linenumbering = 'numbered'                     and $use.extensions != '0' and $linenumbering.extension != '0'">
        <xsl:variable name="rtf">
          <xsl:choose>
            <xsl:when test="$highlight.source != 0">
              <xsl:call-template name="apply-highlighting"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>
        <xsl:element name="{$div.element}" namespace="http://www.w3.org/1999/xhtml">
          <xsl:if test="@language">
            <xsl:attribute name="lang">
              <xsl:value-of select="@language"/>
            </xsl:attribute>
          </xsl:if>
          <xsl:apply-templates select="." mode="common.html.attributes"/>
          <xsl:call-template name="id.attribute"/>
          <xsl:if test="@width != ''">
            <xsl:attribute name="width">
              <xsl:value-of select="@width"/>
            </xsl:attribute>
          </xsl:if>
          <xsl:call-template name="number.rtf.lines">
            <xsl:with-param name="rtf" select="$rtf"/>
          </xsl:call-template>
        </xsl:element>
      </xsl:when>
      <xsl:otherwise>
        <xsl:element name="{$div.element}" namespace="http://www.w3.org/1999/xhtml">
          <xsl:if test="@language">
            <xsl:attribute name="lang">
              <xsl:value-of select="@language"/>
            </xsl:attribute>
          </xsl:if>
          <xsl:apply-templates select="." mode="common.html.attributes"/>
          <xsl:call-template name="id.attribute"/>
          <xsl:if test="@width != ''">
            <xsl:attribute name="width">
              <xsl:value-of select="@width"/>
            </xsl:attribute>
          </xsl:if>
          <xsl:choose>
            <xsl:when test="$highlight.source != 0">
              <xsl:call-template name="apply-highlighting"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:element>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- customize chapter xrefs so that they are not italic -->
  <xsl:template match="d:chapter|d:appendix" mode="insert.title.markup">
    <xsl:param name="purpose"/>
    <xsl:param name="xrefstyle"/>
    <xsl:param name="title"/>

    <xsl:copy-of select="$title"/>
  </xsl:template>

  <!-- PDFs require a pagebreak processing instruction in certain case.
       Add a complementary no-op PI here to avoid errors. -->
  <xsl:template match="processing-instruction('pagebreak')"></xsl:template>
</xsl:stylesheet>
