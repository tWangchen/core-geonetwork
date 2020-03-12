package org.fao.geonet.kernel.batchedit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BatchEditReport implements Serializable {

	private List<String> errorInfo;
	private List<String> processInfo;

	public List<String> getErrorInfo() {
		if(this.errorInfo == null){
			return new ArrayList<String>(); 
		}
		return new ArrayList<String>(this.errorInfo);
	}

	public void setErrorInfo(List<String> errorInfo) {
		this.errorInfo = new ArrayList<String>(errorInfo);
	}

	public List<String> getProcessInfo() {
		if(this.processInfo == null){
			return new ArrayList<String>(); 
		}
		return new ArrayList<String>(this.processInfo);
	}

	public void setProcessInfo(List<String> processInfo) {
		this.processInfo = new ArrayList<String>(processInfo);
	}

}
