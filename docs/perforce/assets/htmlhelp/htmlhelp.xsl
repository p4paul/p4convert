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

  <!-- Perforce HTML Help customizations -->

  <!-- ============================================================= -->
  <!-- Import docbook styles                                         -->
  <!-- ============================================================= -->

  <xsl:import href="../../../docbook-xsl-ns-1.78.1/htmlhelp/htmlhelp.xsl"/>

  <!-- ============================================================= -->
  <!-- parameters and styles                                         -->
  <!-- ============================================================= -->

  <!-- remove style attribute from admon markup, so we can adjust with CSS -->
  <xsl:param name="admon.style"></xsl:param>

  <!-- turn off table borders by default -->
  <xsl:param name="default.table.frame">none</xsl:param>

  <!-- customize the generator meta tag -->
  <xsl:template name="head.content.generator">
    <meta name="generator" content="DocBook {$DistroTitle} V{$VERSION} with Perforce customizations"/>
  </xsl:template>

  <!-- user HTML <head> section customizations --> 
  <xsl:template name="user.head.content">
  <xsl:param name="node" select="."/>
<link rel="stylesheet" href="css/style.css"/>
  </xsl:template>

  <!-- customize the footer to include the Perforce logo -->
  <xsl:template name="user.footer.content">
  <xsl:param name="node" select="."/>
<div id="footer">
<a href="http://www.perforce.com/documentation/" class="btn"><img src="images/perforcelogo.png"/></a>
</div>
  </xsl:template>

  <!-- customize URI template to use monospace text -->
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

  <!-- turn off <body> tag attributes; we'll use CSS instead -->
  <xsl:template name="body.attributes"/>

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
  <xsl:template match="processing-instruction('pagebreak')"/>
  <xsl:template match="processing-instruction('chapterbreak')"/>
</xsl:stylesheet>
