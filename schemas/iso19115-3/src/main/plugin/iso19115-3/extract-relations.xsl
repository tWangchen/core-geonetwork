<?xml version="1.0" encoding="UTF-8"?>
<!--
  Create a simple XML tree for relation description.
  <relations>
    <relation type="related|services|children">
      + super-brief representation.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
  xmlns:gco="http://standards.iso.org/iso/19115/-3/gco/1.0"
  xmlns:gmx="http://standards.iso.org/iso/19115/-3/gmx"
  xmlns:gn="http://www.fao.org/geonetwork"
  xmlns:gn-fn-iso19115-3="http://geonetwork-opensource.org/xsl/functions/profiles/iso19115-3"
  xmlns:mdb="http://standards.iso.org/iso/19115/-3/mdb/1.0"
  xmlns:lan="http://standards.iso.org/iso/19115/-3/lan/1.0"
  xmlns:mcc="http://standards.iso.org/iso/19115/-3/mcc/1.0"
  xmlns:mrd="http://standards.iso.org/iso/19115/-3/mrd/1.0"
  xmlns:cit="http://standards.iso.org/iso/19115/-3/cit/1.0"
  exclude-result-prefixes="#all" >


    <!-- Convert an element gco:CharacterString
    to the GN localized string structure -->
    <xsl:template mode="get-iso19115-3-localized-string" match="*">
        <xsl:for-each select="gco:CharacterString|
                          lan:PT_FreeText/*/lan:LocalisedCharacterString">
            <xsl:variable name="localeId"
                          select="substring-after(@locale, '#')"/>
            <xsl:variable name="mainLanguage"
                          select="ancestor::lan:MD_Metadata/mdb:defaultLocale/*/lan:language/*/@codeListValue"/>
            <value lang="{if (@locale)
                  then ancestor::lan:MD_Metadata/mdb:otherLocale/*/lan:locale/*[@id = $localeId]/lan:languageCode/*/@codeListValue
                  else if ($mainLanguage) then $mainLanguage else $lang}">
                <xsl:value-of select="."/>
            </value>
        </xsl:for-each>
    </xsl:template>



  <!-- Relation contained in the metadata record has to be returned
  It could be document or thumbnails
  -->
  <xsl:template mode="relation" match="metadata[mdb:MD_Metadata or *[contains(@gco:isoType, 'MD_Metadata')]]" priority="99">

    <thumbnails>
      <xsl:for-each select="*/descendant::*[name(.) = 'mri:graphicOverview']/*">
        <item>
          <id><xsl:value-of select="mcc:fileName/gco:CharacterString"/></id>
          <url>
              <xsl:value-of select="mcc:fileName/gco:CharacterString"/>
          </url>
          <title>
              <xsl:apply-templates mode="get-iso19115-3-localized-string"
                                   select="mcc:fileDescription"/>
          </title>
          <type>thumbnail</type>
        </item>
      </xsl:for-each>
    </thumbnails>

    <onlines>
      <xsl:for-each select="*/descendant::*[
                            local-name() = 'portrayalCatalogueCitation'
                            ]/*[cit:onlineResource/*/cit:linkage/gco:CharacterString != '']">
        <item>
          <id><xsl:value-of select="cit:onlineResource/cit:CI_OnlineResource/cit:linkage/gco:CharacterString"/></id>
          <url>
            <xsl:apply-templates mode="get-iso19115-3-localized-string"
                                 select="cit:onlineResource/cit:CI_OnlineResource/cit:linkage"/>
          </url>
          <title>
            <xsl:apply-templates mode="get-iso19115-3-localized-string"
                                 select="cit:title"/>
          </title>
          <type>legend</type>
        </item>
      </xsl:for-each>

      <xsl:for-each select="*/descendant::*[
                            local-name() = 'additionalDocumentation' or
                            local-name() = 'specification' or
                            local-name() = 'reportReference'
                            ]/*[cit:onlineResource/*/cit:linkage/gco:CharacterString != '']">
        <item>
          <id><xsl:value-of select="cit:onlineResource/cit:CI_OnlineResource/cit:linkage/gco:CharacterString"/></id>
          <url>
            <xsl:apply-templates mode="get-iso19115-3-localized-string"
                                 select="cit:onlineResource/cit:CI_OnlineResource/cit:linkage"/>
          </url>
          <title>
             <xsl:apply-templates mode="get-iso19115-3-localized-string"
                                  select="cit:title"/>
          </title>
          <type>dq-report</type>
         </item>
      </xsl:for-each>

		<!-- Joseph commented - For changes in online distribution -->
      <!--  <xsl:for-each select="*/descendant::*[
                            local-name() = 'onLine'
                            ]/*[cit:linkage/gco:CharacterString != '']">
        <item>
          <id><xsl:value-of select="cit:linkage/gco:CharacterString"/></id>
          <title>
              <xsl:apply-templates mode="get-iso19115-3-localized-string"
                                   select="cit:name"/>
          </title>
          <url>
              <xsl:apply-templates mode="get-iso19115-3-localized-string"
                                   select="cit:linkage"/>
          </url>
          <function><xsl:value-of select="cit:function/*/@codeListValue"/></function>
          <applicationProfile><xsl:value-of select="cit:applicationProfile/gco:CharacterString"/></applicationProfile>
          <description>
              <xsl:apply-templates mode="get-iso19115-3-localized-string"
                                   select="cit:description"/>
          </description>
          <protocol><xsl:value-of select="cit:protocol/gco:CharacterString"/></protocol>
          <type>onlinesrc</type>
        </item>
      </xsl:for-each>-->
		
		<!-- Joseph added - For changes in online distribution -->
		<xsl:for-each select="*//mrd:distributorTransferOptions/*[mrd:onLine/cit:CI_OnlineResource/cit:linkage/gco:CharacterString != '']">
          <item>
              <id><xsl:value-of select="mrd:onLine/cit:CI_OnlineResource/cit:linkage/gco:CharacterString"/></id>
              <title>
                  <xsl:apply-templates mode="get-iso19115-3-localized-string"
                      select="mrd:onLine/cit:CI_OnlineResource/cit:name"/>
              </title>
              <url>
                  <xsl:apply-templates mode="get-iso19115-3-localized-string"
                      select="mrd:onLine/cit:CI_OnlineResource/cit:linkage"/>
              </url>
              <function><xsl:value-of select="cit:function/*/@codeListValue"/></function>
              <applicationProfile><xsl:value-of select="mrd:onLine/cit:CI_OnlineResource/cit:applicationProfile/gco:CharacterString"/></applicationProfile>
              <description>
                  <xsl:apply-templates mode="get-iso19115-3-localized-string"
                      select="mrd:onLine/cit:CI_OnlineResource/cit:description"/>
              </description>
              <protocol>
                <xsl:value-of select="mrd:onLine/cit:CI_OnlineResource/cit:protocol/gco:CharacterString"/>
              </protocol>
              <formatname>
                <xsl:value-of select="mrd:distributionFormat/*/mrd:formatSpecificationCitation/*/cit:title/gco:CharacterString"/>
              </formatname>
              <edition>
                <xsl:value-of select="mrd:distributionFormat/*/mrd:formatSpecificationCitation/*/cit:edition/gco:CharacterString"/>
              </edition>
              <filecomp>
                <xsl:value-of select="mrd:distributionFormat/*/mrd:fileDecompressionTechnique/gco:CharacterString"/>
              </filecomp>
              <type>onlinesrc</type>
          </item>
      </xsl:for-each>
      
      <xsl:for-each select="*/descendant::*[
                            local-name() = 'featureCatalogueCitation'
                            ]/*[cit:onlineResource/*/cit:linkage/gco:CharacterString != '']">
        <item>
          <id><xsl:value-of select="cit:onlineResource/cit:CI_OnlineResource/cit:linkage/gco:CharacterString"/></id>
          <url><xsl:value-of select="cit:onlineResource/cit:CI_OnlineResource/cit:linkage/gco:CharacterString"/></url>
          <title>
            <xsl:apply-templates mode="get-iso19115-3-localized-string"
                               select="cit:title"/>
          </title>
          <type>fcats</type>
        </item>
      </xsl:for-each>
    </onlines>
  </xsl:template>
</xsl:stylesheet>
