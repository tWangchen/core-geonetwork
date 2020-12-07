<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    exclude-result-prefixes="xs"
    xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0"
    xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
    xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
    xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
    xmlns:gcx="http://standards.iso.org/iso/19115/-3/gcx/1.0"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    version="2.0">
    
    <xsl:param name="title" select="''"/>
    <xsl:param name="date" select="current-dateTime()"/>
    <xsl:param name="link" select="''"/>
    <xsl:param name="key" select="''"/>
    
    <xsl:param name="keywords" />
    
    <xsl:template match="mri:MD_Keywords">
        <xsl:copy>
            <xsl:call-template name="splitStringToItems">
                <xsl:with-param name="delimiter" />
                <xsl:with-param name="list" select="$keywords" />
            </xsl:call-template>
            <mri:type>
                <mri:MD_KeywordTypeCode codeList="codeListLocation#MD_KeywordTypeCode" codeListValue="theme"/>
            </mri:type>
            <mri:thesaurusName>
                <cit:CI_Citation>
                    <cit:title>
                        <gco:CharacterString><xsl:value-of select="$title"/></gco:CharacterString>
                    </cit:title>
                    <cit:date>
                        <cit:CI_Date>
                            <cit:date>
                                <gco:DateTime><xsl:value-of select="$date"/></gco:DateTime>
                            </cit:date>
                            <cit:dateType>
                                <cit:CI_DateTypeCode codeList="codeListLocation#CI_DateTypeCode" codeListValue="publication"/>
                            </cit:dateType>
                        </cit:CI_Date>
                    </cit:date>
                    <cit:identifier>
                        <mcc:MD_Identifier>
                            <mcc:code>
                                <gcx:Anchor xlink:href="{$link}"><xsl:value-of select="$key"/></gcx:Anchor>
                            </mcc:code>
                        </mcc:MD_Identifier>
                    </cit:identifier>
                </cit:CI_Citation>
            </mri:thesaurusName>
        </xsl:copy>
        
       
    </xsl:template>
    
    <xsl:template name="splitStringToItems">
        
        <xsl:param name="list" />
        <xsl:param name="delimiter" select="','"  />
        <xsl:variable name="_delimiter">
            <xsl:choose>
                <xsl:when test="string-length($delimiter)=0">,</xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$delimiter"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="newlist">
            <xsl:choose>
                <xsl:when test="contains($list, $_delimiter)">
                    <xsl:value-of select="normalize-space($list)" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat(normalize-space($list), $_delimiter)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="first" select="substring-before($newlist, $_delimiter)" />
        <xsl:variable name="remaining" select="substring-after($newlist, $_delimiter)" />
       
        <mri:keyword>
            <gco:CharacterString><xsl:value-of select="$first" /></gco:CharacterString>
        </mri:keyword>
        <xsl:if test="$remaining">
            <xsl:call-template name="splitStringToItems">
                <xsl:with-param name="list" select="$remaining" />
                <xsl:with-param name="delimiter" select="$_delimiter" />
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>