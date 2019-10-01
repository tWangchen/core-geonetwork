<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        version="2.0" xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:srv="http://standards.iso.org/iso/19115/-3/srv/2.0"
        xmlns:gcx="http://standards.iso.org/iso/19115/-3/gcx/1.0" xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
        xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/1.0" xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
        xmlns:mrc="http://standards.iso.org/iso/19115/-3/mrc/1.0" xmlns:lan="http://standards.iso.org/iso/19115/-3/lan/1.0"
        xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0" xmlns:mdq="http://standards.iso.org/iso/19157/-2/mdq/1.0"
        xmlns:mrd="http://standards.iso.org/iso/19115/-3/mrd/1.0"
        xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0" xmlns:xlink="http://www.w3.org/1999/xlink"
        xmlns:gn="http://www.fao.org/geonetwork" exclude-result-prefixes="#all">
        <xsl:template name="sdl_online">
                <xsl:param name="sdlKeyword" />
                <xsl:param name="eCatId" />

        <xsl:variable name="url" select="'file://prod.lan/active/data/sdl/'" />
                <!-- <xsl:variable name="urlNEMO" select="'https://s3-ap-southeast-2.amazonaws.com/nemo-test/'" /> -->

                        <xsl:if test="not($sdlKeyword='National Location Information') and not($sdlKeyword='Exploring for the future') and not($sdlKeyword='NEMO')">

                                <mrd:distributionFormat>
                                        <mrd:MD_Format>
                                                <mrd:formatSpecificationCitation>
                                                        <cit:CI_Citation>
                                                                <cit:title>
                                                                        <gco:CharacterString>winZip</gco:CharacterString>
                                                                </cit:title>
                                                                <cit:date>
                                                                        <cit:CI_Date>
                                                                                <cit:date gco:nilReason="missing" />
                                                                                <cit:dateType gco:nilReason="missing" />
                                                                        </cit:CI_Date>
                                                                </cit:date>
                                                                <cit:edition>
                                                                        <gco:CharacterString>22</gco:CharacterString>
                                                                </cit:edition>
                                                        </cit:CI_Citation>
                                                </mrd:formatSpecificationCitation>
                                                <mrd:formatDistributor>
                                                        <mrd:MD_Distributor>
                                                                <mrd:distributorContact>
                                                                        <cit:CI_Responsibility>
                                                                                <cit:role>
                                                                                        <cit:CI_RoleCode codeList="codeListLocation#CI_RoleCode"
                                                                                                codeListValue="distributor" />
                                                                                </cit:role>
                                                                                <cit:party>
                                                                                        <cit:CI_Organisation>
                                                                                                <cit:name>
                                                                                                        <gco:CharacterString>Geoscience Australia
                                                                                                        </gco:CharacterString>
                                                                                                </cit:name>
                                                                                                <cit:contactInfo>
                                                                                                        <cit:CI_Contact>
                                                                                                                <cit:phone>
                                                                                                                        <cit:CI_Telephone>
                                                                                                                                <cit:number>
                                                                                                                                        <gco:CharacterString>+61 2 6249 9966
                                                                                                                                        </gco:CharacterString>
                                                                                                                                </cit:number>
                                                                                                                                <cit:numberType>
                                                                                                                                        <cit:CI_TelephoneTypeCode
                                                                                                                                                codeList="codeListLocation#CI_TelephoneTypeCode"
                                                                                                                                                codeListValue="voice" />
                                                                                                                                </cit:numberType>
                                                                                                                        </cit:CI_Telephone>
                                                                                                                </cit:phone>
                                                                                                                <cit:phone>
                                                                                                                        <cit:CI_Telephone>
                                                                                                                                <cit:number>
                                                                                                                                        <gco:CharacterString>+61 2 6249 9960
                                                                                                                                        </gco:CharacterString>
                                                                                                                                </cit:number>
                                                                                                                                <cit:numberType>
                                                                                                                                        <cit:CI_TelephoneTypeCode
                                                                                                                                                codeList="codeListLocation#CI_TelephoneTypeCode"
                                                                                                                                                codeListValue="facsimile" />
                                                                                                                                </cit:numberType>
                                                                                                                        </cit:CI_Telephone>
                                                                                                                </cit:phone>
                                                                                                                <cit:address>
                                                                                                                        <cit:CI_Address>
                                                                                                                                <cit:deliveryPoint>
                                                                                                                                        <gco:CharacterString>GPO Box 378</gco:CharacterString>
                                                                                                                                </cit:deliveryPoint>
                                                                                                                                <cit:city>
                                                                                                                                        <gco:CharacterString>Canberra</gco:CharacterString>
                                                                                                                                </cit:city>
                                                                                                                                <cit:administrativeArea>
                                                                                                                                        <gco:CharacterString>ACT</gco:CharacterString>
                                                                                                                                </cit:administrativeArea>
                                                                                                                                <cit:postalCode>
                                                                                                                                        <gco:CharacterString>2601</gco:CharacterString>
                                                                                                                                </cit:postalCode>
                                                                                                                                <cit:country>
                                                                                                                                        <gco:CharacterString>Australia</gco:CharacterString>
                                                                                                                                </cit:country>
                                                                                                                                <cit:electronicMailAddress>
                                                                                                                                        <gco:CharacterString>sales@ga.gov.au
                                                                                                                                        </gco:CharacterString>
                                                                                                                                </cit:electronicMailAddress>
                                                                                                                        </cit:CI_Address>
                                                                                                                </cit:address>
                                                                                                        </cit:CI_Contact>
                                                                                                </cit:contactInfo>
                                                                                        </cit:CI_Organisation>
                                                                                </cit:party>
                                                                        </cit:CI_Responsibility>
                                                                </mrd:distributorContact>
                                                                <mrd:distributorTransferOptions>
                                                                        <mrd:MD_DigitalTransferOptions>
                                                                                <mrd:onLine>
                                                                                        <cit:CI_OnlineResource>
                                                                                                <cit:linkage>
                                                                                                        <gco:CharacterString>


                                                                                                       <!-- <xsl:if test="$sdlKeyword='NEMO'">
                                                                                                                <xsl:variable name="urlFINAL" select="concat($urlNEMO, $sdlKeyword, '/', $eCatId)" /> <xsl:value-of select="$urlFINAL"></xsl:value-of>
                                                                                                        </xsl:if>
                                                                                                                <xsl:if test="not($sdlKeyword='NEMO')">-->
                                                                                                                <xsl:variable name="urlFINAL" select="concat($url, $sdlKeyword, '/', $eCatId)" />
                                                                                                                <xsl:value-of select="$urlFINAL"></xsl:value-of>
                                                                                                         <!-- </xsl:if> -->
                                                                                                        </gco:CharacterString>
                                                                                                </cit:linkage>
                                                                                                <cit:protocol>
                                                                                                        <gco:CharacterString>WWW:LINK-1.0-http--link
                                                                                                        </gco:CharacterString>
                                                                                                </cit:protocol>
                                                                                                <cit:name>
                                                                                                        <gco:CharacterString>Link to Source Dataset Alternate copy</gco:CharacterString>
                                                                                                 </cit:name>
                                                                                                <cit:description>
                                                                                                        <gco:CharacterString>Location of source dataset alternate copy
                                                                                                        </gco:CharacterString>
                                                                                                </cit:description>
                                                                                        </cit:CI_OnlineResource>
                                                                                </mrd:onLine>
                                                                        </mrd:MD_DigitalTransferOptions>
                                                                </mrd:distributorTransferOptions>
                                                        </mrd:MD_Distributor>
                                                </mrd:formatDistributor>
                                        </mrd:MD_Format>
                                </mrd:distributionFormat>
                                </xsl:if>
        </xsl:template>
</xsl:stylesheet>


