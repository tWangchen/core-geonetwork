package org.fao.geonet.domain;

public class MetadataIndexedField {

	private String eCatId;
	private String uuid;
	private String keywords;
	private String pid;
	private String authors;

	public String geteCatId() {
		return eCatId;
	}

	public void seteCatId(String eCatId) {
		this.eCatId = eCatId;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getPid() {
		return pid;
	}

	public void setPid(String pid) {
		this.pid = pid;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public String getAuthors() {
		return authors;
	}

	public void setAuthors(String authors) {
		this.authors = authors;
	}

	@Override
	public String toString() {
	
		StringBuilder sb = new StringBuilder();
		sb.append("eCatId: " + this.eCatId + ", ");
		sb.append("uuid: " + this.uuid + ", ");
		sb.append("pid: " + this.pid + ", ");
		sb.append("keywords: " + this.keywords + ", ");
		sb.append("author: " + this.authors + ", ");
		return sb.toString();
	}
}
