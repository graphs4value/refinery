/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server.message;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public final class XtextWebPushMessage implements XtextWebResponse {	
	@SerializedName("resource")
	private String resourceId;
	
	private String stateId;
	
	private String service;
	
	@SerializedName("push")
	private Object pushData;

	public XtextWebPushMessage(String resourceId, String stateId, String service, Object pushData) {
		super();
		this.resourceId = resourceId;
		this.stateId = stateId;
		this.service = service;
		this.pushData = pushData;
	}

	public String getResourceId() {
		return resourceId;
	}

	public void setResourceId(String resourceId) {
		this.resourceId = resourceId;
	}

	public String getStateId() {
		return stateId;
	}

	public void setStateId(String stateId) {
		this.stateId = stateId;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public Object getPushData() {
		return pushData;
	}

	public void setPushData(Object pushData) {
		this.pushData = pushData;
	}

	@Override
	public int hashCode() {
		return Objects.hash(pushData, resourceId, service, stateId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XtextWebPushMessage other = (XtextWebPushMessage) obj;
		return Objects.equals(pushData, other.pushData) && Objects.equals(resourceId, other.resourceId)
				&& Objects.equals(service, other.service) && Objects.equals(stateId, other.stateId);
	}

	@Override
	public String toString() {
		return "XtextWebPushMessage [resourceId=" + resourceId + ", stateId=" + stateId + ", service=" + service
				+ ", pushData=" + pushData + "]";
	}
}
