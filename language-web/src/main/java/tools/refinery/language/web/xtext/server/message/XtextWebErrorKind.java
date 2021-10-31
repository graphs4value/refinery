package tools.refinery.language.web.xtext.server.message;

import com.google.gson.annotations.SerializedName;

public enum XtextWebErrorKind {
	@SerializedName("request")
	REQUEST_ERROR,

	@SerializedName("server")
	SERVER_ERROR,
}
