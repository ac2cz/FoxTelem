package gui.graph;

public class PlotVariable implements Comparable<PlotVariable>{
	String fieldName;
	String moduleName;
	String shortName;
	
	public PlotVariable(String fieldName, String moduleName, String shortName) {
		this.fieldName = fieldName;
		this.moduleName = moduleName;
		this.shortName = shortName;
	}
	
	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	/**
	 * Return alphabetical order based on Module Name and Short Name
	 */
	@Override
	public int compareTo(PlotVariable pv2) {
		return this.toString().compareTo(pv2.toString());
	}
	
	public String toString() {
		return moduleName + "-" + shortName;
	}
	
}
