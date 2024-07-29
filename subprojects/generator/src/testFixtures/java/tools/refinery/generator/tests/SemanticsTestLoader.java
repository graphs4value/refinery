/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests;

import com.google.inject.Inject;
import org.eclipse.emf.common.util.URI;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.generator.tests.internal.ProblemSplitter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class SemanticsTestLoader {
	@Inject
	private ProblemSplitter problemSplitter;

	@Inject
	private ProblemLoader problemLoader;

	public SemanticsTestLoader extraPath(String path) {
		problemLoader.extraPath(Path.of(path));
		return this;
	}

	public SemanticsTestLoader extraPath(Path path) {
		problemLoader.extraPath(path);
		return this;
	}

	public SemanticsTest loadString(String problemString, URI uri) {
		var builder = new SemanticsTestBuilder(problemLoader, uri);
		problemSplitter.transformProblem(problemString, builder);
		return builder.build();
	}

	public SemanticsTest loadString(String problemString) {
		return loadString(problemString, null);
	}

	public SemanticsTest loadStream(InputStream inputStream, URI uri) throws IOException {
		byte[] bytes;
		try (var outputStream = new ByteArrayOutputStream()) {
			inputStream.transferTo(outputStream);
			bytes = outputStream.toByteArray();
		}
		var problemString = new String(bytes, StandardCharsets.UTF_8);
		return loadString(problemString, uri);
	}

	public SemanticsTest loadStream(InputStream inputStream) throws IOException {
		return loadStream(inputStream, null);
	}

	public SemanticsTest loadFile(File file) throws IOException {
		var uri = URI.createFileURI(file.getAbsolutePath());
		try (var inputStream = new FileInputStream(file)) {
			return loadStream(inputStream, uri);
		}
	}

	public SemanticsTest loadFile(String filePath) throws IOException {
		var uri = URI.createFileURI(filePath);
		try (var inputStream = new FileInputStream(filePath)) {
			return loadStream(inputStream, uri);
		}
	}
}
