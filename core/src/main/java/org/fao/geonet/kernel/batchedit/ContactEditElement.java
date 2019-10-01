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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVRecord;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.exceptions.BatchEditException;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.repository.specification.MetadataSpecs;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.xpath.XPath;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;

import jeeves.server.context.ServiceContext;

/**
 * 
 * @author Joseph John - U89263
 *
 *	This class generates contact element for ISO19115-3 standard 
 */
public class ContactEditElement implements EditElement {
	
	@Override
	public void removeAndAddElement(CSVBatchEdit batchEdit, ApplicationContext context, ServiceContext srvContext,
			Entry<String, Integer> header, CSVRecord csvr, XPath _xpath, List<BatchEditParam> listOfUpdates, BatchEditReport report) {

		String headerVal = header.getKey();
		String[] contacts = csvr.get(headerVal).split(content_separator);

		ElementFilter filter = new ElementFilter("CI_RoleCode", Geonet.Namespaces.CIT);

		for (String con : contacts) {

			String[] contact = con.split(type_separator);

			try{
				if (contact.length > 0) {
					String value = contact[0];
	
					
					Element xmlEle = getContactElement(batchEdit, context, srvContext, value);
	
					String type = "";
	
					if (contact.length >= 2) {
						type = contact[1];
					}
	
					if(xmlEle != null){
						Iterator elements = xmlEle.getDescendants(filter);
		
						while (elements.hasNext()) {
							Element e = (Element) elements.next();
							e.setAttribute("codeListValue", type);
						}
		
						String _val = "<gn_add>" + Xml.getString(xmlEle) + "</gn_add>";
						BatchEditParam e = new BatchEditParam(_xpath.getXPath(), _val);
						listOfUpdates.add(e);
					}
				}
			}catch(BatchEditException e){
				List<String> errs = report.getErrorInfo();
				errs.add(e.getMessage());
				report.setErrorInfo(errs);
			}
		}

	}

	/**
	 * This method get contact from lucene index
	 * 
	 * @param batchEdit
	 * @param context
	 * @param srvContext
	 * @param contact
	 * @return
	 * @throws IOException
	 * @throws JDOMException
	 */
	public Element getContactElement(CSVBatchEdit batchEdit, ApplicationContext context, ServiceContext srvContext, String contact)
			throws BatchEditException {

		try{
			Element request = Xml.loadString(
					"<request><isAdmin>true</isAdmin><_isTemplate>s</_isTemplate><_root>cit:CI_Responsibility</_root><any>*"
							+ contact + "*</any><fast>index</fast></request>",
					false);
			
			Metadata md = batchEdit.getMetadataByLuceneSearch(context, srvContext, request);
			
			return md.getXmlData(false);
		} catch (Exception e) {
			throw new BatchEditException("Contact: Unable to get contact " + contact + " from lucene search");
		}
	}

	/**
	 * This method get contact from database
	 * 
	 * @param context
	 * @param srvContext
	 * @param contact
	 * @return
	 * @throws IOException
	 * @throws JDOMException
	 */
	public Element getContactElementfromDB(ApplicationContext context, ServiceContext srvContext, String contact)
			throws IOException, JDOMException {
		Specification<Metadata> title = (Specification<Metadata>) MetadataSpecs.hasTitle(contact);
		MetadataRepository mdRepo = context.getBean(MetadataRepository.class);
		// Metadata md = mdRepo.findOneByTitle(contact);
		List<Metadata> mds = mdRepo.findAll(Specifications.where(title));

		if (mds != null && mds.size() > 0) {

			Metadata md = mds.get(0);

			// String contactData = md.getData();
			Element xmlEle = md.getXmlData(false);

			return xmlEle;
		}

		return null;
	}
}
