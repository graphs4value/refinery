/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.generator.tests.internal.ExpectationHeader;
import tools.refinery.generator.tests.internal.TestCaseHeader;
import tools.refinery.language.model.problem.Assertion;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.model.problem.Statement;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;

class SemanticsTestCaseBuilder {
	private final TestCaseHeader testCaseHeader;
	private final StringBuilder stringBuilder;
	private final ProblemLoader problemLoader;
	private final URI uri;
	private final Deque<ExpectationHeader> expectationsDeque = new ArrayDeque<>();

	public SemanticsTestCaseBuilder(TestCaseHeader testCaseHeader, StringBuilder stringBuilder,
									ProblemLoader problemLoader, URI uri) {
		this.testCaseHeader = testCaseHeader;
		this.stringBuilder = stringBuilder;
		this.problemLoader = problemLoader;
		this.uri = uri;
	}

	public void acceptExpectation(ExpectationHeader header, String body) {
		stringBuilder.append(body);
		expectationsDeque.addLast(header);
	}

	public SemanticsTestCase build() {
		Problem problem;
		try {
			problem = problemLoader.loadString(stringBuilder.toString(), uri);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to parse problem: " + uri, e);
		}
		if (expectationsDeque.isEmpty() && testCaseHeader.allowErrors()) {
			throw new IllegalStateException("Test has no EXPECT chunks.");
		}
		var statements = problem.getStatements();
		int initialStatementCount = 0;
		ExpectationHeader currentHeader = null;
		var expectations = new ArrayList<SemanticsExpectation>();
		for (var statement : statements) {
			var node = NodeModelUtils.findActualNodeFor(statement);
			if (node == null) {
				throw new IllegalStateException("No node for statement: " + statement);
			}
			var nextHeader = expectationsDeque.peekFirst();
			if (nextHeader != null && node.getStartLine() >= nextHeader.startLine()) {
				currentHeader = nextHeader;
				expectationsDeque.removeFirst();
			}
			if (currentHeader == null) {
				initialStatementCount++;
			} else {
				var expectation = createExpectation(currentHeader, statement, node);
				expectations.add(expectation);
			}
		}
		int statementCount = statements.size();
		if (statementCount > initialStatementCount) {
			statements.subList(initialStatementCount, statementCount).clear();
		}
		return new SemanticsTestCase(testCaseHeader.name(), testCaseHeader.allowErrors(), problem,
				Collections.unmodifiableList(expectations));
	}

	private static SemanticsExpectation createExpectation(ExpectationHeader header, Statement statement,
														  INode node) {
		if (!(statement instanceof Assertion assertion)) {
			throw new IllegalArgumentException("Only assertions are supported in EXPECT chunks, got %s instead."
					.formatted(statement.eClass().getName()));
		}
		if (assertion.isDefault()) {
			throw new IllegalArgumentException("Default assertions are not supported in EXPECT chunks.");
		}
		return new SemanticsExpectation(assertion, header.concreteness(), header.exact(),
				node.getStartLine(), header.description(), NodeModelUtils.getTokenText(node).strip());
	}
}
