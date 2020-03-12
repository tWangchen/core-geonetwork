package org.fao.geonet.constants;

import javax.xml.XMLConstants;

import org.jdom2.Namespace;

public class Namespace2 {

	public static final Namespace[] namespaceList = { Namespaces.MDB, Namespaces.MCC, Namespaces.CIT, Namespaces.CAT,
			Namespaces.GCX, Namespaces.GEX, Namespaces.LAN, Namespaces.MAC, Namespaces.MAS, Namespaces.MCO,
			Namespaces.MDA, Namespaces.MDQ, Namespaces.MDS, Namespaces.MDT, Namespaces.MEX, Namespaces.MMI,
			Namespaces.MPC, Namespaces.MRC, Namespaces.MRD, Namespaces.MRI, Namespaces.MRL, Namespaces.MRS,
			Namespaces.MSR, Namespaces.SRV_3};
	
	public static final class Namespaces {
        public static final Namespace GCO = Namespace.getNamespace("gco", "http://www.isotc211.org/2005/gco");
        public static final Namespace GEONET = Namespace.getNamespace("geonet", "http://www.fao.org/geonetwork");
        public static final Namespace GMX = Namespace.getNamespace("gmx", "http://www.isotc211.org/2005/gmx");
        public static final Namespace GMD = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");
        public static final Namespace OASIS_CATALOG = Namespace.getNamespace("urn:oasis:names:tc:entity:xmlns:xml:catalog");
        public static final Namespace SRV = Namespace.getNamespace("srv", "http://www.isotc211.org/2005/srv");
        public static final Namespace XLINK = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");
        public static final Namespace XSL = Namespace.getNamespace("xsl", "http://www.w3.org/1999/XSL/Transform");
        public static final Namespace XSD = Namespace.getNamespace("xsd", XMLConstants.W3C_XML_SCHEMA_NS_URI);
        public static final Namespace XSI = Namespace.getNamespace("xsi", XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
        public static final Namespace OWS = Namespace.getNamespace("ows", "http://www.opengis.net/ows");
        public static final Namespace OGC = Namespace.getNamespace("ogc", "http://www.opengis.net/ogc");
        public static final Namespace GML = Namespace.getNamespace("gml", "http://www.opengis.net/gml");
        public static final Namespace SVRL = Namespace.getNamespace("svrl", "http://purl.oclc.org/dsdl/svrl");
        public static final Namespace SLD = Namespace.getNamespace("sld", "http://www.opengis.net/sld");
        public static final Namespace SE = Namespace.getNamespace("se", "http://www.opengis.net/se");
        public static final Namespace XML = Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace");
        
    	public static final Namespace MDB = Namespace.getNamespace("mdb", "http://standards.iso.org/iso/19115/-3/mdb/1.0");
    	public static final Namespace MCC = Namespace.getNamespace("mcc", "http://standards.iso.org/iso/19115/-3/mcc/1.0");
    	public static final Namespace CIT = Namespace.getNamespace("cit", "http://standards.iso.org/iso/19115/-3/cit/1.0");
    	public static final Namespace CAT = Namespace.getNamespace("cat", "http://standards.iso.org/iso/19115/-3/cat/1.0");
    	public static final Namespace GCX = Namespace.getNamespace("gcx", "http://standards.iso.org/iso/19115/-3/gcx/1.0");
    	public static final Namespace GEX = Namespace.getNamespace("gex", "http://standards.iso.org/iso/19115/-3/gex/1.0");
    	public static final Namespace LAN = Namespace.getNamespace("lan", "http://standards.iso.org/iso/19115/-3/lan/1.0");

    	public static final Namespace MAC = Namespace.getNamespace("mac", "http://standards.iso.org/iso/19115/-3/mac/1.0");
    	public static final Namespace MAS = Namespace.getNamespace("mas", "http://standards.iso.org/iso/19115/-3/mas/1.0");
    	public static final Namespace MCO = Namespace.getNamespace("mco", "http://standards.iso.org/iso/19115/-3/mco/1.0");
    	public static final Namespace MDA = Namespace.getNamespace("mda", "http://standards.iso.org/iso/19115/-3/mda/1.0");
    	public static final Namespace MDQ = Namespace.getNamespace("mdq", "http://standards.iso.org/iso/19157/-2/mdq/1.0");
    	public static final Namespace MDS = Namespace.getNamespace("mds", "http://standards.iso.org/iso/19115/-3/mds/1.0");
    	public static final Namespace MDT = Namespace.getNamespace("mdt", "http://standards.iso.org/iso/19115/-3/mdt/1.0");
    	public static final Namespace MEX = Namespace.getNamespace("mex", "http://standards.iso.org/iso/19115/-3/mex/1.0");
    	public static final Namespace MMI = Namespace.getNamespace("mmi", "http://standards.iso.org/iso/19115/-3/mmi/1.0");
    	public static final Namespace MPC = Namespace.getNamespace("mpc", "http://standards.iso.org/iso/19115/-3/mpc/1.0");
    	public static final Namespace MRC = Namespace.getNamespace("mrc", "http://standards.iso.org/iso/19115/-3/mrc/1.0");
    	public static final Namespace MRD = Namespace.getNamespace("mrd", "http://standards.iso.org/iso/19115/-3/mrd/1.0");
    	public static final Namespace MRI = Namespace.getNamespace("mri", "http://standards.iso.org/iso/19115/-3/mri/1.0");
    	public static final Namespace MRL = Namespace.getNamespace("mrl", "http://standards.iso.org/iso/19115/-3/mrl/1.0");
    	public static final Namespace MRS = Namespace.getNamespace("mrs", "http://standards.iso.org/iso/19115/-3/mrs/1.0");
    	public static final Namespace MSR = Namespace.getNamespace("msr", "http://standards.iso.org/iso/19115/-3/msr/1.0");
    	public static final Namespace SRV_3 = Namespace.getNamespace("srv", "http://standards.iso.org/iso/19115/-3/srv/2.0");
    	
    }
	
}
