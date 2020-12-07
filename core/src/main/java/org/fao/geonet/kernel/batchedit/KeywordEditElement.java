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

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVRecord;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.exceptions.BatchEditException;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.Thesaurus;
import org.fao.geonet.kernel.ThesaurusManager;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.springframework.context.ApplicationContext;

import jeeves.server.context.ServiceContext;

/**
 * This class creates simple keywords by providing a text and codelist values and also create keyword based on thesaurus.
 *  
 * @author Joseph John - U89263
 *
 */
public class KeywordEditElement implements EditElement {

	XMLOutputter out = new XMLOutputter();

	SchemaManager schemaManager;
	
	@Override
	public void removeAndAddElement(CSVBatchEdit batchEdit, ApplicationContext context, ServiceContext serContext,
			Entry<String, Integer> header, CSVRecord csvr, XPath _xpath, List<BatchEditParam> listOfUpdates,
			BatchEditReport report) {

		schemaManager = context.getBean(SchemaManager.class);
		String headerVal = header.getKey();

		//If more keyword exist, it must be separated by ###
		String[] keywords = csvr.get(headerVal).split(content_separator);

		for (String keyword : keywords) {

			Element rootE = null;

			try {
				
				if (headerVal.equalsIgnoreCase(Geonet.EditType.KEYWORD_THESAURUS))
					rootE = getKeywordElementWithThesaurus(keyword, context, serContext);

			} catch (BatchEditException e) {
				List<String> errs = report.getErrorInfo();
				errs.add(e.getMessage());
				report.setErrorInfo(errs);
			}

			if (rootE != null) {
				String strEle = out.outputString(rootE);

				String _val = "<gn_add>" + strEle + "</gn_add>";
				BatchEditParam e = new BatchEditParam(_xpath.getXPath(), _val);
				listOfUpdates.add(e);
			}
		}

	}


	/**
	 * Creates keyword element based on thesaurus. keyword should be provided in the format title~keyword1,keyword2,..,keywordn
	 * Keywords must match exactly that defined in given title. 
	 * @param title_keyword
	 * @param context
	 * @param serContext
	 * @return
	 * @throws BatchEditException
	 */
	private Element getKeywordElementWithThesaurus(String title_keyword, ApplicationContext context,
			ServiceContext serContext) throws BatchEditException {

		try {
			String[] values = title_keyword.split(type_separator);
			ThesaurusManager thesaurusMan = context.getBean(ThesaurusManager.class);
			Thesaurus thes = null;

			if (values.length > 0) {

				Log.debug(Geonet.GA, "CSVBatchEdit, KeywordEditElement --> title: " + values[0]);
				
				Collection<Thesaurus> thesColl = thesaurusMan.getThesauriMap().values();

				//Reduce the Collection of thesaurus  
				thes = thesColl.stream().filter(t -> t.getTitle().equalsIgnoreCase(values[0].trim())).findFirst().get();

				if (thes != null && values.length > 1) {

					Element mdK = new Element("MD_Keywords", Geonet.Namespaces.MRI);

					String _date = thes.getDate();
					if (!thes.getDate().contains("T")) {
						_date = _date + "T00:00:00";
					} 

					Path p = schemaManager.getSchemaDir(Geonet.SCHEMA_ISO_19115_3).resolve("csv").resolve("keyword-thesaurus.xsl");
					
					Map<String, Object> params = new HashMap<>();
					params.put("title", thes.getTitle());
					params.put("date", _date);
					params.put("link", thes.getDownloadUrl());
					params.put("key", "geonetwork.thesaurus." + thes.getKey());
					params.put("keywords", values[1]);
					
					mdK = Xml.transform(mdK, p, params);
					
					return mdK;
				} else {
					Log.debug(Geonet.GA, "CSVBatchEdit, KeywordEditElement --> ThesaurusByName is null");
				}

			}
		} catch (Exception e) {
			throw new BatchEditException("Unable to process Keyword Element with Thesaurus having keyword "
					+ title_keyword + " - " + e.getMessage());
		}
		return null;

	}


}
