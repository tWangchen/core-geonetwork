<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
    xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/1.0"
    xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
    xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
    xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
    xmlns:gn="http://www.fao.org/geonetwork"
    exclude-result-prefixes="#all" >
   
    <xsl:param name="name"/>
    <xsl:param name="division"/>
    <xsl:param name="position"/>
    
    <xsl:template match="/mdb:MD_Metadata|*[contains(@gco:isoType, 'mdb:MD_Metadata')]">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:apply-templates select="mdb:metadataIdentifier"/>
            <xsl:apply-templates select="mdb:defaultLocale"/>
            <xsl:apply-templates select="mdb:parentMetadata"/>
            <xsl:apply-templates select="mdb:metadataScope"/>
            <xsl:apply-templates select="mdb:contact"/>
            <mdb:contact>
                <cit:CI_Responsibility>
                    <cit:role>
                        <cit:CI_RoleCode codeList="codeListLocation#CI_RoleCode" codeListValue="owner"/>
                    </cit:role>
                    <cit:party>
                        <cit:CI_Individual>
                            <cit:name>
                                <gco:CharacterString><xsl:value-of select="$name"/></gco:CharacterString>
                            </cit:name>
                            <cit:contactInfo>
                                <cit:CI_Contact>
                                    <cit:contactInstructions>
                                        <gco:CharacterString><xsl:value-of select="$division"/></gco:CharacterString>
                                    </cit:contactInstructions>
                                    <cit:contactType>
                                        <gco:CharacterString>Internal Contact</gco:CharacterString>
                                    </cit:contactType>
                                </cit:CI_Contact>
                            </cit:contactInfo>
                            <cit:positionName>
                                <gco:CharacterString><xsl:value-of select="$position"/></gco:CharacterString>
                            </cit:positionName>
                        </cit:CI_Individual>
                    </cit:party>
                </cit:CI_Responsibility>
            </mdb:contact>  
            <xsl:apply-templates select="mdb:dateInfo"/>
            <xsl:apply-templates select="mdb:metadataStandard"/>
            <xsl:apply-templates select="mdb:metadataProfile"/>
            <xsl:apply-templates select="mdb:alternativeMetadataReference"/>
            <xsl:apply-templates select="mdb:otherLocale"/>
            <xsl:apply-templates select="mdb:metadataLinkage"/>
            <xsl:apply-templates select="mdb:spatialRepresentationInfo"/>
            <xsl:apply-templates select="mdb:referenceSystemInfo"/>
            <xsl:apply-templates select="mdb:metadataExtensionInfo"/>
            <xsl:apply-templates select="mdb:identificationInfo"/>
            <xsl:apply-templates select="mdb:contentInfo"/>
            <xsl:apply-templates select="mdb:distributionInfo"/>
            <xsl:apply-templates select="mdb:dataQualityInfo"/>
            <xsl:apply-templates select="mdb:resourceLineage"/>
            <xsl:apply-templates select="mdb:portrayalCatalogueInfo"/>
            <xsl:apply-templates select="mdb:metadataConstraints"/>
            <xsl:apply-templates select="mdb:applicationSchemaInfo"/>
            <xsl:apply-templates select="mdb:metadataMaintenance"/>
            <xsl:apply-templates select="mdb:acquisitionInformation"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- Remove geonet:* elements. -->
    <xsl:template match="gn:*" priority="2"/>
    
    <!-- Copy everything. -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>
</xsl:stylesheet>
