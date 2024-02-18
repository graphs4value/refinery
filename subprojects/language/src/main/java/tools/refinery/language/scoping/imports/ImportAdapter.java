/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping.imports;

import com.google.common.base.Splitter;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.library.RefineryLibrary;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class ImportAdapter extends AdapterImpl {
	private static final Logger LOG = Logger.getLogger(ImportAdapter.class);
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

	private ImportAdapter(ResourceSet resourceSet) {
		libraries = new ArrayList<>(DEFAULT_LIBRARIES);
		libraryPaths = new ArrayList<>(DEFAULT_PATHS);
		for (var resource : resourceSet.getResources()) {
			resourceAdded(resource);
		}
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

	@Override
	public void notifyChanged(Notification msg) {
		switch (msg.getEventType()) {
		case Notification.ADD -> {
			if (msg.getNewValue() instanceof Resource resource) {
				resourceAdded(resource);
			}
		}
		case Notification.ADD_MANY -> {
			if (msg.getNewValue() instanceof List<?> list) {
				manyResourcesAdded(list);
			}
		}
		case Notification.REMOVE -> {
			if (msg.getOldValue() instanceof Resource resource) {
				resourceRemoved(resource);
			}
		}
		case Notification.REMOVE_MANY -> {
			if (msg.getOldValue() instanceof List<?> list) {
				manyResourcesRemoved(list);
			}
		}
		default -> {
			// Nothing to update.
		}
		}
	}

	private void manyResourcesAdded(List<?> list) {
		for (var element : list) {
			if (element instanceof Resource resource) {
				resourceAdded(resource);
			}
		}
	}

	private void manyResourcesRemoved(List<?> list) {
		for (var element : list) {
			if (element instanceof Resource resource) {
				resourceRemoved(resource);
			}
		}
	}

	private void resourceAdded(Resource resource) {
		var uri = resource.getURI();
		for (var library : libraries) {
			var result = library.getQualifiedName(uri, libraryPaths);
			if (result.isPresent()) {
				var qualifiedName = result.get();
				var previousQualifiedName = uriToQualifiedNameMap.putIfAbsent(uri, qualifiedName);
				if (previousQualifiedName == null) {
					if (qualifiedNameToUriMap.put(qualifiedName, uri) != null) {
						throw new IllegalArgumentException("Duplicate resource for" + qualifiedName);
					}
				} else if (!previousQualifiedName.equals(qualifiedName)) {
					LOG.warn("Expected %s to have qualified name %s, got %s instead".formatted(
							uri, previousQualifiedName, qualifiedName));
				}
			}
		}
	}

	private void resourceRemoved(Resource resource) {
		var qualifiedName = uriToQualifiedNameMap.remove(resource.getURI());
		if (qualifiedName != null) {
			qualifiedNameToUriMap.remove(qualifiedName);
		}
	}

	public static ImportAdapter getOrInstall(ResourceSet resourceSet) {
		var adapter = getAdapter(resourceSet);
		if (adapter == null) {
			adapter = new ImportAdapter(resourceSet);
			resourceSet.eAdapters().add(adapter);
		}
		return adapter;
	}

	private static ImportAdapter getAdapter(ResourceSet resourceSet) {
        return (ImportAdapter) EcoreUtil.getAdapter(resourceSet.eAdapters(), ImportAdapter.class);
	}
}
