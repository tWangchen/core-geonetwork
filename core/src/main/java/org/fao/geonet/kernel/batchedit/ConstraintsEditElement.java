package org.fao.geonet.kernel.batchedit;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVRecord;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.exceptions.BatchEditException;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.springframework.context.ApplicationContext;

import jeeves.server.context.ServiceContext;

public class ConstraintsEditElement implements EditElement {

	@Override
	public void removeAndAddElement(CSVBatchEdit batchEdit, ApplicationContext context, ServiceContext srvContext, Entry<String, Integer> header, CSVRecord csvr,
			XPath _xpath, List<BatchEditParam> listOfUpdates, BatchEditReport report) {
		
		String headerVal = header.getKey();
		String[] legalConstraints = csvr.get(headerVal).split(content_separator);

		for (String constraint : legalConstraints) {

			try{
				
				Element xmlEle = getLegalConstraintElement(batchEdit, context, srvContext, constraint);

				if(xmlEle != null){
					
	
					String _val = "<gn_add>" + Xml.getString(xmlEle) + "</gn_add>";
					BatchEditParam e = new BatchEditParam(_xpath.getXPath(), _val);
					listOfUpdates.add(e);
				}
			
			}catch(BatchEditException e){
				List<String> errs = report.getErrorInfo();
				errs.add(e.getMessage());
				report.setErrorInfo(errs);
			}

			
		}
	}
	
	/**
	 * This method get legal contraint from lucene index
	 * 
	 * @param batchEdit
	 * @param context
	 * @param srvContext
	 * @param contact
	 * @return
	 * @throws IOException
	 * @throws JDOMException
	 */
	public Element getLegalConstraintElement(CSVBatchEdit batchEdit, ApplicationContext context, ServiceContext srvContext, String legalConstraint)
			throws BatchEditException {

		try{
			Element request = Xml.loadString(
					"<request><isAdmin>true</isAdmin><_isTemplate>s</_isTemplate><_root>mco:MD_LegalConstraints</_root><any>*"
							+ legalConstraint + "*</any><fast>index</fast></request>",
					false);
			
			Metadata md = batchEdit.getMetadataByLuceneSearch(context, srvContext, request);
			
			return md.getXmlData(false);
		} catch (Exception e) {
			throw new BatchEditException("Contact: Unable to get Legal Constraint " + legalConstraint + " from lucene search");
		}
	}

}
