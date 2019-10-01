package org.fao.geonet.kernel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.fao.geonet.constants.Geonet.Namespaces2;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;


public class TransformManager {

	XMLOutputter outputter = new XMLOutputter();
	
	Namespace[] namespaces = {Namespaces2.MRI, Namespaces2.GCO};
	
	XMLOutputter out = new XMLOutputter();
	org.jdom.output.XMLOutputter out1 = new org.jdom.output.XMLOutputter();
	
	public org.jdom.Element updatePublishKeyWord(org.jdom.Element e, String path, String[] values, String replacePattern, String keyword, boolean isClear) throws Exception{
		Element e2 = containsXPath(convertToJdom2Element(e), path, values, replacePattern, keyword, isClear);
		if(e2 != null){
			return convertToJdomElement(e2);
		} 
		return null;
	}
	
	
	private Element containsXPath(Element e, String path, String[] values, String replacePattern, String keyword, boolean isClear){
		XPathExpression<Element> xpathExp = null;
		Element key_ele = null;
		if(values != null && values.length > 0){
			for (String value : values) {
				String _path = path.replace(replacePattern, value);
				xpathExp = XPathFactory.instance().compile(_path, Filters.element(), null, namespaces);
				key_ele = xpathExp.evaluateFirst(e);
				
				if(key_ele != null){
					if(isClear){
						key_ele.getParentElement().getParentElement().detach();
					}else{
						Element charstr = new Element("CharacterString", "gco", "http://standards.iso.org/iso/19115/-3/gco/1.0");
						key_ele.setContent(charstr.setText(keyword));
					}
					return e;
				}
			}	
		}
		
		return null;
	}
	
	public org.jdom.Element convertToJdomElement(Element e2) throws Exception{
		ByteArrayOutputStream os = new ByteArrayOutputStream(); 
		out.output(e2, os);
		
		org.jdom.input.SAXBuilder builder = new org.jdom.input.SAXBuilder();
		org.jdom.Element e = builder.build(new ByteArrayInputStream(os.toByteArray())).getRootElement();
		
		return e;
	}
	
	public Element convertToJdom2Element(org.jdom.Element e) throws Exception{
		ByteArrayOutputStream os = new ByteArrayOutputStream(); 
		out1.output(e, os);
		
		SAXBuilder builder = new SAXBuilder();
		Element e2 = builder.build(new ByteArrayInputStream(os.toByteArray())).getRootElement();

		return e2;
	}
	
}
