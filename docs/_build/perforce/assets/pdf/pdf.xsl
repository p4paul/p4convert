<?xml version="1.0" encoding="UTF-8"?>
<!-- vim: set ts=2 sw=2 tw=80 ai si: -->
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:d="http://docbook.org/ns/docbook"
  xmlns:fo="http://www.w3.org/1999/XSL/Format"
  xmlns:exsl="http://exslt.org/common"
  version="1.0">

  <!-- ============================================================= -->
  <!-- Import docbook, highlighting, and oXygen customization styles -->
  <!-- ============================================================= -->

  <xsl:import href="../../../docbook-xsl-ns-1.78.1/fo/profile-docbook.xsl"/>

    <!-- Template to add the namespace to non-namespaced documents -->
    <!-- oXygen Patch: Add the docbook namespace only to the elements from no namespace.-->
    <xsl:template match="*" mode="addNS">
        <xsl:choose>
            <xsl:when test="namespace-uri(.) = ''">
                <xsl:element name="{local-name()}" 
                    namespace="http://docbook.org/ns/docbook">
                    <!-- EXM-26444 Fix, replace entityrefs with filerefs -->
                    <xsl:if test="@entityref">
                        <xsl:attribute name="fileref"><xsl:value-of select="unparsed-entity-uri(@entityref)"/></xsl:attribute>
                    </xsl:if>
                    <!-- Copy any attribute except entityref -->
                    <xsl:copy-of select="@*[local-name()!='entityref']"/>
                    <!--EXM-21274 Add the xml:base to the root-->
                    <xsl:if test="not(../..)">
                        <xsl:call-template name="add-xml-base"/>
                    </xsl:if>
                    <xsl:apply-templates select="node()" mode="addNS"/>
                </xsl:element>                
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="."/>
            </xsl:otherwise>
        </xsl:choose>        
    </xsl:template>

  <!-- ============================================================= -->
  <!-- parameters and styles                                         -->
  <!-- ============================================================= -->

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

  <!-- fonts -->
  <xsl:param name="body.font.family">Palatino, serif</xsl:param>
  <xsl:param name="title.font.family">MyriadPro-Cond, sans-serif</xsl:param>
  <xsl:param name="monospace.font.family">TheSansMono, serif</xsl:param>

  <!-- formal titles -->
  <xsl:attribute-set name="formal.title.properties">
    <xsl:attribute name="font-family">MyriadPro, sans-serif</xsl:attribute>
    <xsl:attribute name="font-weight">normal</xsl:attribute>
    <xsl:attribute name="font-style">normal</xsl:attribute>
  </xsl:attribute-set>

  <!-- main body text alignment -->
  <xsl:param name="alignment">left</xsl:param>

  <!-- stub definition of a custom param for the Perforce logo.
       Note: this path needs to be specified via oXygen transformation scenario
       parameters, to '${pdu}/images/perforce-logo.svg', which overrides this
       definition. -->
  <xsl:param name="logo.url">images/perforce-logo.svg</xsl:param>

  <!-- style for a xref's and xlinks -->
  <xsl:attribute-set name="xref.properties">
    <xsl:attribute name="color">blue</xsl:attribute>
    <xsl:attribute name="text-decoration">underline</xsl:attribute>
  </xsl:attribute-set>

  <!-- styles for titlepage title -->
  <xsl:attribute-set name="book.titlepage.recto.style.title">
    <xsl:attribute name="font-family">MyriadPro, sans-serif</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="font-size">34pt</xsl:attribute>
    <xsl:attribute name="font-style">normal</xsl:attribute>
    <xsl:attribute name="text-align">left</xsl:attribute>
    <xsl:attribute name="space-before">18pt</xsl:attribute>
    <xsl:attribute name="margin-top">246pt</xsl:attribute>
  </xsl:attribute-set>

  <!-- styles for titlepage subtitle -->
  <xsl:attribute-set name="book.titlepage.recto.style.subtitle">
    <xsl:attribute name="font-family">Palatino, serif</xsl:attribute>
    <xsl:attribute name="font-weight">normal</xsl:attribute>
    <xsl:attribute name="font-size">16pt</xsl:attribute>
    <xsl:attribute name="font-style">normal</xsl:attribute>
    <xsl:attribute name="text-align">left</xsl:attribute>
    <xsl:attribute name="space-before">216pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:template match="d:subtitle" mode="book.titlepage.recto.auto.mode">
    <fo:block-container position="absolute" top="590pt" left="2pt">
      <fo:block
        xmlns:fo="http://www.w3.org/1999/XSL/Format"
        xsl:use-attribute-sets="book.titlepage.recto.style.subtitle"
      >
        <xsl:apply-templates select="." mode="book.titlepage.recto.mode"/>
      </fo:block>
    </fo:block-container>
  </xsl:template>

  <!-- styles for titlepage pubdate -->
  <xsl:attribute-set name="book.titlepage.recto.style.pubdate">
    <xsl:attribute name="font-family">Palatino, serif</xsl:attribute>
    <xsl:attribute name="font-weight">normal</xsl:attribute>
    <xsl:attribute name="font-size">12pt</xsl:attribute>
    <xsl:attribute name="font-style">italic</xsl:attribute>
    <xsl:attribute name="text-align">left</xsl:attribute>
  </xsl:attribute-set>

  <xsl:template match="d:pubdate" mode="book.titlepage.recto.auto.mode">
    <fo:block-container position="absolute" top="612pt" left="2pt">
      <fo:block
        xmlns:fo="http://www.w3.org/1999/XSL/Format"
        xsl:use-attribute-sets="book.titlepage.recto.style.pubdate"
      >
        <xsl:apply-templates select="." mode="book.titlepage.recto.mode"/>
      </fo:block>
    </fo:block-container>
  </xsl:template>

  <!-- styles for titlepage, verso side, copyright -->
  <xsl:attribute-set name="book.titlepage.verso.style.copyright">
    <xsl:attribute name="font-family">MyriadPro-Cond, sans-serif</xsl:attribute>
    <xsl:attribute name="font-size">10pt</xsl:attribute>
    <xsl:attribute name="space-before">1em</xsl:attribute>
  </xsl:attribute-set>

  <!-- styles for major TOC lines, e.g. a chapter -->
  <xsl:attribute-set name="toc.line.properties.larger">
    <xsl:attribute name="font-family">MyriadPro-Cond, sans-serif</xsl:attribute>
    <xsl:attribute name="font-size">16pt</xsl:attribute>
    <xsl:attribute name="font-weight">normal</xsl:attribute>
    <xsl:attribute name="margin-top">20pt</xsl:attribute>
    <xsl:attribute name="margin-bottom">10pt</xsl:attribute>
    <xsl:attribute name="text-align-last">justify</xsl:attribute>
    <xsl:attribute name="text-align">start</xsl:attribute>
    <xsl:attribute name="end-indent"><xsl:value-of select="concat($toc.indent.width, 'pt')"/></xsl:attribute>
    <xsl:attribute name="last-line-end-indent"><xsl:value-of select="concat('-', $toc.indent.width, 'pt')"/></xsl:attribute>
  </xsl:attribute-set>

  <!-- styles for the 'label' part of a major TOC line, to reduce the size and make it bold -->
  <xsl:attribute-set name="toc.line.properties.larger.label">
    <xsl:attribute name="font-family">MyriadPro-Cond, sans-serif</xsl:attribute>
    <xsl:attribute name="font-size">12pt</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="text-align">start</xsl:attribute>
    <xsl:attribute name="space-end">0pt</xsl:attribute>
    <xsl:attribute name="padding-right">10pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="toc.line.properties">
    <xsl:attribute name="font-family">Palatino, serif</xsl:attribute>
  </xsl:attribute-set>

  <!-- turn off table borders -->
  <xsl:param name="default.table.frame">none</xsl:param>

  <!-- define table row/cell colors to use consistently -->
  <xsl:param name="table.row.head.bgcolor">#f0f0f0</xsl:param>
  <xsl:param name="table.row.even.bgcolor">#f8f8f8</xsl:param>
  <xsl:param name="table.row.odd.bgcolor">#ffffff</xsl:param>

  <!-- define thicknesses/colors for table borders -->
  <xsl:param name="table.cell.border.thickness.head">1pt</xsl:param>
  <xsl:param name="table.cell.border.color.head">#000000</xsl:param>
  <xsl:param name="table.cell.border.thickness.row">0.25pt</xsl:param>
  <xsl:param name="table.cell.border.color.row">#666666</xsl:param>

  <!-- define the height of a chapter's title table -->
  <xsl:param name="chapter.table.height">28pt</xsl:param>

  <!-- styles for a chapter's label -->
  <xsl:attribute-set name="chap.label.properties">
    <xsl:attribute name="font-family">MyriadPro-Cond</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="font-size">12pt</xsl:attribute>
    <!-- font size is added dynamically by section.heading template -->
    <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
    <xsl:attribute name="space-before.minimum">0em</xsl:attribute>
    <xsl:attribute name="space-before.optimum">0em</xsl:attribute>
    <xsl:attribute name="space-before.maximum">0em</xsl:attribute>
    <xsl:attribute name="line-height">1.4</xsl:attribute>
    <xsl:attribute name="text-align">left</xsl:attribute>
    <xsl:attribute name="start-indent">0pt</xsl:attribute>
  </xsl:attribute-set>

  <!-- style for a chapter's title -->
  <xsl:attribute-set name="chap.title.properties">
    <xsl:attribute name="font-family">MyriadPro, sans-serif</xsl:attribute>
    <xsl:attribute name="font-size">22pt</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <!-- font size is added dynamically by section.heading template -->
    <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
    <xsl:attribute name="space-before.minimum">0.8em</xsl:attribute>
    <xsl:attribute name="space-before.optimum">1.0em</xsl:attribute>
    <xsl:attribute name="space-before.maximum">1.2em</xsl:attribute>
    <xsl:attribute name="text-align">left</xsl:attribute>
    <xsl:attribute name="start-indent">0pt</xsl:attribute>
  </xsl:attribute-set>

  <!-- style for a table displaying a chapter's title -->
  <xsl:attribute-set name="chapter.table.properties">
    <xsl:attribute name="table-layout">fixed</xsl:attribute>
    <xsl:attribute name="width">100%</xsl:attribute>
    <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
    <xsl:attribute name="space-before.minimum">0em</xsl:attribute>
    <xsl:attribute name="space-before.optimum">0em</xsl:attribute>
    <xsl:attribute name="space-before.maximum">0em</xsl:attribute>
    <xsl:attribute name="start-indent">0pt</xsl:attribute>
    <xsl:attribute name="border-top-width">1.5pt</xsl:attribute>
    <xsl:attribute name="border-top-color">black</xsl:attribute>
    <xsl:attribute name="border-top-style">solid</xsl:attribute>
    <xsl:attribute name="border-bottom-width">1.5pt</xsl:attribute>
    <xsl:attribute name="border-bottom-color">black</xsl:attribute>
    <xsl:attribute name="border-bottom-style">solid</xsl:attribute>
    <xsl:attribute name="margin-top">0pt</xsl:attribute>
    <xsl:attribute name="margin-bottom">12pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="footer.content.properties">
    <xsl:attribute name="font-style">italic</xsl:attribute>
    <xsl:attribute name="font-family">
      <xsl:value-of select="$body.fontset"/>
    </xsl:attribute>
    <xsl:attribute name="margin-left">
      <xsl:value-of select="$title.margin.left"/>
    </xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="monospace.verbatim.properties"
                     use-attribute-sets="verbatim.properties monospace.properties">
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
    <xsl:attribute name="hyphenation-character">\</xsl:attribute>
    <xsl:attribute name="font-size">9pt</xsl:attribute>
  </xsl:attribute-set>

  <!-- glossary presentation parameters
       note: these do not work together;
             if glosslist.as.blocks is non-zero, you get blocks
             if it's zero, the glossterm.width takes effect.
             adjust as required -->
  <xsl:param name="glosslist.as.blocks" select="1"/>
  <xsl:param name="glossterm.width">216pt</xsl:param>

  <!-- section1 properties -->
  <xsl:attribute-set name="section.title.level1.properties">
    <xsl:attribute name="border-bottom-width">1pt</xsl:attribute>
    <xsl:attribute name="border-bottom-style">dotted</xsl:attribute>
    <xsl:attribute name="border-bottom-color">#dddddd</xsl:attribute>
  </xsl:attribute-set>

  <!-- styles screen blocks -->
  <xsl:attribute-set name="screen.styles">
    <xsl:attribute name="background-color">#f0f0f0</xsl:attribute>
    <xsl:attribute name="padding-left">1em</xsl:attribute>
    <xsl:attribute name="padding-right">1em</xsl:attribute>
    <xsl:attribute name="padding-bottom">1em</xsl:attribute>
    <xsl:attribute name="margin-left">0em</xsl:attribute>
    <xsl:attribute name="margin-right">0em</xsl:attribute>
  </xsl:attribute-set>

  <!-- styles for programlisting blocks -->
  <xsl:attribute-set name="programlisting.styles">
    <xsl:attribute name="padding-left">1em</xsl:attribute>
    <xsl:attribute name="padding-right">1em</xsl:attribute>
    <xsl:attribute name="padding-bottom">1em</xsl:attribute>
    <xsl:attribute name="margin-left">0em</xsl:attribute>
    <xsl:attribute name="margin-right">0em</xsl:attribute>

    <xsl:attribute name="border-top-width">0.5pt</xsl:attribute>
    <xsl:attribute name="border-top-style">solid</xsl:attribute>
    <xsl:attribute name="border-top-color">#000000</xsl:attribute>
    <xsl:attribute name="border-bottom-width">0.5pt</xsl:attribute>
    <xsl:attribute name="border-bottom-style">solid</xsl:attribute>
    <xsl:attribute name="border-bottom-color">#000000</xsl:attribute>
    <xsl:attribute name="border-left-width">0.5pt</xsl:attribute>
    <xsl:attribute name="border-left-style">solid</xsl:attribute>
    <xsl:attribute name="border-left-color">#000000</xsl:attribute>
    <xsl:attribute name="border-right-width">0.5pt</xsl:attribute>
    <xsl:attribute name="border-right-style">solid</xsl:attribute>
    <xsl:attribute name="border-right-color">#000000</xsl:attribute>
  </xsl:attribute-set>

  <!-- style for procedure steps -->
  <xsl:attribute-set name="procedure.step.style">
    <xsl:attribute name="font-weight">bold</xsl:attribute>
  </xsl:attribute-set>

  <!-- style for section 2 headings -->
  <xsl:attribute-set name="section.title.level2.properties">
    <xsl:attribute name="start-indent"><xsl:value-of select="$body.start.indent"/></xsl:attribute>
  </xsl:attribute-set>

  <!-- style for section 3 headings -->
  <xsl:attribute-set name="section.title.level3.properties">
    <xsl:attribute name="start-indent"><xsl:value-of select="$body.start.indent"/></xsl:attribute>
  </xsl:attribute-set>

  <!-- style for section 4 headings -->
  <xsl:attribute-set name="section.title.level4.properties">
    <xsl:attribute name="start-indent"><xsl:value-of select="$body.start.indent"/></xsl:attribute>
  </xsl:attribute-set>

  <!-- style for section 5 headings -->
  <xsl:attribute-set name="section.title.level5.properties">
    <xsl:attribute name="start-indent"><xsl:value-of select="$body.start.indent"/></xsl:attribute>
  </xsl:attribute-set>

  <!-- style for section 6 headings -->
  <xsl:attribute-set name="section.title.level5.properties">
    <xsl:attribute name="start-indent"><xsl:value-of select="$body.start.indent"/></xsl:attribute>
  </xsl:attribute-set>

  <!-- customize the appearance of page numbers in xrefs -->
  <xsl:param name="insert.xref.page.number">yes</xsl:param>
  <xsl:param name="local.l10n.xml" select="document('')"/>
  <l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
    <l:l10n language="en">
      <l:context name="xref">
        <l:template name="page.citation" text=" on page %p"/>
        <l:template name="chapter" text="“%t”"/>
        <l:template name="bridgehead" text="“%t”"/>
        <l:template name="refsection" text="“%t”"/>
        <l:template name="refsect1" text="“%t”"/>
        <l:template name="refsect2" text="“%t”"/>
        <l:template name="refsect3" text="“%t”"/>
        <l:template name="sect1" text="“%t”"/>
        <l:template name="sect2" text="“%t”"/>
        <l:template name="sect3" text="“%t”"/>
        <l:template name="sect4" text="“%t”"/>
        <l:template name="sect5" text="“%t”"/>
        <l:template name="section" text="“%t”"/>
        <l:template name="simplesect" text="“%t”"/>
      </l:context>

      <l:context name="xref-number-and-title">
        <l:template name="chapter" text="Chapter %n, “%t”"/>
      </l:context>
    </l:l10n>
  </l:i18n>

  <l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
    <l:l10n language="en">
      <l:gentext key="PubDate" text=""/>
      <l:gentext key="pubdate" text=""/>
    </l:l10n>
  </l:i18n>

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
  
  <!-- set off remarks in bold blue font and border -->
  <xsl:template match="d:comment|d:remark">
    <xsl:if test="$show.comments != 0">
      <fo:block font-style="italic" font-weight="bold" color="mediumblue" border="solid"
        border-color="mediumblue" padding="2px">
        <xsl:call-template name="inline.charseq"/>
      </fo:block>
    </xsl:if>
  </xsl:template>


  <!-- ============================================================= -->
  <!-- procedure step customization                                  -->
  <!-- ============================================================= -->

  <xsl:template match="d:procedure/d:step/d:para[1]">
    <fo:block xsl:use-attribute-sets="procedure.step.style">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:template>

  <xsl:template match="d:procedure/d:step|d:substeps/d:step">
    <xsl:variable name="id">
      <xsl:call-template name="object.id"/>
    </xsl:variable>

    <xsl:variable name="keep.together">
      <xsl:call-template name="pi.dbfo_keep-together"/>
    </xsl:variable>

    <fo:list-item xsl:use-attribute-sets="list.item.spacing">
      <xsl:if test="$keep.together != ''">
        <xsl:attribute name="keep-together.within-column"><xsl:value-of
                        select="$keep.together"/></xsl:attribute>
      </xsl:if>
      <fo:list-item-label end-indent="label-end()" xsl:use-attribute-sets="procedure.step.style">
        <fo:block id="{$id}">
          <!-- dwc: fix for one step procedures. Use a bullet if there's no step 2 -->
          <xsl:choose>
            <xsl:when test="count(../d:step) = 1">
              <xsl:text>&#x2022;</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="." mode="number">
                <xsl:with-param name="recursive" select="0"/>
              </xsl:apply-templates>.
            </xsl:otherwise>
          </xsl:choose>
        </fo:block>
      </fo:list-item-label>
      <fo:list-item-body start-indent="body-start()">
        <fo:block>
          <xsl:apply-templates/>
        </fo:block>
      </fo:list-item-body>
    </fo:list-item>
  </xsl:template>


  <!-- ============================================================= -->
  <!-- header and footer customizations                              -->
  <!-- ============================================================= -->

  <!-- customized header.table template to do nothing on first pages -->
  <xsl:template name="header.table">
    <xsl:param name="pageclass" select="''"/>
    <xsl:param name="sequence" select="''"/>
    <xsl:param name="gentext-key" select="''"/>

    <!-- default is a single table style for all headers -->
    <!-- Customize it for different page classes or sequence location -->

    <xsl:choose>
      <xsl:when test="$pageclass = 'index'">
        <xsl:attribute name="margin-{$direction.align.start}">0pt</xsl:attribute>
      </xsl:when>
    </xsl:choose>

    <xsl:variable name="column1">
      <xsl:choose>
        <xsl:when test="$double.sided = 0">1</xsl:when>
        <xsl:when test="$sequence = 'first' or $sequence = 'odd'">1</xsl:when>
        <xsl:otherwise>3</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="column3">
      <xsl:choose>
        <xsl:when test="$double.sided = 0">3</xsl:when>
        <xsl:when test="$sequence = 'first' or $sequence = 'odd'">3</xsl:when>
        <xsl:otherwise>1</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="candidate">
      <fo:table xsl:use-attribute-sets="header.table.properties">
        <xsl:call-template name="head.sep.rule">
          <xsl:with-param name="pageclass" select="$pageclass"/>
          <xsl:with-param name="sequence" select="$sequence"/>
          <xsl:with-param name="gentext-key" select="$gentext-key"/>
        </xsl:call-template>
        <fo:table-column column-number="1">
          <xsl:attribute name="column-width">
            <xsl:text>proportional-column-width(</xsl:text>
            <xsl:call-template name="header.footer.width">
              <xsl:with-param name="location">header</xsl:with-param>
              <xsl:with-param name="position" select="$column1"/>
              <xsl:with-param name="pageclass" select="$pageclass"/>
              <xsl:with-param name="sequence" select="$sequence"/>
              <xsl:with-param name="gentext-key" select="$gentext-key"/>
            </xsl:call-template>
            <xsl:text>)</xsl:text>
          </xsl:attribute>
        </fo:table-column>
        <fo:table-column column-number="2">
          <xsl:attribute name="column-width">
            <xsl:text>proportional-column-width(</xsl:text>
            <xsl:call-template name="header.footer.width">
              <xsl:with-param name="location">header</xsl:with-param>
              <xsl:with-param name="position" select="2"/>
              <xsl:with-param name="pageclass" select="$pageclass"/>
              <xsl:with-param name="sequence" select="$sequence"/>
              <xsl:with-param name="gentext-key" select="$gentext-key"/>
            </xsl:call-template>
            <xsl:text>)</xsl:text>
          </xsl:attribute>
        </fo:table-column>
        <fo:table-column column-number="3">
          <xsl:attribute name="column-width">
            <xsl:text>proportional-column-width(</xsl:text>
            <xsl:call-template name="header.footer.width">
              <xsl:with-param name="location">header</xsl:with-param>
              <xsl:with-param name="position" select="$column3"/>
              <xsl:with-param name="pageclass" select="$pageclass"/>
              <xsl:with-param name="sequence" select="$sequence"/>
              <xsl:with-param name="gentext-key" select="$gentext-key"/>
            </xsl:call-template>
            <xsl:text>)</xsl:text>
          </xsl:attribute>
        </fo:table-column>

        <fo:table-body>
          <fo:table-row>
            <xsl:attribute name="block-progression-dimension.minimum">
              <xsl:value-of select="$header.table.height"/>
            </xsl:attribute>
            <fo:table-cell text-align="start" display-align="before">
              <xsl:if test="$fop.extensions = 0">
                <xsl:attribute name="relative-align">baseline</xsl:attribute>
              </xsl:if>
              <fo:block>
                <xsl:call-template name="header.content">
                  <xsl:with-param name="pageclass" select="$pageclass"/>
                  <xsl:with-param name="sequence" select="$sequence"/>
                  <xsl:with-param name="position" select="$direction.align.start"/>
                  <xsl:with-param name="gentext-key" select="$gentext-key"/>
                </xsl:call-template>
              </fo:block>
            </fo:table-cell>
            <fo:table-cell text-align="center" display-align="before">
              <xsl:if test="$fop.extensions = 0">
                <xsl:attribute name="relative-align">baseline</xsl:attribute>
              </xsl:if>
              <fo:block>
                <xsl:call-template name="header.content">
                  <xsl:with-param name="pageclass" select="$pageclass"/>
                  <xsl:with-param name="sequence" select="$sequence"/>
                  <xsl:with-param name="position" select="'center'"/>
                  <xsl:with-param name="gentext-key" select="$gentext-key"/>
                </xsl:call-template>
              </fo:block>
            </fo:table-cell>
            <fo:table-cell text-align="right" display-align="before">
              <xsl:if test="$fop.extensions = 0">
                <xsl:attribute name="relative-align">baseline</xsl:attribute>
              </xsl:if>
              <fo:block>
                <xsl:call-template name="header.content">
                  <xsl:with-param name="pageclass" select="$pageclass"/>
                  <xsl:with-param name="sequence" select="$sequence"/>
                  <xsl:with-param name="position" select="$direction.align.end"/>
                  <xsl:with-param name="gentext-key" select="$gentext-key"/>
                </xsl:call-template>
              </fo:block>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-body>
      </fo:table>
    </xsl:variable>

    <!-- Really output a header? -->
    <xsl:choose>
      <xsl:when test="$pageclass = 'titlepage' and $gentext-key = 'book'
        and $sequence='first'">
        <!-- no, book titlepages have no headers at all -->
      </xsl:when>
      <xsl:when test="$sequence='first'">
        <!-- no, book first pages have no headers at all -->
      </xsl:when>
      <xsl:when test="$sequence = 'blank' and $headers.on.blank.pages = 0">
        <!-- no output -->
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="$candidate"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- customized header.content to display the current chapter title in the header -->
  <xsl:template name="header.content">
    <xsl:param name="pageclass" select="''"/>
    <xsl:param name="sequence" select="''"/>
    <xsl:param name="position" select="''"/>
    <xsl:param name="gentext-key" select="''"/>

    <fo:block>
      <!-- sequence can be odd, even, first, blank -->
      <!-- position can be left, center, right -->
      <xsl:choose>
        <xsl:when test="$sequence = 'blank'">
          <!-- nothing -->
        </xsl:when>

        <xsl:when test="$position='left'">
          <!-- Same for odd, even, empty, and blank sequences -->
          <xsl:call-template name="draft.text"/>
        </xsl:when>

        <xsl:when test="($sequence='odd' or $sequence='even') and $position='center'">
          <xsl:if test="$pageclass != 'titlepage'">
            <xsl:choose>
              <xsl:when test="ancestor::d:book and ($double.sided != 0)">
                <xsl:apply-templates select="." mode="object.title.markup"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:apply-templates select="." mode="titleabbrev.markup"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
        </xsl:when>

        <xsl:when test="$position='center'">
          <!-- nothing for empty and blank sequences -->
        </xsl:when>

        <xsl:when test="$position='right'">
          <!-- Same for odd, even, empty, and blank sequences -->
          <xsl:call-template name="draft.text"/>
        </xsl:when>

        <xsl:when test="$sequence = 'first'">
          <!-- nothing for first pages -->
        </xsl:when>

        <xsl:when test="$sequence = 'blank'">
          <!-- nothing for blank pages -->
        </xsl:when>
      </xsl:choose>
    </fo:block>
  </xsl:template>

  <!-- customized footer.content to include the book's title -->
  <xsl:template name="footer.content">
    <xsl:param name="pageclass" select="''"/>
    <xsl:param name="sequence" select="''"/>
    <xsl:param name="position" select="''"/>
    <xsl:param name="gentext-key" select="''"/>

    <fo:block>
      <!-- pageclass can be front, body, back -->
      <!-- sequence can be odd, even, first, blank -->
      <!-- position can be left, center, right -->
      <xsl:choose>
        <xsl:when test="$pageclass = 'titlepage'">
          <!-- nop; no footer on title pages -->
        </xsl:when>

        <xsl:when test="$double.sided != 0 and $sequence = 'even' and $position = 'left'">
          <fo:page-number/>
        </xsl:when>

        <xsl:when test="$double.sided != 0 and $sequence = 'even' and $position = 'right'">
          <xsl:value-of select="//d:book/d:title"/>
        </xsl:when>

        <xsl:when test="$double.sided != 0 and ($sequence = 'odd' or $sequence = 'first')">
          <xsl:choose>
            <xsl:when test="$position = 'left'">
              <xsl:value-of select="//d:book/d:title"/>
            </xsl:when>
            <xsl:when test="$position = 'right'">
              <fo:page-number/>
            </xsl:when>
          </xsl:choose>
        </xsl:when>

        <xsl:when test="$double.sided = 0 and $position='center'">
          <fo:page-number/> - <xsl:value-of select="//d:book/d:title"/>
        </xsl:when>

        <xsl:when test="$sequence='blank'">
          <xsl:choose>
            <xsl:when test="$double.sided = 0 and $position = 'center'">
              <fo:page-number/>
            </xsl:when>
            <xsl:when test="$double.sided != 0 and $position = 'left'">
              <fo:page-number/>
            </xsl:when>
            <xsl:when test="$double.sided != 0 and $position = 'right'">
              <xsl:value-of select="//d:book/d:title"/>
            </xsl:when>
            <xsl:otherwise>
              <!-- nop -->
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>

        <xsl:otherwise>
          <!-- nop -->
        </xsl:otherwise>
      </xsl:choose>
    </fo:block>
  </xsl:template>

  <!-- ============================================== -->
  <!-- Customize the appearance of the Chapter titles -->
  <!-- ============================================== -->

  <!-- customized object.title.markup mode to handle chapter title -->
  <xsl:template match="*" mode="chapter.title.markup">
    <!-- select the gentext template -->
    <xsl:param name="allow-anchors" select="0"/>
    <xsl:variable name="template">
      <xsl:apply-templates select="." mode="chapter.title.template"/>
    </xsl:variable>

    <!-- apply template with current context -->
    <xsl:call-template name="substitute-markup">
      <xsl:with-param name="allow-anchors" select="$allow-anchors"/>
      <xsl:with-param name="template" select="$template"/>
    </xsl:call-template>
  </xsl:template>

  <!-- customized object.title.template to handle chapter title -->
  <xsl:template match="*" mode="chapter.title.template">
    <xsl:call-template name="gentext.template">
      <xsl:with-param name="context" select="'title'"/>
      <xsl:with-param name="name">
        <xsl:call-template name="xpath.location"/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <!-- select the correct gentext template for a chapter title -->
  <xsl:template match="d:chapter|d:appendix" mode="chapter.title.template">
    <xsl:choose>
      <xsl:when test="string($chapter.autolabel) != 0">
        <xsl:call-template name="gentext.template">
          <xsl:with-param name="context" select="'xref-number'"/>
          <xsl:with-param name="name">
            <xsl:call-template name="xpath.location"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="gentext.template">
          <xsl:with-param name="context" select="'title-unnumbered'"/>
          <xsl:with-param name="name">
            <xsl:call-template name="xpath.location"/>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- emits chapter label text without its title -->
  <xsl:template name="chapter.label">
    <xsl:param name="node" select="."/>

    <xsl:variable name="type">
      <xsl:value-of select="local-name($node)"/>
    </xsl:variable>

    <xsl:variable name="label">
      <xsl:choose>
        <xsl:when test="$type = 'chapter'">
          <xsl:apply-templates select="$node" mode="label.markup"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text> </xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <fo:block xsl:use-attribute-sets="chap.label.properties">
      <xsl:choose>
        <xsl:when test="string($chapter.autolabel) != 0">
          <xsl:call-template name="substitute-markup">
            <xsl:with-param name="template">
              <xsl:call-template name="gentext.template">
                <xsl:with-param name="context" select="'xref-number'"/>
                <xsl:with-param name="name" select="$type"/>
              </xsl:call-template>
            </xsl:with-param>
            <xsl:with-param name="label" select="$label"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise/>
      </xsl:choose>
    </fo:block>
  </xsl:template>

  <!-- emits chapter title text without its label -->
  <xsl:template name="chapter.title">
    <xsl:param name="node" select="."/>

    <fo:block xsl:use-attribute-sets="chap.title.properties">
      <xsl:apply-templates select="$node" mode="title.markup"/>
    </fo:block>
  </xsl:template>

  <!-- custom template to emit chapter/appendix titles in a table -->
  <xsl:template name="chapappendix.title.table">
    <xsl:param name="label" select="''"/>
    <xsl:param name="title" select="''"/>

    <fo:block xsl:use-attribute-sets="pgwide.properties">
      <fo:table xsl:use-attribute-sets="chapter.table.properties">
        <xsl:if test="string($chapter.autolabel) != 0">
          <fo:table-column column-number="1">
            <xsl:attribute name="column-width">
              <xsl:text>proportional-column-width(0.2)</xsl:text>
            </xsl:attribute>
          </fo:table-column>
        </xsl:if>
        <fo:table-column column-number="2">
          <xsl:attribute name="column-width">
            <xsl:text>proportional-column-width(1)</xsl:text>
          </xsl:attribute>
        </fo:table-column>

        <fo:table-body>
          <fo:table-row>
            <xsl:attribute name="block-progression-dimension.minimum">
              <xsl:value-of select="$chapter.table.height"/>
            </xsl:attribute>
            <xsl:if test="string($chapter.autolabel) != 0">
              <fo:table-cell text-align="left" display-align="before">
                <xsl:attribute name="padding-before">6pt</xsl:attribute>
                <xsl:attribute name="padding-after">3pt</xsl:attribute>
                <!-- need to emit the chapter label here -->
                <xsl:call-template name="chapter.label">
                  <xsl:with-param name="node" select="$label"/>
                </xsl:call-template>
              </fo:table-cell>
            </xsl:if>
            <fo:table-cell text-align="left" display-align="before">
              <xsl:attribute name="padding-before">6pt</xsl:attribute>
              <xsl:attribute name="padding-after">3pt</xsl:attribute>
              <!-- need to emit the chapter title here -->
              <xsl:call-template name="chapter.title">
                <xsl:with-param name="node" select="$title"/>
              </xsl:call-template>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-body>
      </fo:table>
    </fo:block>
  </xsl:template>

  <!-- custom chapter title markup start point -->
  <xsl:template match="d:title" mode="chapter.titlepage.recto.auto.mode">
    <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="chapter.titlepage.recto.style">
      <xsl:call-template name="chapappendix.title.table">
        <xsl:with-param name="label" select="ancestor-or-self::d:chapter[1]"/>
        <xsl:with-param name="title" select="ancestor-or-self::d:chapter[1]"/>
      </xsl:call-template>
    </fo:block>
  </xsl:template>

  <!-- custom appendix title markup start point -->
  <xsl:template match="d:title" mode="appendix.titlepage.recto.auto.mode">
    <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="appendix.titlepage.recto.style" margin-left="{$title.margin.left}">
      <xsl:call-template name="chapappendix.title.table">
        <xsl:with-param name="label" select="ancestor-or-self::d:appendix[1]"/>
        <xsl:with-param name="title" select="ancestor-or-self::d:appendix[1]"/>
      </xsl:call-template>
    </fo:block>
  </xsl:template>

  <!-- customize chapter xrefs so that they are not italic -->
  <xsl:template match="d:chapter|d:appendix" mode="insert.title.markup">
    <xsl:param name="purpose"/>
    <xsl:param name="xrefstyle"/>
    <xsl:param name="title"/>

    <xsl:copy-of select="$title"/>
  </xsl:template>

  <!-- ===================================== -->
  <!-- Declare our own page master templates -->
  <!-- ===================================== -->

  <xsl:template name="user.pagemasters">
    <!-- page sequence setup for chapter pages -->
    <fo:page-sequence-master master-name="chapter">
      <fo:repeatable-page-master-alternatives>
        <fo:conditional-page-master-reference master-reference="blank"
                                              blank-or-not-blank="blank"/>
        <fo:conditional-page-master-reference master-reference="chapter-first"
                                              page-position="first"/>
        <fo:conditional-page-master-reference master-reference="body-odd"
                                              odd-or-even="odd"/>
        <fo:conditional-page-master-reference
                                              odd-or-even="even">
          <xsl:attribute name="master-reference">
            <xsl:choose>
              <xsl:when test="$double.sided != 0">body-even</xsl:when>
              <xsl:otherwise>body-odd</xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
        </fo:conditional-page-master-reference>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>

        <!-- setup back matter -->
    <fo:page-sequence-master master-name="appendix">
      <fo:repeatable-page-master-alternatives>
        <fo:conditional-page-master-reference master-reference="blank"
                                              blank-or-not-blank="blank"/>
        <fo:conditional-page-master-reference master-reference="appendix-first"
                                              page-position="first"/>
        <fo:conditional-page-master-reference master-reference="back-odd"
                                              odd-or-even="odd"/>
        <fo:conditional-page-master-reference
                                              odd-or-even="even">
          <xsl:attribute name="master-reference">
            <xsl:choose>
              <xsl:when test="$double.sided != 0">back-even</xsl:when>
              <xsl:otherwise>back-odd</xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
        </fo:conditional-page-master-reference>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>

    <!-- chapter master page definition -->
    <fo:simple-page-master master-name="chapter-first"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="0"
                           margin-bottom="{$page.margin.bottom}">
      <xsl:attribute name="margin-{$direction.align.start}">
        <xsl:value-of select="$page.margin.inner"/>
        <xsl:if test="$fop.extensions != 0">
          <xsl:value-of select="concat(' - (',$title.margin.left,')')"/>
        </xsl:if>
      </xsl:attribute>
      <xsl:attribute name="margin-{$direction.align.end}">
        <xsl:value-of select="$page.margin.outer"/>
      </xsl:attribute>
      <xsl:if test="$axf.extensions != 0">
        <xsl:call-template name="axf-page-master-properties">
          <xsl:with-param name="page.master">chapter-first</xsl:with-param>
        </xsl:call-template>
      </xsl:if>
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.body}"
                      column-count="{$column.count.body}">
        <xsl:attribute name="margin-{$direction.align.start}">
          <xsl:value-of select="$body.margin.inner"/>
        </xsl:attribute>
        <xsl:attribute name="margin-{$direction.align.end}">
          <xsl:value-of select="$body.margin.outer"/>
        </xsl:attribute>
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-first"
                        extent="{$region.before.extent}"
                        precedence="{$region.before.precedence}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-first"
                       extent="{$region.after.extent}"
                        precedence="{$region.after.precedence}"
                       display-align="after"/>
      <xsl:call-template name="region.inner">
        <xsl:with-param name="sequence">first</xsl:with-param>
        <xsl:with-param name="pageclass">body</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="region.outer">
        <xsl:with-param name="sequence">first</xsl:with-param>
        <xsl:with-param name="pageclass">body</xsl:with-param>
      </xsl:call-template>
    </fo:simple-page-master>

    <!-- appendix pages -->
    <fo:simple-page-master master-name="appendix-first"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="0"
                           margin-bottom="{$page.margin.bottom}">
      <xsl:attribute name="margin-{$direction.align.start}">
        <xsl:value-of select="$page.margin.inner"/>
        <xsl:if test="$fop.extensions != 0">
          <xsl:value-of select="concat(' - (',$title.margin.left,')')"/>
        </xsl:if>
      </xsl:attribute>
      <xsl:attribute name="margin-{$direction.align.end}">
        <xsl:value-of select="$page.margin.outer"/>
      </xsl:attribute>
      <xsl:if test="$axf.extensions != 0">
        <xsl:call-template name="axf-page-master-properties">
          <xsl:with-param name="page.master">appendix-first</xsl:with-param>
        </xsl:call-template>
      </xsl:if>
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.back}"
                      column-count="{$column.count.back}">
        <xsl:attribute name="margin-{$direction.align.start}">
          <xsl:value-of select="$body.margin.inner"/>
        </xsl:attribute>
        <xsl:attribute name="margin-{$direction.align.end}">
          <xsl:value-of select="$body.margin.outer"/>
        </xsl:attribute>
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-first"
                        extent="{$region.before.extent}"
                        precedence="{$region.before.precedence}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-first"
                       extent="{$region.after.extent}"
                        precedence="{$region.after.precedence}"
                       display-align="after"/>
      <xsl:call-template name="region.inner">
        <xsl:with-param name="sequence">first</xsl:with-param>
        <xsl:with-param name="pageclass">back</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="region.outer">
        <xsl:with-param name="sequence">first</xsl:with-param>
        <xsl:with-param name="pageclass">back</xsl:with-param>
      </xsl:call-template>
    </fo:simple-page-master>

  </xsl:template>

  <!-- customize the book's title page text so that long lines split
       appropriately. Also, use attribute set (defined above) for all
       text styling. -->
  <xsl:template match="d:title" mode="book.titlepage.recto.auto.mode">
    <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="book.titlepage.recto.style.title">
      <xsl:call-template name="division.title">
        <xsl:with-param name="node" select="ancestor-or-self::d:book[1]"/>
        <xsl:with-param name="titlepage" select="1"/>
      </xsl:call-template>
    </fo:block>
  </xsl:template>

  <!-- customize the division.title so that titlepage titles split on colons -->
  <xsl:template name="division.title">
    <xsl:param name="node" select="."/>
    <xsl:param name="titlepage"/>
    <xsl:variable name="id">
      <xsl:call-template name="object.id">
        <xsl:with-param name="object" select="$node"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="title">
      <xsl:apply-templates select="$node" mode="object.title.markup">
        <xsl:with-param name="allow-anchors" select="1"/>
      </xsl:apply-templates>
    </xsl:variable>

    <xsl:if test="$passivetex.extensions != 0">
      <fotex:bookmark xmlns:fotex="http://www.tug.org/fotex"
                      fotex-bookmark-level="1"
                      fotex-bookmark-label="{$id}">
        <xsl:value-of select="$title"/>
      </fotex:bookmark>
    </xsl:if>

    <fo:block keep-with-next.within-column="always"
              hyphenate="false">
      <xsl:if test="$axf.extensions != 0">
        <xsl:attribute name="axf:outline-level">
          <xsl:choose>
            <xsl:when test="count($node/ancestor::*) > 0">
              <xsl:value-of select="count($node/ancestor::*)"/>
            </xsl:when>
            <xsl:otherwise>1</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:attribute name="axf:outline-expand">false</xsl:attribute>
        <xsl:attribute name="axf:outline-title">
          <xsl:value-of select="normalize-space($title)"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="$titlepage = 1">
          <xsl:call-template name="tokenizeTitle">
            <xsl:with-param name="list" select="$title"/>
            <xsl:with-param name="delimiter" select="':'"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:copy-of select="$title"/>
        </xsl:otherwise>
      </xsl:choose>
    </fo:block>
  </xsl:template>

  <!-- custom template to split titlepage titlestrings on colons -->
  <xsl:template name="tokenizeTitle">
    <!--passed template parameter -->
    <xsl:param name="list"/>
    <xsl:param name="delimiter"/>
    <xsl:choose>
      <xsl:when test="contains($list, $delimiter)">
        <fo:block hyphenate="false">
          <!-- get everything in front of the first delimiter -->
          <xsl:value-of select="substring-before($list,$delimiter)"/>:
        </fo:block>
        <xsl:call-template name="tokenizeTitle">
          <!-- store anything left in another variable -->
          <xsl:with-param name="list" select="substring-after($list,$delimiter)"/>
          <xsl:with-param name="delimiter" select="$delimiter"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="$list = ''">
            <xsl:text/>
          </xsl:when>
          <xsl:otherwise>
            <fo:block hyphenate="false">
              <xsl:value-of select="$list"/>
            </fo:block>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="breadcrumby">
    <xsl:param name="node" select="."/>
    <xsl:message>
      <xsl:text>local name:</xsl:text>
      <xsl:value-of select="local-name($node)"/>
    </xsl:message>
    <xsl:if test="parent::*">
      <xsl:call-template name="breadcrumby">
        <xsl:with-param name="node" select="parent::*"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <!-- customize the book's title page with the inclusion of an absolutely-positioned Perforce logo -->
  <xsl:template name="book.titlepage.recto">
    <xsl:choose>
      <xsl:when test="d:bookinfo/d:title">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:title"/>
      </xsl:when>
      <xsl:when test="d:info/d:title">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:title"/>
      </xsl:when>
      <xsl:when test="d:title">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:title"/>
        <fo:block-container position="absolute" top="-18pt" left="-40pt" width="160pt" height="40pt">
          <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" text-align="center">
            <fo:external-graphic src="url({$logo.url})" width="160pt" height="39.36pt" content-width="scale-to-fit" content-height="scale-to-fit"/>
          </fo:block>
        </fo:block-container>
      </xsl:when>
    </xsl:choose>

    <xsl:choose>
      <xsl:when test="d:bookinfo/d:subtitle">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:subtitle"/>
      </xsl:when>
      <xsl:when test="d:info/d:subtitle">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:subtitle"/>
      </xsl:when>
      <xsl:when test="d:subtitle">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:subtitle"/>
      </xsl:when>
    </xsl:choose>

    <xsl:choose>
      <xsl:when test="d:bookinfo/d:pubdate">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:pubdate"/>
      </xsl:when>
      <xsl:when test="d:info/d:pubdate">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:pubdate"/>
      </xsl:when>
      <xsl:when test="d:pubdate">
        <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:pubdate"/>
      </xsl:when>
    </xsl:choose>

    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:corpauthor"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:corpauthor"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:authorgroup"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:authorgroup"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:author"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:author"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:bookinfo/d:itermset"/>
    <xsl:apply-templates mode="book.titlepage.recto.auto.mode" select="d:info/d:itermset"/>
  </xsl:template>

  <!-- customize the presentation of the title/subtitle on the titlepage
       verso side -->
  <xsl:template name="book.verso.title">
    <fo:block>
      <xsl:apply-templates mode="titlepage.mode"/>

      <xsl:if test="following-sibling::d:subtitle
                    |following-sibling::d:info/d:subtitle
                    |following-sibling::d:bookinfo/d:subtitle">
        <fo:block>
          <xsl:apply-templates select="(following-sibling::d:subtitle
                                       |following-sibling::d:info/d:subtitle
                                       |following-sibling::d:bookinfo/d:subtitle)[1]"
                               mode="book.verso.subtitle.mode"/>
        </fo:block>
      </xsl:if>
    </fo:block>
  </xsl:template>

  <!-- customize the book's copyright info on the titlepage verso side -->
  <xsl:template match="d:copyright" mode="book.titlepage.verso.auto.mode">
    <fo:block xmlns:fo="http://www.w3.org/1999/XSL/Format" xsl:use-attribute-sets="book.titlepage.verso.style.copyright">
      <xsl:apply-templates select="." mode="book.titlepage.verso.mode"/>
    </fo:block>
  </xsl:template>

  <!-- customize the select.user.pagemaster template to select the
       chapter master page when presenting a chapter. -->
  <xsl:template name="select.user.pagemaster">
    <xsl:param name="element"/>
    <xsl:param name="pageclass"/>
    <xsl:param name="default-pagemaster"/>

    <xsl:choose>
      <xsl:when test="$element = 'chapter'">chapter</xsl:when>
      <xsl:when test="$element = 'appendix'">appendix</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$default-pagemaster"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- customized set.flow.properties that sets the indents for chapters -->
  <xsl:template name="set.flow.properties">
    <xsl:param name="element" select="local-name(.)"/>
    <xsl:param name="master-reference" select="''"/>

    <!-- This template is called after each <fo:flow> starts. -->
    <!-- Customize this template to set attributes on fo:flow -->

    <!-- remove -draft from reference -->
    <xsl:variable name="pageclass">
      <xsl:choose>
        <xsl:when test="contains($master-reference, '-draft')">
          <xsl:value-of select="substring-before($master-reference, '-draft')"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$master-reference"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="$fop.extensions != 0 or $passivetex.extensions != 0">
        <!-- body.start.indent does not work well with these processors -->
      </xsl:when>
      <xsl:when test="starts-with($pageclass, 'body') or
                      starts-with($pageclass, 'lot') or
                      starts-with($pageclass, 'front') or
                      $element = 'preface' or
                      $element = 'chapter' or
                      $element = 'appendix' or
                      (starts-with($pageclass, 'back') and
                      $element = 'appendix')">
        <xsl:attribute name="start-indent">
          <xsl:value-of select="$body.start.indent"/>
        </xsl:attribute>
        <xsl:attribute name="end-indent">
          <xsl:value-of select="$body.end.indent"/>
        </xsl:attribute>
      </xsl:when>
    </xsl:choose>

  </xsl:template>

  <!-- ================================================== -->
  <!-- customize tables to match existing Perforce styles -->
  <!-- ================================================== -->

  <!-- customized table.row.properties to apply color parameters -->
  <xsl:template name="table.row.properties">
    <xsl:variable name="tabstyle">
      <xsl:call-template name="tabstyle"/>
    </xsl:variable>

    <xsl:variable name="row-height">
      <xsl:if test="processing-instruction('dbfo')">
        <xsl:call-template name="pi.dbfo_row-height"/>
      </xsl:if>
    </xsl:variable>

    <xsl:if test="$row-height != ''">
      <xsl:attribute name="block-progression-dimension">
        <xsl:value-of select="$row-height"/>
      </xsl:attribute>
    </xsl:if>

    <xsl:variable name="bgcolor">
      <xsl:call-template name="pi.dbfo_bgcolor"/>
    </xsl:variable>

    <xsl:variable name="rownum">
      <xsl:number from="d:tgroup" count="d:row"/>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="ancestor::d:thead">
        <xsl:attribute name="background-color">
          <xsl:value-of select="$table.row.head.bgcolor"/>
        </xsl:attribute>
      </xsl:when>
      <xsl:when test="$rownum mod 2 = 0 and not(d:tfoot)">
        <xsl:attribute name="background-color">
          <xsl:value-of select="$table.row.even.bgcolor"/>
        </xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="background-color">
          <xsl:value-of select="$table.row.odd.bgcolor"/>
        </xsl:attribute>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:if test="$bgcolor != ''">
      <xsl:attribute name="background-color">
        <xsl:value-of select="$bgcolor"/>
      </xsl:attribute>
    </xsl:if>

    <!-- Keep header row with next row -->
    <xsl:if test="ancestor::d:thead">
      <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
    </xsl:if>
  </xsl:template>

  <!-- customized table.cell properties to control colsep, rowsep, and background colors -->
  <xsl:template name="table.cell.properties">
    <xsl:param name="bgcolor.pi" select="''"/>
    <xsl:param name="rowsep.inherit" select="1"/>
    <xsl:param name="colsep.inherit" select="1"/>
    <xsl:param name="col" select="1"/>
    <xsl:param name="valign.inherit" select="''"/>
    <xsl:param name="align.inherit" select="''"/>
    <xsl:param name="char.inherit" select="''"/>

    <xsl:variable name="rownum">
      <xsl:number from="d:tgroup" count="d:row"/>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="ancestor::d:tgroup">
        <xsl:attribute name="padding-top">0.5em</xsl:attribute>
        <xsl:attribute name="padding-left">0.5em</xsl:attribute>
        <xsl:attribute name="padding-bottom">0.5em</xsl:attribute>
        <xsl:attribute name="padding-right">0.5em</xsl:attribute>

        <xsl:if test="$bgcolor.pi != ''">
          <xsl:attribute name="background-color">
            <xsl:value-of select="$bgcolor.pi"/>
          </xsl:attribute>
        </xsl:if>

        <xsl:choose>
          <xsl:when test="ancestor::d:thead">
            <xsl:attribute name="background-color">
              <xsl:value-of select="$table.row.head.bgcolor"/>
            </xsl:attribute>
          </xsl:when>
          <xsl:when test="$rownum mod 2 = 0 and not(d:tfoot)">
            <xsl:attribute name="background-color">
              <xsl:value-of select="$table.row.even.bgcolor"/>
            </xsl:attribute>
          </xsl:when>
          <xsl:otherwise>
            <xsl:attribute name="background-color">
              <xsl:value-of select="$table.row.odd.bgcolor"/>
            </xsl:attribute>
          </xsl:otherwise>
        </xsl:choose>

        <xsl:if test="$rowsep.inherit &gt; 0">
          <xsl:choose>
            <xsl:when test="ancestor::d:thead and @role='noborder'">
              <xsl:attribute name="padding-bottom">0</xsl:attribute>
            </xsl:when>
            <xsl:when test="ancestor::d:thead">
              <xsl:call-template name="border">
                <xsl:with-param name="side" select="'bottom'"/>
                <xsl:with-param name="thickness" select="$table.cell.border.thickness.head"/>
                <xsl:with-param name="color" select="$table.cell.border.color.head"/>
              </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
              <xsl:call-template name="border">
                <xsl:with-param name="side" select="'bottom'"/>
                <xsl:with-param name="thickness" select="$table.cell.border.thickness.row"/>
                <xsl:with-param name="color" select="$table.cell.border.color.row"/>
              </xsl:call-template>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>

        <!-- disabled to remove border separating columns
        <xsl:if test="$colsep.inherit &gt; 0 and
                        $col &lt; (ancestor::d:tgroup/@cols|ancestor::d:entrytbl/@
  cols)[last()]">
          <xsl:call-template name="border">
            <xsl:with-param name="side" select="'end'"/>
          </xsl:call-template>
        </xsl:if>
        -->

        <xsl:if test="$valign.inherit != ''">
          <xsl:attribute name="display-align">
            <xsl:choose>
              <xsl:when test="$valign.inherit='top'">before</xsl:when>
              <xsl:when test="$valign.inherit='middle'">center</xsl:when>
              <xsl:when test="$valign.inherit='bottom'">after</xsl:when>
              <xsl:otherwise>
                <xsl:message>
                  <xsl:text>Unexpected valign value: </xsl:text>
                  <xsl:value-of select="$valign.inherit"/>
                  <xsl:text>, center used.</xsl:text>
                </xsl:message>
                <xsl:text>center</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
        </xsl:if>

        <xsl:choose>
          <xsl:when test="$align.inherit = 'char' and $char.inherit != ''">
            <xsl:attribute name="text-align">
              <xsl:value-of select="$char.inherit"/>
            </xsl:attribute>
          </xsl:when>
          <xsl:when test="$align.inherit != ''">
            <xsl:attribute name="text-align">
              <xsl:value-of select="$align.inherit"/>
            </xsl:attribute>
          </xsl:when>
        </xsl:choose>
      </xsl:when>

      <xsl:otherwise>
        <!-- HTML table -->
        <xsl:if test="$bgcolor.pi != ''">
          <xsl:attribute name="background-color">
            <xsl:value-of select="$bgcolor.pi"/>
          </xsl:attribute>
        </xsl:if>

        <xsl:if test="$align.inherit != ''">
          <xsl:attribute name="text-align">
            <xsl:value-of select="$align.inherit"/>
          </xsl:attribute>
        </xsl:if>

        <xsl:if test="$valign.inherit != ''">
          <xsl:attribute name="display-align">
            <xsl:choose>
              <xsl:when test="$valign.inherit='top'">before</xsl:when>
              <xsl:when test="$valign.inherit='middle'">center</xsl:when>
              <xsl:when test="$valign.inherit='bottom'">after</xsl:when>
              <xsl:otherwise>
                <xsl:message>
                  <xsl:text>Unexpected valign value: </xsl:text>
                  <xsl:value-of select="$valign.inherit"/>
                  <xsl:text>, center used.</xsl:text>
                </xsl:message>
                <xsl:text>center</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
        </xsl:if>

        <xsl:call-template name="html.table.cell.rules"/>

      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- customize table.cell.block.properties -->
  <xsl:template name="table.cell.block.properties">
    <!-- highlight this entry? -->
    <xsl:choose>
      <xsl:when test="ancestor::d:thead or ancestor::d:tfoot">
        <xsl:attribute name="font-family">MyriadPro, sans-serif</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
      </xsl:when>
      <!-- Make row headers bold too -->
      <xsl:when test="ancestor::d:tbody and
                      (ancestor::d:table[@rowheader = 'firstcol'] or
                      ancestor::d:informaltable[@rowheader = 'firstcol']) and
                      ancestor-or-self::d:entry[1][count(preceding-sibling::d:entry) = 0]">
        <xsl:attribute name="font-family">MyriadPro, sans-serif</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <!-- customize the table border template to allow per tow thickness and color -->
  <xsl:template name="border">
    <xsl:param name="side" select="'start'"/>
    <xsl:param name="thickness" select="$table.cell.border.thickness"/>
    <xsl:param name="color" select="$table.cell.border.color"/>

    <xsl:attribute name="border-{$side}-width">
      <xsl:value-of select="$thickness"/>
    </xsl:attribute>
    <xsl:attribute name="border-{$side}-style">
      <xsl:value-of select="$table.cell.border.style"/>
    </xsl:attribute>
    <xsl:attribute name="border-{$side}-color">
      <xsl:value-of select="$color"/>
    </xsl:attribute>
  </xsl:template>

  <!-- ============================================================= -->
  <!-- TOC customizations                                            -->
  <!-- ============================================================= -->

  <!-- customized toc.line to change presentation for major book sections,
       eg. chapter, preface, .etc -->
  <xsl:template name="toc.line">
    <xsl:param name="toc-context" select="NOTANODE"/>

    <xsl:variable name="id">
      <xsl:call-template name="object.id"/>
    </xsl:variable>

    <xsl:variable name="label">
      <xsl:apply-templates select="." mode="label.markup"/>
    </xsl:variable>

    <xsl:variable name="level">
      <xsl:value-of select="local-name()"/>
    </xsl:variable>

    <xsl:variable name="bigger">
      <xsl:choose>
        <xsl:when test="contains('preface|chapter|reference|glossary|index|appendix', $level)">
          <xsl:text>1</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>0</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="$bigger > 0">
        <xsl:call-template name="toc.line.larger">
          <xsl:with-param name="toc-context" select="$toc-context"/>
          <xsl:with-param name="id" select="$id"/>
          <xsl:with-param name="label" select="$label"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="toc.line.regular">
          <xsl:with-param name="toc-context" select="$toc-context"/>
          <xsl:with-param name="id" select="$id"/>
          <xsl:with-param name="label" select="$label"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="toc.line.regular">
    <xsl:param name="toc-context" select="NOTANODE"/>
    <xsl:param name="id"/>
    <xsl:param name="label"/>

    <fo:block xsl:use-attribute-sets="toc.line.properties">
      <fo:basic-link internal-destination="{$id}">
        <xsl:if test="$label != ''">
          <xsl:copy-of select="$label"/>
          <xsl:value-of select="$autotoc.label.separator"/>
        </xsl:if>
        <xsl:apply-templates select="." mode="titleabbrev.markup"/>
      </fo:basic-link>
      <xsl:text> </xsl:text>
      <fo:leader leader-pattern="dots"
                 leader-pattern-width="3pt"
                 leader-alignment="reference-area"
                 keep-with-next.within-line="always"/>
      <xsl:text> </xsl:text>
      <fo:basic-link internal-destination="{$id}">
        <fo:page-number-citation ref-id="{$id}"/>
      </fo:basic-link>
    </fo:block>
  </xsl:template>

  <xsl:template name="toc.line.larger">
    <xsl:param name="toc-context" select="NOTANODE"/>
    <xsl:param name="id"/>
    <xsl:param name="label"/>

    <xsl:variable name="title">
      <xsl:apply-templates select="." mode="title.markup"/>
    </xsl:variable>

    <xsl:variable name="level">
      <xsl:value-of select="local-name()"/>
    </xsl:variable>

    <fo:block xsl:use-attribute-sets="toc.line.properties.larger">
      <xsl:if test="$label != ''">
        <fo:inline keep-with-next.within-line="always"
          xsl:use-attribute-sets="toc.line.properties.larger.label">
          <fo:basic-link internal-destination="{$id}">
              <xsl:call-template name="gentext">
                <xsl:with-param name="key" select="$level"/>
              </xsl:call-template>
              <xsl:text> </xsl:text>
              <xsl:copy-of select="$label"/>
              <xsl:text> </xsl:text>
          </fo:basic-link>
        </fo:inline>
        <fo:inline keep-with-next.within-line="always"
          xsl:use-attribute-sets="toc.line.properties.larger.label">
          <xsl:text> </xsl:text>
        </fo:inline>
      </xsl:if>
      <fo:basic-link internal-destination="{$id}">
        <xsl:apply-templates select="." mode="titleabbrev.markup"/>
      </fo:basic-link>
      <xsl:text> </xsl:text>
      <fo:leader leader-pattern="dots"
                 leader-pattern-width="3pt"
                 leader-alignment="reference-area"
                 keep-with-next.within-line="always"/>
      <xsl:text> </xsl:text>
      <fo:basic-link internal-destination="{$id}">
        <fo:page-number-citation ref-id="{$id}"/>
      </fo:basic-link>
    </fo:block>
  </xsl:template>

  <!-- ============================================================= -->
  <!-- glossary customizations                                       -->
  <!-- ============================================================= -->

  <!-- perform case manipulation to ensure 'See Also' is presented as 'See also' -->
  <!-- in this case, for glossary.as.list -->
  <xsl:template match="d:glossentry/d:glossdef" mode="glossary.as.list">
    <xsl:apply-templates select="*[local-name(.) != 'glossseealso']"/>
    <xsl:if test="d:glossseealso">
      <fo:block>
        <xsl:variable name="template">
          <xsl:call-template name="gentext.template">
            <xsl:with-param name="context" select="'glossary'"/>
            <xsl:with-param name="name" select="'seealso'"/>
          </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="lowercase">abcdefghijklmnopqrstuvwxyz</xsl:variable>
        <xsl:variable name="uppercase">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
        <xsl:variable name="template2">
          <xsl:value-of select="translate(substring($template, 1, 1), $lowercase, $uppercase)"/>
          <xsl:value-of select="translate(substring($template, 2), $uppercase, $lowercase)"/>
        </xsl:variable>
        <xsl:variable name="title">
          <xsl:apply-templates select="d:glossseealso" mode="glossary.as.list"/>
        </xsl:variable>
        <xsl:call-template name="substitute-markup">
          <xsl:with-param name="template" select="$template2"/>
          <xsl:with-param name="title" select="$title"/>
        </xsl:call-template>
      </fo:block>
    </xsl:if>
  </xsl:template>

  <!-- in this case, for glossary.as.blocks -->
  <xsl:template match="d:glossentry/d:glossdef" mode="glossary.as.blocks">
    <xsl:apply-templates select="*[local-name(.) != 'glossseealso']"
      mode="glossary.as.blocks"/>
    <xsl:if test="d:glossseealso">
      <fo:block>
        <xsl:variable name="template">
          <xsl:call-template name="gentext.template">
            <xsl:with-param name="context" select="'glossary'"/>
            <xsl:with-param name="name" select="'seealso'"/>
          </xsl:call-template>
        </xsl:variable>
        <xsl:variable name="lowercase">abcdefghijklmnopqrstuvwxyz</xsl:variable>
        <xsl:variable name="uppercase">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
        <xsl:variable name="template2">
          <xsl:value-of select="translate(substring($template, 1, 1), $lowercase, $uppercase)"/>
          <xsl:value-of select="translate(substring($template, 2), $uppercase, $lowercase)"/>
        </xsl:variable>
        <xsl:variable name="title">
          <xsl:apply-templates select="d:glossseealso" mode="glossary.as.blocks"/>
        </xsl:variable>
        <xsl:call-template name="substitute-markup">
          <xsl:with-param name="template" select="$template2"/>
          <xsl:with-param name="title" select="$title"/>
        </xsl:call-template>
      </fo:block>
    </xsl:if>
  </xsl:template>

  <!-- ============================================================= -->
  <!-- verbatim customizations                                       -->
  <!-- ============================================================= -->

  <!-- customize literallayout and screen content to avoid initial
       blank line due to preferred markup style -->
  <xsl:template match="d:literallayout/text()[1]">
    <xsl:choose>
      <xsl:when test=".='&#xA;' or .='&#xD;&#xA;'"/>
      <xsl:otherwise>
        <xsl:value-of select="."/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- customize program listings to apply styles -->
  <xsl:template match="d:programlisting|d:screen|d:synopsis">
    <xsl:param name="suppress-numbers" select="'0'"/>
    <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>

    <xsl:variable name="content">
      <xsl:choose>
        <xsl:when test="$suppress-numbers = '0'
                        and @linenumbering = 'numbered'
                        and $use.extensions != '0'
                        and $linenumbering.extension != '0'">
          <xsl:call-template name="number.rtf.lines">
            <xsl:with-param name="rtf">
              <xsl:choose>
                <xsl:when test="$highlight.source != 0">
                  <xsl:call-template name="apply-highlighting"/>
                </xsl:when>
                <xsl:otherwise>
                  <xsl:apply-templates/>
                </xsl:otherwise>
              </xsl:choose>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:choose>
            <xsl:when test="$highlight.source != 0">
              <xsl:call-template name="apply-highlighting"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="keep.together">
      <xsl:choose>
        <xsl:when test="self::d:programlisting and @role = 'split'">
          <xsl:text>auto</xsl:text>
        </xsl:when>
        <xsl:when test="self::d:programlisting or self::d:screen">
          <xsl:text>always</xsl:text>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="pi.dbfo_keep-together"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:variable name="block.content">
      <xsl:choose>
        <xsl:when test="$shade.verbatim != 0">
          <fo:block id="{$id}"
               xsl:use-attribute-sets="monospace.verbatim.properties shade.verbatim.style">
            <xsl:if test="$keep.together != ''">
              <xsl:attribute name="keep-together.within-column"><xsl:value-of
              select="$keep.together"/></xsl:attribute>
            </xsl:if>
            <xsl:choose>
              <xsl:when test="$hyphenate.verbatim != 0 and
                              $exsl.node.set.available != 0">
                <xsl:apply-templates select="exsl:node-set($content)"
                                     mode="hyphenate.verbatim"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:copy-of select="$content"/>
              </xsl:otherwise>
            </xsl:choose>
          </fo:block>
        </xsl:when>
        <xsl:when test="self::d:programlisting">
          <fo:block id="{$id}"
                    xsl:use-attribute-sets="monospace.verbatim.properties programlisting.styles">
            <xsl:if test="$keep.together != ''">
              <xsl:attribute name="keep-together.within-column"><xsl:value-of
              select="$keep.together"/></xsl:attribute>
            </xsl:if>
            <xsl:choose>
              <xsl:when test="$hyphenate.verbatim != 0 and
                              $exsl.node.set.available != 0">
                <xsl:apply-templates select="exsl:node-set($content)"
                                     mode="hyphenate.verbatim"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:copy-of select="$content"/>
              </xsl:otherwise>
            </xsl:choose>
          </fo:block>
        </xsl:when>
        <xsl:when test="self::d:screen">
          <fo:block id="{$id}"
                    xsl:use-attribute-sets="monospace.verbatim.properties screen.styles">
            <xsl:if test="$keep.together != ''">
              <xsl:attribute name="keep-together.within-column"><xsl:value-of
              select="$keep.together"/></xsl:attribute>
            </xsl:if>
            <xsl:choose>
              <xsl:when test="$hyphenate.verbatim != 0 and
                              $exsl.node.set.available != 0">
                <xsl:apply-templates select="exsl:node-set($content)"
                                     mode="hyphenate.verbatim"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:copy-of select="$content"/>
              </xsl:otherwise>
            </xsl:choose>
          </fo:block>
        </xsl:when>
        <xsl:otherwise>
          <fo:block id="{$id}"
                    xsl:use-attribute-sets="monospace.verbatim.properties">
            <xsl:if test="$keep.together != ''">
              <xsl:attribute name="keep-together.within-column"><xsl:value-of
              select="$keep.together"/></xsl:attribute>
            </xsl:if>
            <xsl:choose>
              <xsl:when test="$hyphenate.verbatim != 0 and
                              $exsl.node.set.available != 0">
                <xsl:apply-templates select="exsl:node-set($content)"
                                     mode="hyphenate.verbatim"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:copy-of select="$content"/>
              </xsl:otherwise>
            </xsl:choose>
          </fo:block>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:choose>
      <!-- Need a block-container for these features -->
      <xsl:when test="@width != '' or
                      (self::d:programlisting and
                      starts-with($writing.mode, 'rl'))">
        <fo:block-container start-indent="0pt" end-indent="0pt">
          <xsl:if test="@width != ''">
            <xsl:attribute name="width">
              <xsl:value-of select="concat(@width, '*', $monospace.verbatim.font.width)"/>
            </xsl:attribute>
          </xsl:if>
          <!-- All known program code is left-to-right -->
          <xsl:if test="self::d:programlisting and
                        starts-with($writing.mode, 'rl')">
            <xsl:attribute name="writing-mode">lr-tb</xsl:attribute>
          </xsl:if>
          <xsl:copy-of select="$block.content"/>
        </fo:block-container>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="$block.content"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- customize command tags to be bold and monospaced -->
  <xsl:template match="d:command">
    <xsl:call-template name="inline.monoseq"/>
  </xsl:template>

  <!-- add a custom inline style for parameter tags to use: adds
       keep-together to prevent wrapping on the hyphen -->
  <xsl:template name="inline.italicmonoseqkeep">
    <xsl:param name="content">
      <xsl:call-template name="simple.xlink">
        <xsl:with-param name="content">
          <xsl:apply-templates/>
        </xsl:with-param>
      </xsl:call-template>
    </xsl:param>

    <fo:inline font-style="italic" xsl:use-attribute-sets="monospace.properties" keep-together.within-line="always">
      <xsl:call-template name="anchor"/>
      <xsl:if test="@dir">
        <xsl:attribute name="direction">
          <xsl:choose>
            <xsl:when test="@dir = 'ltr' or @dir = 'lro'">ltr</xsl:when>
            <xsl:otherwise>rtl</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
      </xsl:if>
      <xsl:copy-of select="$content"/>
    </fo:inline>
  </xsl:template>

  <!-- customize the inline parameter style -->
  <xsl:template match="d:parameter">
    <xsl:call-template name="inline.italicmonoseqkeep"/>
  </xsl:template>

  <!-- ============================================================= -->
  <!-- admonition customizations                                     -->
  <!-- ============================================================= -->

  <!-- customize the width of admon graphics -->
  <xsl:template match="*" mode="admon.graphic.width">
    <xsl:param name="node" select="."/>
    <xsl:text>18pt</xsl:text>
  </xsl:template>

  <xsl:attribute-set name="admonition.title.properties">
    <xsl:attribute name="font-size">10pt</xsl:attribute>
    <xsl:attribute name="font-family">MyriadPro, sans-serif</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="hyphenate">false</xsl:attribute>
    <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="admonition.table.properties">
    <xsl:attribute name="table-layout">fixed</xsl:attribute>
    <xsl:attribute name="width">100%</xsl:attribute>
    <xsl:attribute name="space-before.minimum">0.5em</xsl:attribute>
    <xsl:attribute name="space-before.optimum">1em</xsl:attribute>
    <xsl:attribute name="space-before.maximum">2em</xsl:attribute>
    <xsl:attribute name="space-after.minimum">0.5em</xsl:attribute>
    <xsl:attribute name="space-after.optimum">1em</xsl:attribute>
    <xsl:attribute name="space-after.maximum">2em</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="admonition.title.cell.properties">
    <xsl:attribute name="border-width">0.5pt</xsl:attribute>
    <xsl:attribute name="border-style">solid</xsl:attribute>
    <xsl:attribute name="border-color">black</xsl:attribute>
    <xsl:attribute name="border-right-width">0pt</xsl:attribute>
    <xsl:attribute name="padding-top">2pt</xsl:attribute>
    <xsl:attribute name="padding-right">4pt</xsl:attribute>
    <xsl:attribute name="padding-bottom">2pt</xsl:attribute>
    <xsl:attribute name="padding-left">4pt</xsl:attribute>
    <xsl:attribute name="margin-top">0.5pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="admonition.body.cell.properties">
    <xsl:attribute name="border-style">solid</xsl:attribute>
    <xsl:attribute name="border-color">black</xsl:attribute>
    <xsl:attribute name="border-top-width">0.5pt</xsl:attribute>
    <xsl:attribute name="border-right-width">1pt</xsl:attribute>
    <xsl:attribute name="border-bottom-width">0.5pt</xsl:attribute>
    <xsl:attribute name="border-left-width">4pt</xsl:attribute>
    <xsl:attribute name="padding-top">4pt</xsl:attribute>
    <xsl:attribute name="padding-right">1em</xsl:attribute>
    <xsl:attribute name="padding-bottom">4pt</xsl:attribute>
    <xsl:attribute name="padding-left">1em</xsl:attribute>
    <xsl:attribute name="background-color">#f8f8f8</xsl:attribute>
  </xsl:attribute-set>

  <xsl:template name="admonition.border.color">
    <xsl:param name="type" select="unspecified"/>
    <xsl:param name="body" select="0"/>

    <xsl:choose>
      <xsl:when test="$type = 'note'">
        <xsl:attribute name="border-color">#006688</xsl:attribute>
        <xsl:if test="$body > 0">
          <xsl:attribute name="background-color">#d9e8ed</xsl:attribute>
        </xsl:if>
      </xsl:when>
      <xsl:when test="$type = 'important'">
        <xsl:attribute name="border-color">#440088</xsl:attribute>
        <xsl:if test="$body > 0">
          <xsl:attribute name="background-color">#e3d9ed</xsl:attribute>
        </xsl:if>
      </xsl:when>
      <xsl:when test="$type = 'warning'">
        <xsl:attribute name="border-color">#ff0000</xsl:attribute>
        <xsl:if test="$body > 0">
          <xsl:attribute name="background-color">#ffeeee</xsl:attribute>
        </xsl:if>
      </xsl:when>
      <xsl:when test="$type = 'caution'">
        <xsl:attribute name="border-color">#f79319</xsl:attribute>
        <xsl:if test="$body > 0">
          <xsl:attribute name="background-color">#fdeedc</xsl:attribute>
        </xsl:if>
      </xsl:when>
      <xsl:when test="$type = 'tip'">
        <xsl:attribute name="border-color">#0000ff</xsl:attribute>
        <xsl:if test="$body > 0">
          <xsl:attribute name="background-color">#eeeeff</xsl:attribute>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="border-color">black</xsl:attribute>
        <xsl:if test="$body > 0">
          <xsl:attribute name="background-color">#f8f8f8</xsl:attribute>
        </xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="nongraphical.admonition">
    <xsl:variable name="id">
      <xsl:call-template name="object.id"/>
    </xsl:variable>

    <fo:block id="{$id}" >
      <fo:table xsl:use-attribute-sets="admonition.table.properties">
        <fo:table-column column-number="1" column-width="proportional-column-width(1.00)"/>
        <fo:table-column column-number="2" column-width="proportional-column-width(6.00)"/>
        <fo:table-body start-indent="0pt" end-indent="0pt">
          <fo:table-row>
            <fo:table-cell xsl:use-attribute-sets="admonition.title.cell.properties">
              <xsl:call-template name="admonition.border.color">
                <xsl:with-param name="type" select="name(.)"/>
              </xsl:call-template>
              <fo:block xsl:use-attribute-sets="admonition.title.properties">
                <xsl:apply-templates select="." mode="object.title.markup"/>
              </fo:block>
            </fo:table-cell>
            <fo:table-cell xsl:use-attribute-sets="admonition.body.cell.properties"
                           number-rows-spanned="2"
            >
              <xsl:call-template name="admonition.border.color">
                <xsl:with-param name="type" select="name(.)"/>
                <xsl:with-param name="body" select="1"/>
              </xsl:call-template>
              <fo:block xsl:use-attribute-sets="admonition.properties">
                <xsl:apply-templates/>
              </fo:block>
            </fo:table-cell>
          </fo:table-row>
          <fo:table-row>
            <fo:table-cell>
              <fo:block/>
            </fo:table-cell>
          </fo:table-row>
        </fo:table-body>
      </fo:table>
    </fo:block>
  </xsl:template>

  <xsl:template match="d:guibutton">
    <xsl:call-template name="inline.boldseq"/>
  </xsl:template>

  <xsl:template match="d:guiicon">
    <xsl:call-template name="inline.boldseq"/>
  </xsl:template>

  <xsl:template match="d:guilabel">
    <xsl:call-template name="inline.boldseq"/>
  </xsl:template>

  <xsl:template match="d:guimenu">
    <xsl:call-template name="inline.boldseq"/>
  </xsl:template>

  <xsl:template match="d:guimenuitem">
    <xsl:call-template name="inline.boldseq"/>
  </xsl:template>

  <xsl:template match="d:guisubmenu">
    <xsl:call-template name="inline.boldseq"/>
  </xsl:template>

  <!-- add a processing instruction to introduce a page break -->
  <xsl:template match="processing-instruction('pagebreak')">
    <fo:block break-after='page'/>
  </xsl:template>
</xsl:stylesheet>
