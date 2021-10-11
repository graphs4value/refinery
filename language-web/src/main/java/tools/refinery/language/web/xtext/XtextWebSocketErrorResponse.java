package tools.refinery.language.web.xtext;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public final class XtextWebSocketErrorResponse implements XtextWebSocketResponse {
	private String id;

	@SerializedName("error")
	private String errorMessage;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public int hashCode() {
		return Objects.hash(errorMessage, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XtextWebSocketErrorResponse other = (XtextWebSocketErrorResponse) obj;
		return Objects.equals(errorMessage, other.errorMessage) && Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "XtextWebSocketError [id=" + id + ", errorMessage=" + errorMessage + "]";
	}
}
