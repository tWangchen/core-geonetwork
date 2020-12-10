<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
    xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
    exclude-result-prefixes="xs"
    version="2.0">
    
    <xsl:template match="/cit:CI_Responsibility">
        <xsl:copy>
            <cit:role>
                <cit:CI_RoleCode codeList="codeListLocation#CI_RoleCode" codeListValue=""/>
            </cit:role>
            <cit:party>
                <cit:CI_Organisation>
                    <xsl:copy-of select="cit:party/cit:CI_Organisation/cit:name" />
                    <cit:contactInfo>
                        <cit:CI_Contact>
                        	<xsl:copy-of select="cit:party/cit:CI_Organisation/cit:contactInfo/cit:CI_Contact/cit:phone" />
                            <xsl:copy-of select="cit:party/cit:CI_Organisation/cit:contactInfo/cit:CI_Contact/cit:address" />
                            <cit:contactType>
                                <gco:CharacterString>External Contact</gco:CharacterString>
                            </cit:contactType>
                        </cit:CI_Contact>
                    </cit:contactInfo>
                    <xsl:copy-of select="cit:party/cit:CI_Organisation/cit:individual" />
                </cit:CI_Organisation>
            </cit:party>
        </xsl:copy>
    </xsl:template>
    
</xsl:stylesheet>