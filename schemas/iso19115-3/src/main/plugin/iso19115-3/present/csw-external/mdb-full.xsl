<?xml version="1.0" encoding="UTF-8"?>

<!--
	Purpose:
	This stylesheet removes and alters content in an ISO19115-3 xml document in accordance with 
	Geoscience Australia policy for restricting public visibility of content that is for internal 
	user only.
	
	The stylesheet is intended for use with the GeoNetwork software delivering metadata records
	via CSW.
-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/1.0"
    xmlns:geonet="http://www.fao.org/geonetwork"
    xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
    xmlns:mri="http://standards.iso.org/iso/19115/-3/mri/1.0"
    xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
    xmlns:mrd="http://standards.iso.org/iso/19115/-3/mrd/1.0"
    xmlns:srv="http://standards.iso.org/iso/19115/-3/srv/2.0"
    xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    exclude-result-prefixes="#all">
    
    <xsl:output method="xml" indent="yes"/>
    <xsl:strip-space elements="*"/>
    
    <xsl:variable name="agencyEmailAddress" select="'clientservices@ga.gov.au'"/>
    <xsl:variable name="eCatInternalHostname" select="'intranet.ga.gov.au'"/>
    <xsl:variable name="eCatExternalHostname" select="'ecat.ga.gov.au'"/>
    <xsl:variable name="thumbnailWAF" select="'http://img.ecat.ga.gov.au/thumbnails/'"/>
    
    <!-- standard copy template -->
    <!--
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*"/>
            <xsl:apply-templates/>
        </xsl:copy>
    </xsl:template>
    -->
    
    <xsl:param name="displayInfo"/>
    
    <xsl:template match="@*|node()[name(.)!='geonet:info']">
        <xsl:variable name="info" select="geonet:info"/>
        <xsl:copy>
            <xsl:apply-templates select="@*|node()[name(.)!='geonet:info']"/>
            <!-- GeoNetwork elements added when resultType is equal to results_with_summary -->
            <xsl:if test="$displayInfo = 'true'">
                <xsl:copy-of select="$info"/>
            </xsl:if>
        </xsl:copy>
    </xsl:template>
    
    <!-- Remove citedResponsibleParty elements where Organisation name is CORP, CSEMD, RD, EGD or ICTIS -->
    <xsl:template
        match="cit:citedResponsibleParty[lower-case(./cit:CI_Responsibility/cit:party/cit:CI_Organisation/cit:name/gco:CharacterString)='corp' or
        lower-case(./cit:CI_Responsibility/cit:party/cit:CI_Organisation/cit:name/gco:CharacterString)='csemd' or
        lower-case(./cit:CI_Responsibility/cit:party/cit:CI_Organisation/cit:name/gco:CharacterString)='rd' or
        lower-case(./cit:CI_Responsibility/cit:party/cit:CI_Organisation/cit:name/gco:CharacterString)='egd' or
        lower-case(./cit:CI_Responsibility/cit:party/cit:CI_Organisation/cit:name/gco:CharacterString)='ed' or
        lower-case(./cit:CI_Responsibility/cit:party/cit:CI_Organisation/cit:name/gco:CharacterString)='exec' or
        lower-case(./cit:CI_Responsibility/cit:party/cit:CI_Organisation/cit:name/gco:CharacterString)='mnhd' or
        lower-case(./cit:CI_Responsibility/cit:party/cit:CI_Organisation/cit:name/gco:CharacterString)='ictis']"/>
    
    
    <!-- Remove any existing contacts -->
    <xsl:template match="mdb:MD_Metadata/mdb:contact"/>
    <xsl:template match="mdb:MD_Metadata/mdb:identificationInfo/mri:MD_DataIdentification/mri:pointOfContact"/> 
    <xsl:template match="mdb:MD_Metadata/mdb:identificationInfo/srv:SV_ServiceIdentification/mri:pointOfContact"/>

    <!-- Include the official metadata contact element after metadataScope 
         Note that if there's multiple metadataScope elements this won't work -->
    <xsl:template match="mdb:MD_Metadata/mdb:metadataScope">
        <xsl:copy>
            <xsl:copy-of select="@*" />
            <xsl:apply-templates select="@*|node()[name(.)!='geonet:info']"/>
        </xsl:copy>
        <mdb:contact>
            <cit:CI_Responsibility>
                <cit:role>
                    <cit:CI_RoleCode codeList="codeListLocation#CI_RoleCode" codeListValue="custodian">custodian</cit:CI_RoleCode>
                </cit:role>
                <cit:party>
                    <cit:CI_Organisation>
                        <cit:name>
                            <gco:CharacterString>Commonwealth of Australia (Geoscience Australia)</gco:CharacterString>
                        </cit:name>
                        <cit:contactInfo>
                            <cit:CI_Contact>
                                <cit:phone>
                                    <cit:CI_Telephone>
                                        <cit:number>
                                            <gco:CharacterString>02 6249 9966</gco:CharacterString>
                                        </cit:number>
                                        <cit:numberType>
                                            <cit:CI_TelephoneTypeCode codeList="codeListLocation#CI_TelephoneTypeCode" codeListValue="voice">voice</cit:CI_TelephoneTypeCode>
                                        </cit:numberType>
                                    </cit:CI_Telephone>
                                </cit:phone>
                                <cit:phone>
                                    <cit:CI_Telephone>
                                        <cit:number>
                                            <gco:CharacterString>02 6249 9960</gco:CharacterString>
                                        </cit:number>
                                        <cit:numberType>
                                            <cit:CI_TelephoneTypeCode codeList="codeListLocation#CI_TelephoneTypeCode" codeListValue="facsimile">facsimile</cit:CI_TelephoneTypeCode>
                                        </cit:numberType>
                                    </cit:CI_Telephone>
                                </cit:phone>
                                <cit:address>
                                    <cit:CI_Address>
                                        <cit:deliveryPoint>
                                            <gco:CharacterString>Cnr Jerrabomberra Ave and Hindmarsh Dr GPO Box 378</gco:CharacterString>
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
                                            <gco:CharacterString>clientservices@ga.gov.au</gco:CharacterString>
                                        </cit:electronicMailAddress>
                                    </cit:CI_Address>
                                </cit:address>
                            </cit:CI_Contact>
                        </cit:contactInfo>
                        <cit:individual>
                            <cit:CI_Individual>
                                <cit:positionName>
                                    <gco:CharacterString>Manager Client Services</gco:CharacterString>
                                </cit:positionName>
                            </cit:CI_Individual>
                        </cit:individual>
                    </cit:CI_Organisation>
                </cit:party>
            </cit:CI_Responsibility>
        </mdb:contact>        
    </xsl:template>
    
    <!-- Include the official resource pointOfContact elements after abstract -->
    
    <!-- dataset metadata -->
    <xsl:template match="mdb:MD_Metadata/mdb:identificationInfo/mri:MD_DataIdentification/mri:abstract">
        <xsl:copy>
            <xsl:copy-of select="@*" />
            <xsl:apply-templates select="@*|node()[name(.)!='geonet:info']"/>
        </xsl:copy>
        <mri:pointOfContact>
            <cit:CI_Responsibility>
                <cit:role>
                    <cit:CI_RoleCode codeList="codeListLocation#CI_RoleCode" codeListValue="owner">owner</cit:CI_RoleCode>
                </cit:role>
                <cit:party>
                    <cit:CI_Organisation>
                        <cit:name>
                            <gco:CharacterString>Commonwealth of Australia (Geoscience Australia)</gco:CharacterString>
                        </cit:name>
                    </cit:CI_Organisation>
                </cit:party>
            </cit:CI_Responsibility>
        </mri:pointOfContact>
        <mri:pointOfContact>
            <cit:CI_Responsibility>
                <cit:role>
                    <cit:CI_RoleCode codeList="codeListLocation#CI_RoleCode" codeListValue="custodian">custodian</cit:CI_RoleCode>
                </cit:role>
                <cit:party>
                    <cit:CI_Organisation>
                        <cit:name>
                            <gco:CharacterString>Commonwealth of Australia (Geoscience Australia)</gco:CharacterString>
                        </cit:name>
                        <cit:contactInfo>
                            <cit:CI_Contact>
                                <cit:phone>
                                    <cit:CI_Telephone>
                                        <cit:number>
                                            <gco:CharacterString>02 6249 9966</gco:CharacterString>
                                        </cit:number>
                                        <cit:numberType>
                                            <cit:CI_TelephoneTypeCode codeList="codeListLocation#CI_TelephoneTypeCode" codeListValue="voice">voice</cit:CI_TelephoneTypeCode>
                                        </cit:numberType>
                                    </cit:CI_Telephone>
                                </cit:phone>
                                <cit:phone>
                                    <cit:CI_Telephone>
                                        <cit:number>
                                            <gco:CharacterString>02 6249 9960</gco:CharacterString>
                                        </cit:number>
                                        <cit:numberType>
                                            <cit:CI_TelephoneTypeCode codeList="codeListLocation#CI_TelephoneTypeCode" codeListValue="facsimile">facsimile</cit:CI_TelephoneTypeCode>
                                        </cit:numberType>
                                    </cit:CI_Telephone>
                                </cit:phone>
                                <cit:address>
                                    <cit:CI_Address>
                                        <cit:deliveryPoint>
                                            <gco:CharacterString>Cnr Jerrabomberra Ave and Hindmarsh Dr GPO Box 378</gco:CharacterString>
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
                                            <gco:CharacterString>clientservices@ga.gov.au</gco:CharacterString>
                                        </cit:electronicMailAddress>
                                    </cit:CI_Address>
                                </cit:address>
                            </cit:CI_Contact>
                        </cit:contactInfo>
                        <cit:individual>
                            <cit:CI_Individual>
                                <cit:positionName>
                                    <gco:CharacterString>Manager Client Services</gco:CharacterString>
                                </cit:positionName>
                            </cit:CI_Individual>
                        </cit:individual>
                    </cit:CI_Organisation>
                </cit:party>
            </cit:CI_Responsibility>
        </mri:pointOfContact>
    </xsl:template>
    
    <!-- service metadata -->
    <xsl:template match="mdb:MD_Metadata/mdb:identificationInfo/srv:SV_ServiceIdentification/mri:abstract">
        <xsl:copy>
            <xsl:copy-of select="@*" />
            <xsl:apply-templates select="@*|node()[name(.)!='geonet:info']"/>
        </xsl:copy>
        <mri:pointOfContact>
            <cit:CI_Responsibility>
                <cit:role>
                    <cit:CI_RoleCode codeList="codeListLocation#CI_RoleCode" codeListValue="owner">owner</cit:CI_RoleCode>
                </cit:role>
                <cit:party>
                    <cit:CI_Organisation>
                        <cit:name>
                            <gco:CharacterString>Commonwealth of Australia (Geoscience Australia)</gco:CharacterString>
                        </cit:name>
                    </cit:CI_Organisation>
                </cit:party>
            </cit:CI_Responsibility>
        </mri:pointOfContact>
        <mri:pointOfContact>
            <cit:CI_Responsibility>
                <cit:role>
                    <cit:CI_RoleCode codeList="codeListLocation#CI_RoleCode" codeListValue="custodian">custodian</cit:CI_RoleCode>
                </cit:role>
                <cit:party>
                    <cit:CI_Organisation>
                        <cit:name>
                            <gco:CharacterString>Commonwealth of Australia (Geoscience Australia)</gco:CharacterString>
                        </cit:name>
                        <cit:contactInfo>
                            <cit:CI_Contact>
                                <cit:phone>
                                    <cit:CI_Telephone>
                                        <cit:number>
                                            <gco:CharacterString>02 6249 9966</gco:CharacterString>
                                        </cit:number>
                                        <cit:numberType>
                                            <cit:CI_TelephoneTypeCode codeList="codeListLocation#CI_TelephoneTypeCode" codeListValue="voice">voice</cit:CI_TelephoneTypeCode>
                                        </cit:numberType>
                                    </cit:CI_Telephone>
                                </cit:phone>
                                <cit:phone>
                                    <cit:CI_Telephone>
                                        <cit:number>
                                            <gco:CharacterString>02 6249 9960</gco:CharacterString>
                                        </cit:number>
                                        <cit:numberType>
                                            <cit:CI_TelephoneTypeCode codeList="codeListLocation#CI_TelephoneTypeCode" codeListValue="facsimile">facsimile</cit:CI_TelephoneTypeCode>
                                        </cit:numberType>
                                    </cit:CI_Telephone>
                                </cit:phone>
                                <cit:address>
                                    <cit:CI_Address>
                                        <cit:deliveryPoint>
                                            <gco:CharacterString>Cnr Jerrabomberra Ave and Hindmarsh Dr GPO Box 378</gco:CharacterString>
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
                                            <gco:CharacterString>clientservices@ga.gov.au</gco:CharacterString>
                                        </cit:electronicMailAddress>
                                    </cit:CI_Address>
                                </cit:address>
                            </cit:CI_Contact>
                        </cit:contactInfo>
                        <cit:individual>
                            <cit:CI_Individual>
                                <cit:positionName>
                                    <gco:CharacterString>Manager Client Services</gco:CharacterString>
                                </cit:positionName>
                            </cit:CI_Individual>
                        </cit:individual>
                    </cit:CI_Organisation>
                </cit:party>
            </cit:CI_Responsibility>
        </mri:pointOfContact>
    </xsl:template>

    <!-- Only include onLine distribution elements containing http or https links -->
    
    <!-- distribution information structure 1 -->
    <xsl:template match="mdb:MD_Metadata/mdb:distributionInfo/mrd:MD_Distribution/mrd:transferOptions/mrd:MD_DigitalTransferOptions/mrd:onLine">
        <xsl:if test="./cit:CI_OnlineResource/cit:linkage/gco:CharacterString[matches(., '^(http|file:///g/data)')]">
            <xsl:copy>
                <xsl:copy-of select="@*" />
                <xsl:apply-templates select="@*|node()[name(.)!='geonet:info']"/>
            </xsl:copy>
        </xsl:if>
    </xsl:template>
    <!-- distribution information structure 2 -->
    <xsl:template match="mdb:MD_Metadata/mdb:distributionInfo/mrd:MD_Distribution/mrd:distributionFormat">
        <xsl:choose>
            <!-- if mrd:distributionFormat contains transfer options.... -->
            <xsl:when test="./mrd:MD_Format/mrd:formatDistributor/mrd:MD_Distributor/mrd:distributorTransferOptions">
                <!-- Only keep the mrd:distributionFormat element if the transfer options are of types http(s) or file:///g/data -->
                <xsl:if test="./mrd:MD_Format/mrd:formatDistributor/mrd:MD_Distributor/mrd:distributorTransferOptions/mrd:MD_DigitalTransferOptions/mrd:onLine/cit:CI_OnlineResource/cit:linkage/gco:CharacterString[matches(., '^(http|file:///g/data)')]">
                    <xsl:copy>
                        <xsl:copy-of select="@*" />
                        <xsl:apply-templates select="@*|node()[name(.)!='geonet:info']"/>
                    </xsl:copy>
                </xsl:if>
            </xsl:when>
            <!-- Otherwise keep the mrd:distributionFormat element -->
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:copy-of select="@*" />
                    <xsl:apply-templates select="@*|node()[name(.)!='geonet:info']"/>
                </xsl:copy>                
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
 
    <!-- Ensure electronicEmailAddress for the metadata point of contact is set to the appropriate 
        generic GA address -->
    <xsl:template match="mdb:MD_Metadata/mdb:contact/cit:CI_Responsibility/cit:party/cit:CI_Organisation/cit:contactInfo/cit:CI_Contact/cit:address/cit:CI_Address/cit:electronicMailAddress/gco:CharacterString">
        <xsl:element name="gco:CharacterString">
            <xsl:value-of select="$agencyEmailAddress"/>
        </xsl:element>  
    </xsl:template>
    
    <!-- Ensure electronicEmailAddress for the metadata point of contact is set to the appropriate 
        generic GA address -->
    <xsl:template match="mdb:MD_Metadata/mdb:identificationInfo/mri:MD_DataIdentification/mri:pointOfContact/cit:CI_Responsibility/cit:party/cit:CI_Organisation/cit:contactInfo/cit:CI_Contact/cit:address/cit:CI_Address/cit:electronicMailAddress/gco:CharacterString">
        <xsl:element name="gco:CharacterString">
            <xsl:value-of select="$agencyEmailAddress"/>
        </xsl:element>  
    </xsl:template>
    
    <!-- Change attribute references to internal eCat hostname in srv:operatesOn element to external eCat hostname -->
    <xsl:template match="@xlink:href[parent::srv:operatesOn]">
        <xsl:attribute name="xlink:href">
            <xsl:value-of select="replace(., $eCatInternalHostname, $eCatExternalHostname)"/>
        </xsl:attribute>
    </xsl:template>
    
    <!-- Change internal eCat hostname in mdb:metadataLinkage element to external eCat hostname -->
    <xsl:template match="mdb:MD_Metadata/mdb:metadataLinkage[4]/cit:CI_OnlineResource[1]/cit:linkage[1]/gco:CharacterString">
        <xsl:element name="gco:CharacterString">
            <xsl:value-of select="replace(., $eCatInternalHostname, $eCatExternalHostname)"/>
        </xsl:element>
    </xsl:template>
    
    <!-- Change URLs for the graphic overview files (thumbnails) from GeoNetwork file delivery service
         to independent, publicly visible web accessible folder -->
    <xsl:template match="mri:graphicOverview/mcc:MD_BrowseGraphic/mcc:fileName/gco:CharacterString">
        <xsl:choose>
            <xsl:when test=".[contains(., '/resources.get?')]">
                <xsl:variable name="uuid"
                    select="substring-before(substring-after(., 'uuid='), '&amp;')"/>
                <xsl:variable name="fname"
                    select="substring-after(., 'fname=')"/>
                <xsl:variable name="thumbnail_url"
                    select="concat($thumbnailWAF, $uuid, '_', $fname)"/>
                <xsl:element name="gco:CharacterString">
                    <xsl:value-of select="$thumbnail_url"/>
                </xsl:element>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy>
                    <xsl:copy-of select="@*|node()" />
                </xsl:copy>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>