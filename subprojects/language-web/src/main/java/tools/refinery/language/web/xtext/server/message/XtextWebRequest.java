/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server.message;

import com.google.gson.annotations.SerializedName;

import java.util.Map;
import java.util.Objects;

public class XtextWebRequest {
	private String id;

	@SerializedName("request")
	private Map<String, String> requestData;

	public XtextWebRequest() {
		this(null, null);
	}

	public XtextWebRequest(String id, Map<String, String> requestData) {
		this.id = id;
		this.requestData = requestData;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Map<String, String> getRequestData() {
		return requestData;
	}

	public void setRequestData(Map<String, String> requestData) {
		this.requestData = requestData;
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
		XtextWebRequest other = (XtextWebRequest) obj;
		return Objects.equals(id, other.id) && Objects.equals(requestData, other.requestData);
	}

	@Override
	public String toString() {
		return "XtextWebSocketRequest [id=" + id + ", requestData=" + requestData + "]";
	}
}
