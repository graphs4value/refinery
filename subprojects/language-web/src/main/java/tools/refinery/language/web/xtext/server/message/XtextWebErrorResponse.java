/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server.message;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public final class XtextWebErrorResponse implements XtextWebResponse {
	private String id;

	@SerializedName("error")
	private XtextWebErrorKind errorKind;

	@SerializedName("message")
	private String errorMessage;

	public XtextWebErrorResponse(String id, XtextWebErrorKind errorKind, String errorMessage) {
		super();
		this.id = id;
		this.errorKind = errorKind;
		this.errorMessage = errorMessage;
	}

	public XtextWebErrorResponse(XtextWebRequest request, XtextWebErrorKind errorKind,
			String errorMessage) {
		this(request.getId(), errorKind, errorMessage);
	}

	public XtextWebErrorResponse(XtextWebRequest request, XtextWebErrorKind errorKind, Throwable t) {
		this(request, errorKind, t.getMessage());
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public XtextWebErrorKind getErrorKind() {
		return errorKind;
	}

	public void setErrorKind(XtextWebErrorKind errorKind) {
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
		return Objects.hash(errorKind, errorMessage, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XtextWebErrorResponse other = (XtextWebErrorResponse) obj;
		return errorKind == other.errorKind && Objects.equals(errorMessage, other.errorMessage)
				&& Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "XtextWebSocketErrorResponse [id=" + id + ", errorKind=" + errorKind + ", errorMessage=" + errorMessage
				+ "]";
	}
}
