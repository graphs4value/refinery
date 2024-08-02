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
import org.opentest4j.TestAbortedException;
import tools.refinery.generator.ModelSemanticsFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class DynamicTestLoader {
	private static final String TEST_SUFFIX = ".problem";

	@Inject
	private Provider<SemanticsTestLoader> testLoaderProvider;

	private Provider<ModelSemanticsFactory> semanticsFactoryProvider;

	@Inject
	public void setSemanticsFactoryProvider(Provider<ModelSemanticsFactory> semanticsFactoryProvider) {
		this.semanticsFactoryProvider = semanticsFactoryProvider;
	}

	public Stream<DynamicNode> allFromClasspath(Class<?> contextClass) {
		var paths = getExtraPaths(contextClass);
		if (paths.isEmpty()) {
			throw new IllegalArgumentException("No file system paths found for class: " + contextClass);
		}
		var loader = getTestLoader(paths);
		var nodes = new ArrayList<DynamicNode>();
		for (var path : paths) {
			List<DynamicNode> childNodes;
			try {
				childNodes = nodesFromPath(loader, path);
			} catch (IOException e) {
				throw new IllegalArgumentException("Failed to enumerate path: " + path, e);
			}
			nodes.addAll(childNodes);
		}
		if (nodes.isEmpty()) {
			throw new TestAbortedException("No tests found for class: " + contextClass);
		}
		return nodes.stream();
	}

	private List<DynamicNode> nodesFromPath(SemanticsTestLoader loader, Path path) throws IOException {
		var nodes = new ArrayList<DynamicNode>();
		try (var childList = Files.list(path)) {
			var iterator = childList.iterator();
			while (iterator.hasNext()) {
				var childPath = iterator.next();
				var fileName = childPath.getFileName().toString();
				var uri = childPath.toUri();
				try {
					if (Files.isDirectory(childPath)) {
						var childNodes = nodesFromPath(loader, childPath);
						if (!childNodes.isEmpty()) {
							nodes.add(dynamicContainer(fileName, uri, childNodes.stream()));
						}
					} else if (fileName.endsWith(TEST_SUFFIX)) {
						SemanticsTest test = loader.loadFile(childPath.toFile());
						if (test != null) {
							nodes.add(createDynamicNode(fileName, uri, test));
						}
					}
				} catch (IOException | RuntimeException e) {
					nodes.add(createErrorNode(fileName, uri, e));
				}
			}
		}
		return nodes;
	}

	public Stream<DynamicNode> fromClasspath(Class<?> contextClass, String name) {
		var loader = getTestLoader(contextClass);
		URL url;
		URI uri;
		try {
			url = safeGetResource(contextClass, name);
			uri = url.toURI();
		} catch (RuntimeException | URISyntaxException e) {
			return Stream.of(createErrorNode(name, null, e));
		}
		SemanticsTest test;
		try {
			test = loadFromUrl(loader, uri, url);
		} catch (IOException | RuntimeException e) {
			return Stream.of(createErrorNode(name, uri, e));
		}
		return createDynamicNodes(uri, test);
	}

	public Stream<DynamicNode> fromClasspath(Class<?> contextClass, String... names) {
		return fromClasspath(contextClass, Stream.of(names));
	}

	public Stream<DynamicNode> fromClasspath(Class<?> contextClass, Stream<String> names) {
		var loader = getTestLoader(contextClass);
		return names.map(name -> nodeFromClasspath(loader, contextClass, name));
	}

	private DynamicNode nodeFromClasspath(SemanticsTestLoader loader, Class<?> contextClass, String name) {
		URL url;
		URI uri;
		try {
			url = safeGetResource(contextClass, name);
			uri = url.toURI();
		} catch (RuntimeException | URISyntaxException e) {
			return createErrorNode(name, null, e);
		}
		SemanticsTest test;
		try {
			test = loadFromUrl(loader, uri, url);
		} catch (IOException | RuntimeException e) {
			return createErrorNode(name, uri, e);
		}
		return createDynamicNode(name, uri, test);
	}

	private static URL safeGetResource(Class<?> contextClass, String name) {
		var url = contextClass.getResource(name);
		if (url == null) {
			throw new IllegalStateException("Test resource %s was not found.".formatted(name));
		}
		return url;
	}

	private SemanticsTest loadFromUrl(SemanticsTestLoader testLoader, URI uri, URL url) throws IOException {
		var eclipseUri = org.eclipse.emf.common.util.URI.createURI(uri.toString());
		try (var inputStream = url.openStream()) {
			return testLoader.loadStream(inputStream, eclipseUri);
		}
	}

	public Stream<DynamicNode> fromString(Class<?> contextClass, String problem) {
		var testLoader = getTestLoader(contextClass);
		SemanticsTest test;
		try {
			test = testLoader.loadString(problem);
		} catch (RuntimeException e) {
			return Stream.of(createErrorNode("<string>", null, e));
		}
		return createDynamicNodes(null, test);
	}

	public Stream<DynamicNode> fromString(String problem) {
		return fromString(null, problem);
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
				testCaseName = "[%d]".formatted(i + 1);
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
		return dynamicTest(name, uri, () -> {
			throw new RuntimeException(message, cause);
		});
	}

	private SemanticsTestLoader getTestLoader(Class<?> contextClass) {
		var extraPaths = getExtraPaths(contextClass);
		return getTestLoader(extraPaths);
	}

	private SemanticsTestLoader getTestLoader(List<Path> extraPaths) {
		var loader = testLoaderProvider.get();
		extraPaths.forEach(loader::extraPath);
		return loader;
	}

	private List<Path> getExtraPaths(Class<?> contextClass) {
		if (contextClass == null) {
			return List.of();
		}
		var resourceName = contextClass.getPackageName().replace('.', '/');
		Enumeration<URL> resources;
		try {
			resources = contextClass.getClassLoader().getResources(resourceName);
		} catch (IOException e) {
			// Failed to find any resources.
			return List.of();
		}
		var directories = new ArrayList<Path>();
		while (resources.hasMoreElements()) {
			var url = resources.nextElement();
			var path = getPath(url);
			if (path != null && path.getFileSystem() == FileSystems.getDefault()) {
				directories.add(path);
			}
		}
		return directories;
	}

	private static Path getPath(URL url) {
		URI uri;
		try {
			uri = url.toURI();
		} catch (URISyntaxException e) {
			return null;
		}
		Path path;
		try {
			path = Path.of(uri);
		} catch (FileSystemNotFoundException e) {
			return null;
		}
		return path.toAbsolutePath();
	}
}
