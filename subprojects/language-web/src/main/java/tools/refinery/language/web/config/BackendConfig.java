package tools.refinery.language.web.config;

import com.google.gson.annotations.SerializedName;

public class BackendConfig {
	@SerializedName("webSocketURL")
	private String webSocketUrl;

	public BackendConfig(String webSocketUrl) {
		this.webSocketUrl = webSocketUrl;
	}

	public String getWebSocketUrl() {
		return webSocketUrl;
	}

	public void setWebSocketUrl(String webSocketUrl) {
		this.webSocketUrl = webSocketUrl;
	}
}
