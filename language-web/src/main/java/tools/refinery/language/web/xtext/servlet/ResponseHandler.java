package tools.refinery.language.web.xtext.servlet;

import java.io.IOException;

@FunctionalInterface
public interface ResponseHandler {
	void onResponse(XtextWebSocketResponse response) throws IOException;
}
