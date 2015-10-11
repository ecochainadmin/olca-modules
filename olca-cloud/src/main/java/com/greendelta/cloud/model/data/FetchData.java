package com.greendelta.cloud.model.data;

public class FetchData extends DatasetIdentifier {

	private static final long serialVersionUID = 417426973222267018L;
	private String json;

	public FetchData() {

	}

	public FetchData(DatasetIdentifier identifier) {
		setRefId(identifier.getRefId());
		setType(identifier.getType());
		setVersion(identifier.getVersion());
		setLastChange(identifier.getLastChange());
		setName(identifier.getName());
		setCategoryRefId(identifier.getCategoryRefId());
		setCategoryType(identifier.getCategoryType());
	}

	public boolean isDeleted() {
		return json == null || json.isEmpty();
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

}
