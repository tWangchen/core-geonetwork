<?xml version="1.0" encoding="UTF-8"?>
<!--
  The main entry point for all user interface generated
  from XSLT.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="2.0"
                exclude-result-prefixes="#all">

  <xsl:template name="header">
    <div class="navbar navbar-default gn-top-bar" role="navigation">
      <div class="container">
        <div class="navbar-header">
          <button type="button"
                  class="navbar-toggle collapsed"
                  data-toggle="collapse"
                  data-target="#navbar"
                  title="{$i18n/toggleNavigation}"
                  aria-expanded="false"
                  aria-controls="navbar">
            <span class="sr-only">
              <xsl:value-of select="$i18n/toggleNavigation"/>
            </span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
            <span class="icon-bar"></span>
          </button>
        </div>
        <div id="navbar" class="navbar-collapse collapse">
          
          <div class="col-md-6">
              <!-- <a href="http://www.ga.gov.au/"  title="Navigate to the Geoscience Australia home page">
              	<img src="../../../catalog/views/default/images/ga.header.logo.png" />
                <img src="https://ecat.ga.gov.au/geonetwork/catalog/views/default/images/ga.header.logo.png" />
              </a> -->
          </div>
          <div class="title-lg col-md-6">
            <span>Data and Publications Search</span>
            <a title="{$t/search}" href="{/root/gui/nodeUrl}search" onclick="location.href=('{/root/gui/nodeUrl}{$lang}/catalog.search#/search');return false;">
              <i class="fa fa-fw fa-search hidden-sm">&#160;</i>
              <!-- <span><xsl:value-of select="$t/search"/></span> -->
            </a>
          </div> 
               
        </div>
      </div>
    </div>

    <xsl:if test="/root/search/response">
      <form action="{$nodeUrl}search"
            class="form-horizontal" role="form">
        <div class="row gn-top-search" style="margin:20px">
          <div class="col-md-offset-3 col-md-1 relative"><b><xsl:value-of select="$t/search"/></b></div>
          <div class="col-md-5 relative">
            <div class="gn-form-any input-group input-group-lg">
              <input type="text"
                      name="any"
                      id="gn-any-field"
                      aria-label="{$t/anyPlaceHolder}"
                      placeholder="{$t/anyPlaceHolder}"
                      value="{/root/request/any}"
                      class="form-control"
                      autofocus=""/>
              <div class="input-group-btn">
                <button type="submit"
                        class="btn btn-default"
                        title="{$t/search}">
                  <i class="fa fa-search">&#160;</i>
                </button>
                <a href="{$nodeUrl}search"
                    class="btn btn-default"
                    title="{$t/reset}">
                  <i class="fa fa-times">&#160;</i>
                </a>
              </div>
            </div>
            <input type="hidden" name="fast" value="index"/>
          </div>
        </div>
      </form>
    </xsl:if>
  </xsl:template>

  <xsl:template name="footer">

    <footer id="footer-main" class="footer-global">

      <div class="container">
        <div class="block-group">
    
          <nav id="footer-bottom-nav" class="block col-width-auto">
            <ul class="nav nav-pills">
              <!--  <li>&copy; Commonwealth of Australia</li> -->
              <li><a href="http://www.ga.gov.au/copyright">Copyright</a></li>
              <li><a href="http://www.ga.gov.au/privacy">Privacy</a></li>
              <li><a href="http://www.ga.gov.au/accessibility">Accessibility</a></li>
              <li><a href="http://www.ga.gov.au/sitemap">Sitemap</a></li>
              <li><a href="http://www.ga.gov.au/ips">Information Publication Scheme</a></li>
              <li><a href="http://www.ga.gov.au/ips/foi">Freedom of Information</a></li>
            </ul>
          </nav>
    
          <div class="block col-lg-1-12" style="padding-top:15px;">Geoscience Australia acknowledges the
            traditional owners of the country throughout Australia and their continuing connection to land, sea
            and community. We pay our respect to them and their cultures and to the elders past and present.
          </div>
    
        </div>
      </div>
    </footer>
  </xsl:template>


</xsl:stylesheet>
