/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server.message;

import com.google.gson.annotations.SerializedName;
import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.IUnwrappableServiceResult;

import java.util.Objects;

public final class XtextWebOkResponse implements XtextWebResponse {
	private String id;

	@SerializedName("response")
	private Object responseData;

	public XtextWebOkResponse(String id, Object responseData) {
		this.id = id;
		this.responseData = responseData;
	}

	public XtextWebOkResponse(XtextWebRequest request, IServiceResult result) {
		this(request.getId(), maybeUnwrap(result));
	}

	public String getId() {
		return id;
	}

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
		XtextWebOkResponse other = (XtextWebOkResponse) obj;
		return Objects.equals(id, other.id) && Objects.equals(responseData, other.responseData);
	}

	@Override
	public String toString() {
		return "XtextWebSocketOkResponse [id=" + id + ", responseData=" + responseData + "]";
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
