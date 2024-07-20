/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests;

import org.eclipse.emf.common.util.URI;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.generator.tests.internal.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

class SemanticsTestBuilder implements ChunkAcceptor {
	private final Pattern LINE_PATTERN = Pattern.compile("^.+$", Pattern.MULTILINE);

	private final ProblemLoader problemLoader;
	private final URI uri;
	private final StringBuilder commonBuilder = new StringBuilder();
	private final List<SemanticsTestCase> testCases = new ArrayList<>();
	private SemanticsTestCaseBuilder testCaseBuilder;
	private boolean singleTestMode;

	public SemanticsTestBuilder(ProblemLoader problemLoader, URI uri) {
		this.problemLoader = problemLoader;
		this.uri = uri;
	}

	@Override
	public void acceptChunk(ChunkHeader header, String body) {
		switch (header) {
		case CommonHeader ignored -> {
			if (testCaseBuilder != null) {
				throw new IllegalStateException("Can't accept common test code after the first test case.");
			}
			commonBuilder.append(body);
		}
		case TestCaseHeader testCaseHeader -> {
			if (singleTestMode) {
				throw new IllegalStateException("Can't accept TEST chunk after an EXPECT chunk.");
			}
			acceptTestCase(testCaseHeader, body);
			appendEmptyLines(body);
		}
		case ExpectationHeader expectationHeader -> {
			if (testCaseBuilder == null) {
				acceptTestCase(new TestCaseHeader(false, null), null);
				singleTestMode = true;
			}
			testCaseBuilder.acceptExpectation(expectationHeader, body);
			appendEmptyLines(body);
		}
		default -> throw new IllegalArgumentException("Unknown ChunkHeader: " + header);
		}
	}

	private void appendEmptyLines(String body) {
		if (singleTestMode) {
			return;
		}
		var placeholder = LINE_PATTERN.matcher(body).replaceAll("");
		commonBuilder.append(placeholder);
	}

	private void acceptTestCase(TestCaseHeader header, String body) {
		if (testCaseBuilder != null) {
			testCases.add(testCaseBuilder.build());
		}
		var problemStringBuilder = new StringBuilder(commonBuilder);
		if (body != null) {
			problemStringBuilder.append(body);
		}
		testCaseBuilder = new SemanticsTestCaseBuilder(header, problemStringBuilder, problemLoader, uri);
	}

	@Override
	public void acceptEnd() {
		if (testCaseBuilder == null) {
			throw new IllegalStateException("Test file contained no TEST or EXPECT chunks.");
		}
		testCases.add(testCaseBuilder.build());
	}

	public SemanticsTest build() {
		if (testCases.isEmpty()) {
			throw new IllegalStateException("No test cases.");
		}
		return new SemanticsTest(Collections.unmodifiableList(testCases));
	}
}
