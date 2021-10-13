package tools.refinery.language.web.xtext;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public final class XtextWebSocketErrorResponse implements XtextWebSocketResponse {
	private String id;

	private int index;

	@SerializedName("error")
	private XtextWebSocketErrorKind errorKind;

	@SerializedName("message")
	private String errorMessage;

	public XtextWebSocketErrorResponse(String id, int index, XtextWebSocketErrorKind errorKind, String errorMessage) {
		super();
		this.id = id;
		this.index = index;
		this.errorKind = errorKind;
		this.errorMessage = errorMessage;
	}

	public XtextWebSocketErrorResponse(XtextWebSocketRequest request, int index, XtextWebSocketErrorKind errorKind,
			String errorMessage) {
		this(request.getId(), index, errorKind, errorMessage);
	}

	public XtextWebSocketErrorResponse(XtextWebSocketRequest request, int index, XtextWebSocketErrorKind errorKind) {
		this(request, index, errorKind, (String) null);
	}

	public XtextWebSocketErrorResponse(XtextWebSocketRequest request, int index, XtextWebSocketErrorKind errorKind,
			Throwable t) {
		this(request, index, errorKind, t.getMessage());
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public XtextWebSocketErrorKind getErrorKind() {
		return errorKind;
	}

	public void setErrorKind(XtextWebSocketErrorKind errorKind) {
		this.errorKind = errorKind;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public int hashCode() {
		return Objects.hash(errorKind, errorMessage, id, index);
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
		return errorKind == other.errorKind && Objects.equals(errorMessage, other.errorMessage)
				&& Objects.equals(id, other.id) && index == other.index;
	}

	@Override
	public String toString() {
		return "XtextWebSocketErrorResponse [id=" + id + ", index=" + index + ", errorKind=" + errorKind
				+ ", errorMessage=" + errorMessage + "]";
	}
}
