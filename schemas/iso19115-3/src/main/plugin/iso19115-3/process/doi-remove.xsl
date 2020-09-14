<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet   xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
    xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
    xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/1.0"
    xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0"
    xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
    xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
    xmlns:geonet="http://www.fao.org/geonetwork"
    exclude-result-prefixes="#all">
    
    <!-- Remove a DOI in the metadata record. -->
    <xsl:output method="xml" indent="yes"/>
    
    <xsl:param name="doi"
        select="'Digital Object Identifier'"/>
    
    <!-- Remove online resources matching DOI protocols. -->
    <xsl:template match="mdb:identificationInfo/*/mri:citation/*/cit:identifier[
        */mcc:codeSpace/gco:CharacterString = $doi]"
        priority="2"/>
    
    <!-- Do a copy of every nodes and attributes -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- Remove geonet:* elements. -->
    <xsl:template match="geonet:*" priority="2"/>
</xsl:stylesheet>
