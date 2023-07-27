package tools.refinery.visualization.internal;

public enum FileFormat {
	BMP("bmp"),
	DOT("dot"),
	JPEG("jpg"),
	PDF("pdf"),
	PLAIN("plain"),
	PNG("png"),
	SVG("svg");

	private final String format;

	FileFormat(String format) {
		this.format = format;
	}

	public String getFormat() {
		return format;
	}
}
