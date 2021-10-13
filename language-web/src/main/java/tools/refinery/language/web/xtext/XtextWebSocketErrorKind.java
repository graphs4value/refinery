package tools.refinery.language.web.xtext;

import com.google.gson.annotations.SerializedName;

public enum XtextWebSocketErrorKind {
	@SerializedName("request")
	REQUEST_ERROR,

	@SerializedName("server")
	SERVER_ERROR,

	@SerializedName("transaction")
	TRANSACTION_CANCELLED,
}
