package tools.refinery.language.web.xtext.server;

import tools.refinery.language.web.xtext.server.message.XtextWebResponse;

@FunctionalInterface
public interface ResponseHandler {
	void onResponse(XtextWebResponse response) throws ResponseHandlerException;
}
