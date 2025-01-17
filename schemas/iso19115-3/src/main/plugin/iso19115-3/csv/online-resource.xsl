<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    xmlns:util="java:java.util.UUID"
    xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
    xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    version="2.0">
    
    <xsl:param name="name" select="''"/>
    <xsl:param name="linkage" select="''"/>
    <xsl:param name="description" select="''"/>
    <xsl:param name="protocol" select="'WWW:LINK-1.0-http--link'"/>
    <xsl:param name="function" select="'information'"/>
    
    
    <xsl:template match="/cit:CI_OnlineResource">
        <xsl:copy>
            <cit:linkage>
                <gco:CharacterString><xsl:value-of select="$linkage"/></gco:CharacterString>
            </cit:linkage>
            <cit:protocol>
                <gco:CharacterString xsi:type="gco:CodeType" codeSpace="http://pid.geoscience.gov.au/def/schema/ga/ISO19115-3-2016/codelist/ga_profile_codelists.xml#gapCI_ProtocolTypeCode">WWW:LINK-1.0-http--link</gco:CharacterString>
            </cit:protocol>
            <cit:name>
                <gco:CharacterString><xsl:value-of select="$name"/></gco:CharacterString>
            </cit:name>
            <cit:description>
                <gco:CharacterString><xsl:value-of select="$description"/></gco:CharacterString>
            </cit:description>
            <cit:function>
                <cit:CI_OnLineFunctionCode codeList="codeListLocation#CI_OnLineFunctionCode" codeListValue="{$function}"/>
            </cit:function>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>