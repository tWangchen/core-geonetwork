//==============================================================================
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Joseph John,
//===	Canberra - Australia. email: joseph.john@ga.gov.au
//==============================================================================
package org.fao.geonet.kernel.batchedit;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.constants.Geonet.Namespaces;
import org.fao.geonet.exceptions.BatchEditException;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.springframework.context.ApplicationContext;

import jeeves.server.context.ServiceContext;

/**
 * This class creates online resource element for Data storage, Associated
 * resources, Additional Information and Distribution link.
 * 
 * @author Joseph John - U89263
 *
 */
public class OnlineResourceEditElement implements EditElement {

	XMLOutputter out = new XMLOutputter();

	@Override
	public void removeAndAddElement(CSVBatchEdit batchEdit, ApplicationContext context, ServiceContext serContext,
			Entry<String, Integer> header, CSVRecord csvr, XPath _xpath, List<BatchEditParam> listOfUpdates, BatchEditReport report) {

		String headerVal = header.getKey();

		String[] contents = csvr.get(headerVal).split(content_separator);
		for (String content : contents) {
			String[] values = content.split(type_separator);

			String name = "", desc = "", linkage = "", protocol = "WWW:LINK-1.0-http--link", function = "";

			if (values.length > 0)
				name = values[0];
			if (values.length > 1)
				desc = values[1];
			if (values.length > 2)
				linkage = values[2];
			if (values.length > 3)
				protocol = values[3];
			if (values.length > 4)
				function = values[4];

			Element rootE = null;

			try {
				if (Geonet.EditType.ASSOCIATED_RES.equalsIgnoreCase(headerVal)) {
					rootE = getOnlineResourceElement(name, desc, linkage, protocol, function);
				} else if (Geonet.EditType.DISTRIBUTION_LINK.equalsIgnoreCase(headerVal)){
					rootE = getDistributionOnlineResourceElement(name, desc, linkage, protocol, function);
				}else if (Geonet.EditType.DATA_STORAGE_LINK.equalsIgnoreCase(headerVal)){
					rootE = getResourceFormatElement(content);
				}else if (Geonet.EditType.ADDITIONAL_INFO.equalsIgnoreCase(headerVal)) {
					rootE = additionalInformation(name, desc, linkage, protocol, function);
				}
			} catch (BatchEditException e) {
				List<String> errs = report.getErrorInfo();
				errs.add(e.getMessage());
				report.setErrorInfo(errs);
			}

			if(rootE != null){
				String strEle = out.outputString(rootE);
	
				//Log.debug(Geonet.SEARCH_ENGINE, "OnlineResource EditElement --> strEle : " + strEle);
	
				String _val = "<gn_add>" + strEle + "</gn_add>";
	
				BatchEditParam e = new BatchEditParam(_xpath.getXPath(), _val);
				listOfUpdates.add(e);
			}
		}

	}

	/**
	 * Creates Online resource element 
	 * @param _name
	 * @param description
	 * @param link
	 * @return
	 * @throws BatchEditException
	 */
	private Element getDistributionOnlineResourceElement(String _name, String description, String link, String protocol, String function) throws BatchEditException {
		try{
			
			Element digitalTransferOptions = new Element("MD_DigitalTransferOptions", Geonet.Namespaces.MRD);
			Element online = new Element("onLine", Geonet.Namespaces.MRD);
			
			Element onlineRes = onlineResElement(_name, description, link, protocol, function);
			return digitalTransferOptions.addContent(online.addContent(onlineRes));
			
			
		}catch(BatchEditException e){
			throw new BatchEditException("Unable to process Online Resource Element having name " + _name + " and link " + link);
		}
	}
	
	
	/**
	 * Creates Online resource for resource format - Data Storage 
	 * @param _name
	 * @param description
	 * @param link
	 * @return
	 * @throws BatchEditException
	 */
	private Element getResourceFormatElement(String link) throws BatchEditException {
		try{
			String _title = "Product data repository: Various Formats";
			String _name = "Data Store directory containing the digital product files";
			String description = "Data Store directory containing one or more files, possibly in a variety of formats, accessible to Geoscience Australia staff only for internal purposes";
			String protocol = "FILE:DATA-DIRECTORY";
			String function = "offlineAccess";
			Element mdFormat = new Element("MD_Format", Namespaces.MRD);
			Element formatSpec = new Element("formatSpecificationCitation", Namespaces.MRD);
			Element citation = new Element("CI_Citation", Namespaces.CIT);
			Element title = new Element("title", Namespaces.CIT);
			Element charStr = new Element("CharacterString", Namespaces.GCO_3);
			
			title.addContent(charStr.setText(_title));
			Element onlineRes = new Element("onlineResource", Geonet.Namespaces.CIT);
			onlineRes.addContent(onlineResElement(_name, description, link, protocol, function));
			return mdFormat.addContent(formatSpec.addContent(citation.addContent(Arrays.asList(title, onlineRes))));
			
		}catch(BatchEditException e){
			throw new BatchEditException("Unable to process resource format having name link " + link);
		}
	}
	
