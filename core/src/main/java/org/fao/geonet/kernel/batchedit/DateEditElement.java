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
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.exceptions.BatchEditException;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.springframework.context.ApplicationContext;

import jeeves.server.context.ServiceContext;

/**
 * This class creates Citation Date element 
 * @author Joseph John - U89263
 *
 */
public class DateEditElement implements EditElement {

	XMLOutputter out = new XMLOutputter();

	@Override
	public void removeAndAddElement(CSVBatchEdit batchEdit, ApplicationContext context, ServiceContext serContext,
			Entry<String, Integer> header, CSVRecord csvr, XPath _xpath, List<BatchEditParam> listOfUpdates,
			BatchEditReport report) {

		String headerVal = header.getKey();

		String[] contents = csvr.get(headerVal).split(content_separator);

		for (String content : contents) {
			String[] values = content.split(type_separator);

			Element rootE = null;
			if (headerVal.equalsIgnoreCase(Geonet.EditType.CITATION_DATE))
				try {
					rootE = getCitationDateElement(batchEdit, values);
				} catch (BatchEditException e) {
					List<String> errs = report.getErrorInfo();
					errs.add(e.getMessage());
					report.setErrorInfo(errs);
				}

			String strEle = out.outputString(rootE);

			String _val = "<gn_add>" + strEle + "</gn_add>";

			BatchEditParam e = new BatchEditParam(_xpath.getXPath(), _val);
			listOfUpdates.add(e);

		}

	}

	/**
	 * Create citation date element. Values should be in the format datetime~codelist
	 * @param batchEdit
	 * @param values
	 * @return
	 * @throws BatchEditException
	 */
	private Element getCitationDateElement(CSVBatchEdit batchEdit, String[] values) throws BatchEditException {

		//Element date = new Element("date", Geonet.Namespaces.CIT);
		Element ciDate = new Element("CI_Date", Geonet.Namespaces.CIT);
		Element date1 = new Element("date", Geonet.Namespaces.CIT);
		Element dateType = new Element("dateType", Geonet.Namespaces.CIT);

		try {
			if (values.length > 0) {
				String dateStr = "Date";
				String dateVal = values[0];
				if (dateVal.contains("T")) {
					dateStr = "DateTime";
				}
				date1.addContent(new Element(dateStr, Geonet.Namespaces.GCO_3).setText(values[0]));
			}

			if (values.length > 1) {
				String clval = values[1];
				Element typeAttr = new Element("CI_DateTypeCode", Geonet.Namespaces.CIT);
				typeAttr.setAttribute("codeList", "codeListLocation#CI_DateTypeCode");
				if (clval.contains(" ")) {
					clval = batchEdit.toTitleCase(clval);
				}
				typeAttr.setAttribute("codeListValue", clval);
				dateType.addContent(typeAttr);
			}

			//date.addContent(ciDate.addContent(Arrays.asList(date1, dateType)));
			ciDate.addContent(Arrays.asList(date1, dateType));
			return ciDate;
		} catch (Exception e) {
			throw new BatchEditException("Unable to process Citation Date Element");
		}
	}

}
