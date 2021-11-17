package tools.refinery.language.mapping;

public class ModelToStoreException extends Exception {
	private static final long serialVersionUID = 1L;

	public ModelToStoreException(String errorText) {
		super(errorText);
	}

	public ModelToStoreException() {
		super();
	}
}
