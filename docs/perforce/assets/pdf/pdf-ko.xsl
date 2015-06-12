<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:d="http://docbook.org/ns/docbook"
  xmlns:fo="http://www.w3.org/1999/XSL/Format"
  version="1.0">

  <!-- ============================================================= -->
  <!-- Import Perforce FO styles, which import DocBook styles        -->
  <!-- ============================================================= -->

  <xsl:import href="pdf.xsl"/>

  <!-- ============================================================= -->
  <!-- Korean-specific FO customizations follow                      -->
  <!-- ============================================================= -->

  <!-- fonts -->
  <xsl:param name="body.font.family">Arial Unicode MS</xsl:param>
  <xsl:param name="title.font.family">Arial Unicode MS</xsl:param>
  <xsl:param name="monospace.font.family">Arial Unicode MS</xsl:param>

  <!-- formal titles -->
  <xsl:attribute-set name="formal.title.properties">
    <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
    <xsl:attribute name="font-weight">normal</xsl:attribute>
    <xsl:attribute name="font-style">normal</xsl:attribute>
  </xsl:attribute-set>

  <!-- styles for titlepage title -->
  <xsl:attribute-set name="book.titlepage.recto.style.title">
    <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="font-size">36pt</xsl:attribute>
    <xsl:attribute name="font-style">normal</xsl:attribute>
    <xsl:attribute name="text-align">left</xsl:attribute>
    <xsl:attribute name="space-before">18pt</xsl:attribute>
  </xsl:attribute-set>

  <!-- styles for titlepage subtitle -->
  <xsl:attribute-set name="book.titlepage.recto.style.subtitle">
    <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
    <xsl:attribute name="font-weight">normal</xsl:attribute>
    <xsl:attribute name="font-size">18pt</xsl:attribute>
    <xsl:attribute name="font-style">normal</xsl:attribute>
    <xsl:attribute name="text-align">left</xsl:attribute>
    <xsl:attribute name="space-before">36pt</xsl:attribute>
  </xsl:attribute-set>

  <!-- styles for titlepage, verso side, copyright -->
  <xsl:attribute-set name="book.titlepage.verso.style.copyright">
    <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
    <xsl:attribute name="font-size">10pt</xsl:attribute>
    <xsl:attribute name="space-before">1em</xsl:attribute>
  </xsl:attribute-set>

  <!-- styles for major TOC lines, e.g. a chapter -->
  <xsl:attribute-set name="toc.line.properties.larger">
    <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
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
    <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
    <xsl:attribute name="font-size">12pt</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="text-align">start</xsl:attribute>
    <xsl:attribute name="space-end">0pt</xsl:attribute>
    <xsl:attribute name="padding-right">10pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="toc.line.properties">
    <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
  </xsl:attribute-set>

  <!-- styles for a chapter's label -->
  <xsl:attribute-set name="chap.label.properties">
    <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
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
    <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
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

  <xsl:param name="local.l10n.xml" select="document('')"/>
  <l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
    <l:l10n language="ko_kr">
      <l:context name="xref">
        <l:template name="page.citation" text=" on page %p"/>
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
    </l:l10n>
  </l:i18n>

  <!-- customize table.cell.block.properties -->
  <xsl:template name="table.cell.block.properties">
    <!-- highlight this entry? -->
    <xsl:choose>
      <xsl:when test="ancestor::d:thead or ancestor::d:tfoot">
        <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
      </xsl:when>
      <!-- Make row headers bold too -->
      <xsl:when test="ancestor::d:tbody and
                      (ancestor::d:table[@rowheader = 'firstcol'] or
                      ancestor::d:informaltable[@rowheader = 'firstcol']) and
                      ancestor-or-self::d:entry[1][count(preceding-sibling::d:entry) = 0]">
        <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:attribute-set name="admonition.title.properties">
    <xsl:attribute name="font-size">10pt</xsl:attribute>
    <xsl:attribute name="font-family">Arial Unicode MS</xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="hyphenate">false</xsl:attribute>
    <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
  </xsl:attribute-set>

</xsl:stylesheet>
