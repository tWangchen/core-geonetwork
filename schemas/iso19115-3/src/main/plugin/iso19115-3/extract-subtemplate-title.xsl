<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
  xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:template match="cit:CI_Responsibility">
		 <title><xsl:value-of select="cit:party/cit:CI_Individual/cit:name/gco:CharacterString"/></title>
	</xsl:template>

	<xsl:template match="*">
		<title></title>
	</xsl:template>
	
</xsl:stylesheet>
