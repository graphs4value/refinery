package tools.refinery.language.web.xtext;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public final class XtextWebSocketOkResponse implements XtextWebSocketResponse {
	private String id;

	@SerializedName("response")
	private Object responseData;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public Object getResponseData() {
		return responseData;
	}

	public void setResponseData(Object responseData) {
		this.responseData = responseData;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, responseData);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XtextWebSocketOkResponse other = (XtextWebSocketOkResponse) obj;
		return Objects.equals(id, other.id) && Objects.equals(responseData, other.responseData);
	}

	@Override
	public String toString() {
		return "XtextWebSocketResponse [id=" + id + ", responseData=" + responseData + "]";
	}
}
