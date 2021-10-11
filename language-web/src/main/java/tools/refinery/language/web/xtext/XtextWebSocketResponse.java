package tools.refinery.language.web.xtext;

public sealed interface XtextWebSocketResponse permits XtextWebSocketOkResponse, XtextWebSocketErrorResponse {
	public String getId();

	public void setId(String id);
}
