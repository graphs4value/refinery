package tools.refinery.language.web.xtext;

import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class XtextWebSocketRequest {
	private String id;

	@SerializedName("request")
	private Map<String, String> requestData;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, String> getRequestData() {
		return requestData;
	}

	public void setRequestData(Map<String, String> request) {
		this.requestData = request;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, requestData);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XtextWebSocketRequest other = (XtextWebSocketRequest) obj;
		return Objects.equals(id, other.id) && Objects.equals(requestData, other.requestData);
	}

	@Override
	public String toString() {
		return "XtextWebSocketRequest [id=" + id + ", requestData=" + requestData + "]";
	}
}
