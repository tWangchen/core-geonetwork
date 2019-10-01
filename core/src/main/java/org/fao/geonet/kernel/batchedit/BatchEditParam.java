package org.fao.geonet.kernel.batchedit;

import org.apache.commons.lang.StringUtils;

public class BatchEditParam {

    private String xpath;
    private String value;

    public BatchEditParam() {
    }

    public BatchEditParam(String xpath, String value) {
        if (StringUtils.isEmpty(xpath)) {
            throw new IllegalArgumentException(
                "Parameter xpath is not set. It should be not empty and define the XPath of the element to update.");
        }
        this.xpath = xpath;
        this.value = value;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer("Editing xpath ");
        sb.append(this.xpath);
        if (StringUtils.isNotEmpty(this.value)) {
            sb.append(", searching for ");
            sb.append(this.value);
        }
        sb.append(".");
        return sb.toString();
    }

}
