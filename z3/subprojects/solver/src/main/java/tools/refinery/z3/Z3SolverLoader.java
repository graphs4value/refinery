/*
 * Copyright 2010-2022 Google LLC
 * Copyright 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * This file is based on
 * https://github.com/google/or-tools/blob/f3d1d5f6a67356ec38a7fd2ab607624eea8ad3a6/ortools/java/com/google/ortools/Loader.java
 * We adapted the loader logic to extract the JNI libraries of Z3 and the corresponding dependencies instead of the
 * Google OR-Tools JNI libraries.
 */
package tools.refinery.z3;

import com.sun.jna.Platform;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;

public final class Z3SolverLoader {
	private static final String JNI_LIBRARY_NAME = "z3java";
	private static final String SOLVER_LIBRARY_NAME = "z3";

	private static boolean loaded;

	private Z3SolverLoader() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static synchronized void loadNativeLibraries() {
		if (loaded) {
			return;
		}
		try {
			System.loadLibrary(getOsSpecificLibraryName(JNI_LIBRARY_NAME));
			loaded = true;
			return;
		} catch (UnsatisfiedLinkError e) {
			// Continue, we'll have to extract the libraries from the classpath.
		}
		try {
			extractAndLoad();
			loaded = true;
		} catch (IOException e) {
			throw new IllegalStateException("Could not extract and load " + JNI_LIBRARY_NAME, e);
		}
	}

	private static void extractAndLoad() throws IOException {
		var resourceName = JNI_LIBRARY_NAME + "-" + Platform.RESOURCE_PREFIX;
		var resource = Z3SolverLoader.class.getClassLoader().getResource(resourceName);
		if (resource == null) {
			throw new IllegalStateException("Resource %s was not found".formatted(resourceName));
		}
		URI resourceUri;
		try {
			resourceUri = resource.toURI();
		} catch (URISyntaxException e) {
			throw new IllegalStateException("Invalid resource URI: " + resource);
		}
		FileSystem fileSystem = null;
		boolean newFileSystem = false;
		Path extractedPath;
		try {
			try {
				fileSystem = FileSystems.newFileSystem(resourceUri, Map.of());
				newFileSystem = true;
			} catch (FileSystemAlreadyExistsException e) {
				fileSystem = FileSystems.getFileSystem(resourceUri);
			}
			var resourcePath = fileSystem.provider().getPath(resourceUri);
			if (fileSystem.equals(FileSystems.getDefault())) {
				extractedPath = resourcePath;
			} else {
				extractedPath = extract(resourcePath);
			}
		} finally {
			if (newFileSystem) {
				fileSystem.close();
			}
		}
		// We can't rely on RPATH, so we load libraries in reverse dependency order manually.
		try {
			loadFromPath(extractedPath, SOLVER_LIBRARY_NAME);
		} catch (UnsatisfiedLinkError e) {
			if (Platform.isWindows()) {
				// Try again with our packaged msvcp140 if the system one is missing.
				loadFromPathExactName(extractedPath, "vcruntime140.dll");
				loadFromPathExactName(extractedPath, "vcruntime140_1.dll");
				loadFromPathExactName(extractedPath, "msvcp140.dll");
				loadFromPath(extractedPath, SOLVER_LIBRARY_NAME);
			} else {
				throw e;
			}
		}
		loadFromPath(extractedPath, JNI_LIBRARY_NAME);
	}

	private static Path extract(Path resourcePath) throws IOException {
		var tempDir = Files.createTempDirectory(JNI_LIBRARY_NAME).toAbsolutePath();
		tempDir.toFile().deleteOnExit();
		Files.walkFileTree(resourcePath, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				var result = super.preVisitDirectory(dir, attrs);
				var targetPath = getTargetPath(dir, resourcePath, tempDir);
				if (!Files.exists(targetPath)) {
					Files.createDirectory(targetPath);
					targetPath.toFile().deleteOnExit();
				}
				return result;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				var result = super.visitFile(file, attrs);
				var targetPath = getTargetPath(file, resourcePath, tempDir);
				Files.copy(file, targetPath);
				targetPath.toFile().deleteOnExit();
				return result;
			}
		});
		return tempDir;
	}

	private static Path getTargetPath(Path sourcePath, Path resourcePath, Path tempDir) {
		var targetPath = tempDir.resolve(resourcePath.relativize(sourcePath).toString()).normalize();
		if (!targetPath.startsWith(tempDir)) {
			throw new IllegalStateException("Target path '%s' for '%s' is outside '%s'"
					.formatted(targetPath, sourcePath, tempDir));
		}
		return targetPath;
	}

	private static String getOsSpecificLibraryName(String libraryName) {
		var osSpecificLibraryNamePrefix = Platform.isWindows() ? "lib" : "";
		return osSpecificLibraryNamePrefix + libraryName;
	}

	private static void loadFromPath(Path extractedPath, String libraryName) {
		var osSpecificLibraryName = getOsSpecificLibraryName(libraryName);
		loadFromPathExactName(extractedPath, System.mapLibraryName(osSpecificLibraryName));
	}

	private static void loadFromPathExactName(Path extractedPath, String exactName) {
		var library = extractedPath.resolve(exactName)
				.toAbsolutePath()
				.toString();
		System.load(library);
	}
}
