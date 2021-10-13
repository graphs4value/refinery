package tools.refinery.language.web.xtext;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class XtextWebSocketRequest {
	private String id;

	@SerializedName("resource")
	private String resourceName;

	private String contentType;

	private String requiredStateId;

	@SerializedName("request")
	private List<Map<String, String>> requestData;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getResourceName() {
		return resourceName;
	}

	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getRequiredStateId() {
		return requiredStateId;
	}

	public void setRequiredStateId(String requiredStateId) {
		this.requiredStateId = requiredStateId;
	}

	public List<Map<String, String>> getRequestData() {
		return requestData;
	}

	public void setRequestData(List<Map<String, String>> requestData) {
		this.requestData = requestData;
	}

	@Override
	public int hashCode() {
		return Objects.hash(contentType, id, requestData, requiredStateId, resourceName);
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
		return Objects.equals(contentType, other.contentType) && Objects.equals(id, other.id)
				&& Objects.equals(requestData, other.requestData)
				&& Objects.equals(requiredStateId, other.requiredStateId)
				&& Objects.equals(resourceName, other.resourceName);
	}

	@Override
	public String toString() {
		return "XtextWebSocketRequest [id=" + id + ", resourceName=" + resourceName + ", contentType=" + contentType
				+ ", requiredStateId=" + requiredStateId + ", requestData=" + requestData + "]";
	}
}
