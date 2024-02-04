/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping.imports;

import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.library.RefineryLibrary;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class ImportAdapter extends AdapterImpl {
	private static final List<RefineryLibrary> DEFAULT_LIBRARIES;
	private static final List<Path> DEFAULT_PATHS;

	static {
		var serviceLoader = ServiceLoader.load(RefineryLibrary.class);
		var defaultLibraries = new ArrayList<RefineryLibrary>();
		for (var service : serviceLoader) {
			defaultLibraries.add(service);
		}
		DEFAULT_LIBRARIES = List.copyOf(defaultLibraries);
		var pathEnv = System.getenv("REFINERY_LIBRARY_PATH");
		if (pathEnv == null) {
			DEFAULT_PATHS = List.of();
		} else {
			DEFAULT_PATHS = Splitter.on(File.pathSeparatorChar)
					.splitToStream(pathEnv)
					.map(pathString -> Path.of(pathString).toAbsolutePath().normalize())
					.toList();
		}
	}

	private final List<RefineryLibrary> libraries;
	private final List<Path> libraryPaths;
	private final Cache<QualifiedName, QualifiedName> failedResolutions =
			CacheBuilder.newBuilder().maximumSize(100).build();
	private final Map<QualifiedName, URI> qualifiedNameToUriMap = new LinkedHashMap<>();
	private final Map<URI, QualifiedName> uriToQualifiedNameMap = new LinkedHashMap<>();

	private ImportAdapter() {
		libraries = new ArrayList<>(DEFAULT_LIBRARIES);
		libraryPaths = new ArrayList<>(DEFAULT_PATHS);
	}

	@Override
	public boolean isAdapterForType(Object type) {
		return type == ImportAdapter.class;
	}

	public List<RefineryLibrary> getLibraries() {
		return libraries;
	}

	public List<Path> getLibraryPaths() {
		return libraryPaths;
	}

	public URI resolveQualifiedName(QualifiedName qualifiedName) {
		var uri = getResolvedUri(qualifiedName);
		if (uri != null) {
			return uri;
		}
		if (isFailed(qualifiedName)) {
			return null;
		}
		for (var library : libraries) {
			var result = library.resolveQualifiedName(qualifiedName, libraryPaths);
			if (result.isPresent()) {
				uri = result.get();
				markAsResolved(qualifiedName, uri);
				return uri;
			}
		}
		markAsUnresolved(qualifiedName);
		return null;
	}

	private URI getResolvedUri(QualifiedName qualifiedName) {
		return qualifiedNameToUriMap.get(qualifiedName);
	}

	private boolean isFailed(QualifiedName qualifiedName) {
		return failedResolutions.getIfPresent(qualifiedName) != null;
	}

	private void markAsResolved(QualifiedName qualifiedName, URI uri) {
		if (qualifiedNameToUriMap.put(qualifiedName, uri) != null) {
			throw new IllegalArgumentException("Already resolved " + qualifiedName);
		}
		// We don't need to signal an error here, because modules with multiple qualified names will lead to
		// validation errors later.
		uriToQualifiedNameMap.putIfAbsent(uri, qualifiedName);
		failedResolutions.invalidate(qualifiedName);
	}

	private void markAsUnresolved(QualifiedName qualifiedName) {
		failedResolutions.put(qualifiedName, qualifiedName);
	}

	public QualifiedName getQualifiedName(URI uri) {
		return uriToQualifiedNameMap.get(uri);
	}

	public static ImportAdapter getOrInstall(ResourceSet resourceSet) {
		var adapter = getAdapter(resourceSet);
		if (adapter == null) {
			adapter = new ImportAdapter();
			resourceSet.eAdapters().add(adapter);
		}
		return adapter;
	}

	private static ImportAdapter getAdapter(ResourceSet resourceSet) {
        return (ImportAdapter) EcoreUtil.getAdapter(resourceSet.eAdapters(), ImportAdapter.class);
	}

	public static void copySettings(EObject context, ResourceSet newResourceSet) {
		var resource = context.eResource();
		if (resource == null) {
			return;
		}
		var originalResourceSet = resource.getResourceSet();
		if (originalResourceSet == null) {
			return;
		}
		copySettings(originalResourceSet, newResourceSet);
	}

	public static void copySettings(ResourceSet originalResourceSet, ResourceSet newResourceSet) {
		var originalAdapter = getAdapter(originalResourceSet);
		if (originalAdapter == null) {
			return;
		}
		var newAdapter = getOrInstall(newResourceSet);
		newAdapter.libraries.clear();
		newAdapter.libraries.addAll(originalAdapter.libraries);
		newAdapter.libraryPaths.clear();
		newAdapter.libraryPaths.addAll(originalAdapter.libraryPaths);
		newAdapter.uriToQualifiedNameMap.putAll(originalAdapter.uriToQualifiedNameMap);
		newAdapter.qualifiedNameToUriMap.putAll(originalAdapter.qualifiedNameToUriMap);
	}
}
