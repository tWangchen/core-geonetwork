<?xml version="1.0" encoding="UTF-8"?>
<!--  
Stylesheet used to remove a reference to a parent record.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:srv="http://standards.iso.org/iso/19115/-3/srv/2.0"
    xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
    xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0"
    xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
    xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
    xmlns:gn="http://www.fao.org/geonetwork"
    exclude-result-prefixes="#all" version="2.0">
    
    
    <xsl:param name="code"/>
    <xsl:param name="type"/>
    <xsl:param name="forceRemove" select="'false'" />
    <xsl:param name="associationType" select="'dependency'"/>
    
    <!-- Do a copy of every nodes and attributes -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
    
    <xsl:template match="gn:*|mri:associatedResource[mri:MD_AssociatedResource[mri:metadataReference/cit:CI_Citation/cit:identifier/mcc:MD_Identifier/mcc:description/gco:CharacterString=$type]/mri:metadataReference/cit:CI_Citation/cit:identifier/mcc:MD_Identifier/mcc:code/gco:CharacterString = $code and $forceRemove]" priority="2" />
    <xsl:template match="gn:*|mri:associatedResource[mri:MD_AssociatedResource[mri:metadataReference/cit:CI_Citation/cit:identifier/mcc:MD_Identifier/mcc:description/gco:CharacterString=$type]/mri:metadataReference/cit:CI_Citation/cit:identifier/mcc:MD_Identifier/mcc:code/gco:CharacterString = $code and mri:MD_AssociatedResource/mri:associationType/mri:DS_AssociationTypeCode/@codeListValue = $associationType and not($forceRemove)]" priority="2" />
        
        
</xsl:stylesheet>