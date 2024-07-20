/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.tests;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.function.Executable;
import tools.refinery.generator.ModelSemanticsFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class DynamicTestLoader {
	@Inject
	private SemanticsTestLoader testLoader;

	@Inject
	private Provider<ModelSemanticsFactory> semanticsFactoryProvider;

	public Stream<DynamicNode> fromClasspath(Class<?> klass, String name) {
		URL url;
		URI uri;
		try {
			url = safeGetResource(klass, name);
			uri = url.toURI();
		} catch (RuntimeException | URISyntaxException e) {
			return Stream.of(createErrorNode(name, null, e));
		}
		SemanticsTest test;
		try {
			test = loadFromUrl(uri, url);
		} catch (IOException | RuntimeException e) {
			return Stream.of(createErrorNode(name, uri, e));
		}
		return createDynamicNodes(uri, test);
	}

	public Stream<DynamicNode> fromClasspath(Class<?> klass, String... names) {
		return fromClasspath(klass, Stream.of(names));
	}

	public Stream<DynamicNode> fromClasspath(Class<?> klass, Stream<String> names) {
		return names.map(name -> nodeFromClasspath(klass, name));
	}

	private DynamicNode nodeFromClasspath(Class<?> klass, String name) {
		URL url;
		URI uri;
		try {
			url = safeGetResource(klass, name);
			uri = url.toURI();
		} catch (RuntimeException | URISyntaxException e) {
			return createErrorNode(name, null, e);
		}
		SemanticsTest test;
		try {
			test = loadFromUrl(uri, url);
		} catch (IOException | RuntimeException e) {
			return createErrorNode(name, uri, e);
		}
		return createDynamicNode(name, uri, test);
	}

	private static URL safeGetResource(Class<?> klass, String name) {
		var url = klass.getResource(name);
		if (url == null) {
			throw new IllegalStateException("Test resource %s was not found.".formatted(name));
		}
		return url;
	}

	private SemanticsTest loadFromUrl(URI uri, URL url) throws IOException {
		var eclipseUri = org.eclipse.emf.common.util.URI.createURI(uri.toString());
		try (var inputStream = url.openStream()) {
			return testLoader.loadStream(inputStream, eclipseUri);
		}
	}

	public Stream<DynamicNode> fromString(String problem) {
		SemanticsTest test;
		try {
			test = testLoader.loadString(problem);
		} catch (RuntimeException e) {
			return Stream.of(createErrorNode("<string>", null, e));
		}
		return createDynamicNodes(null, test);
	}

	private DynamicNode createDynamicNode(String name, URI uri, SemanticsTest test) {
		var testCases = test.testCases();
		if (testCases.size() == 1 && testCases.getFirst().name() == null) {
			var testCase = testCases.getFirst();
			return dynamicTest(name, uri, asExecutable(testCase));
		}
		var children = createDynamicNodes(uri, test);
		return dynamicContainer(name, uri, children);
	}

	private Stream<DynamicNode> createDynamicNodes(URI uri, SemanticsTest test) {
		var testCases = test.testCases();
		var children = new ArrayList<DynamicNode>();
		int testCaseCount = testCases.size();
		for (int i = 0; i < testCaseCount; i++) {
			var testCase = testCases.get(i);
			var testCaseName = testCase.name();
			if (testCaseName == null) {
				testCaseName = "[%d]".formatted(i);
			}
			children.add(dynamicTest(testCaseName, uri, asExecutable(testCase)));
		}
		return children.stream();
	}

	private Executable asExecutable(SemanticsTestCase testCase) {
		return () -> testCase.execute(semanticsFactoryProvider.get());
	}

	private DynamicNode createErrorNode(String name, URI uri, Throwable cause) {
		var messageBuilder = new StringBuilder();
		messageBuilder.append("Error while reading test '").append(name).append("'");
		if (uri != null) {
			messageBuilder.append(" from ").append(uri);
		}
		if (cause != null) {
			messageBuilder.append(": ").append(cause.getMessage());
		}
		var message = messageBuilder.toString();
		return dynamicTest(name, uri, () ->	{
			throw new RuntimeException(message, cause);
		});
	}
}
