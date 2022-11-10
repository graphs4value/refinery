package tools.refinery.language.web.xtext.server;

import java.io.Serial;

public class ResponseHandlerException extends Exception {

	@Serial
	private static final long serialVersionUID = 3589866922420268164L;

	public ResponseHandlerException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResponseHandlerException(String message) {
		super(message);
	}
}
