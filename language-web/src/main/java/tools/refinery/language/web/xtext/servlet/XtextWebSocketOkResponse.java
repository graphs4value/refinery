package tools.refinery.language.web.xtext.servlet;

import java.util.Objects;

import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.IUnwrappableServiceResult;

import com.google.gson.annotations.SerializedName;

public final class XtextWebSocketOkResponse implements XtextWebSocketResponse {
	private String id;

	private int index;

	@SerializedName("response")
	private Object responseData;

	public XtextWebSocketOkResponse(String id, int index, Object responseData) {
		super();
		this.id = id;
		this.index = index;
		this.responseData = responseData;
	}

	public XtextWebSocketOkResponse(XtextWebSocketRequest request, int index, IServiceResult result) {
		this(request.getId(), index, maybeUnwrap(result));
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

	public Object getResponseData() {
		return responseData;
	}

	public void setResponseData(Object responseData) {
		this.responseData = responseData;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, index, responseData);
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
		return Objects.equals(id, other.id) && index == other.index && Objects.equals(responseData, other.responseData);
	}

	@Override
	public String toString() {
		return "XtextWebSocketOkResponse [id=" + id + ", index=" + index + ", responseData=" + responseData + "]";
	}

	private static Object maybeUnwrap(IServiceResult result) {
		if (result instanceof IUnwrappableServiceResult unwrappableServiceResult
				&& unwrappableServiceResult.getContent() != null) {
			return unwrappableServiceResult.getContent();
		} else {
			return result;
		}
	}
}