	/**
	 * Creates Online resource element 
	 * @param _name
	 * @param description
	 * @param link
	 * @return
	 * @throws BatchEditException
	 */
	private Element getOnlineResourceElement(String _name, String description, String link, String protocol, String function) throws BatchEditException {
		try{
			
			return onlineResElement(_name, description, link, protocol, function);
			
		}catch(BatchEditException e){
			throw new BatchEditException("Unable to process Online Resource Element having name " + _name + " and link " + link);
		}
	}

	/**
	 * Creates additional information link
	 * @param _name
	 * @param description
	 * @param link
	 * @return
	 * @throws BatchEditException
	 */
	private Element additionalInformation(String _name, String description, String link, String protocol, String function) throws BatchEditException {
		try{
			Element citation = new Element("CI_Citation", Geonet.Namespaces.CIT);
			Element onlineres = new Element("onlineResource", Geonet.Namespaces.CIT);
			Element title = new Element("title", Geonet.Namespaces.CIT);
	
			citation.addContent(title.addContent(new Element("CharacterString", Geonet.Namespaces.GCO_3).setText(_name)));
			citation.addContent(onlineres.addContent(onlineResElement(_name, description, link, protocol, function)));
		
			return citation;
			
		}catch(BatchEditException e){
			throw new BatchEditException("Unable to process additional Information having name " + _name + " and link " + link);
		}
	}

	/**
	 * Create online resource element
	 * @param _name
	 * @param description
	 * @param link
	 * @return
	 * @throws BatchEditException
	 */
	private Element onlineResElement(String _name, String description, String link, String _protocol, String _function) throws BatchEditException {

		try {
			Element onlineRes = new Element("CI_OnlineResource", Geonet.Namespaces.CIT);

			Element linkage = new Element("linkage", Geonet.Namespaces.CIT);
			linkage.addContent(new Element("CharacterString", Geonet.Namespaces.GCO_3).setText(link));

			Element protocol = new Element("protocol", Geonet.Namespaces.CIT);
			Element charString = new Element("CharacterString", Geonet.Namespaces.GCO_3);
			charString.addNamespaceDeclaration(Namespaces.XSI);
			charString.setAttribute("type", "gco:CodeType", Namespaces.XSI);
			charString.setAttribute("codeSpace", "http://pid.geoscience.gov.au/def/schema/ga/ISO19115-3-2016/codelist/ga_profile_codelists.xml#gapCI_ProtocolTypeCode");
			protocol.addContent(charString.setText(_protocol));

			Element name = new Element("name", Geonet.Namespaces.CIT);
			name.addContent(new Element("CharacterString", Geonet.Namespaces.GCO_3).setText(_name));

			Element desc = new Element("description", Geonet.Namespaces.CIT);
			desc.addContent(new Element("CharacterString", Geonet.Namespaces.GCO_3).setText(description));

			Element function = new Element("function", Geonet.Namespaces.CIT);
			Element cl = new Element("CI_OnLineFunctionCode", Geonet.Namespaces.CIT);
			cl.setAttribute("codeList", "codeListLocation#CI_OnLineFunctionCode");
			if(StringUtils.isEmpty(_function)){
				_function = "information";
			}
			cl.setAttribute("codeListValue", _function);
			function.addContent(cl);

			onlineRes.addContent(Arrays.asList(linkage, protocol, name, desc, function));

			return onlineRes;
		} catch (Exception e) {
			throw new BatchEditException("Unable to process onlineRes Element having name " + _name + " and link " + link);
		}
	}
}
