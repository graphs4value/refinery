package tools.refinery.language.web.xtext.server;

public class ResponseHandlerException extends Exception {

	private static final long serialVersionUID = 3589866922420268164L;

	public ResponseHandlerException(String message, Throwable cause) {
		super(message, cause);
	}

	public ResponseHandlerException(String message) {
		super(message);
	}
}
