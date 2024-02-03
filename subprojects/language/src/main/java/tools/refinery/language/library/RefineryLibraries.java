/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.library;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.naming.QualifiedName;
import tools.refinery.language.scoping.imports.NamedImport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;

public final class RefineryLibraries {
	private static final ServiceLoader<RefineryLibrary> SERVICE_LOADER = ServiceLoader.load(RefineryLibrary.class);
	private static final List<NamedImport> AUTOMATIC_IMPORTS;

	static {
		var imports = new LinkedHashMap<QualifiedName, URI>();
		for (var service : SERVICE_LOADER) {
			for (var qualifiedName : service.getAutomaticImports()) {
				var uri = service.resolveQualifiedName(qualifiedName).orElseThrow(
						() -> new IllegalStateException("Automatic import %s was not found".formatted(qualifiedName)));
				if (imports.put(qualifiedName, uri) != null) {
					throw new IllegalStateException("Duplicate automatic import " + qualifiedName);
				}
			}
		}
		AUTOMATIC_IMPORTS = imports.entrySet().stream()
				.map(entry -> NamedImport.implicit(entry.getValue(), entry.getKey()))
				.toList();
	}

	private RefineryLibraries() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static List<NamedImport> getAutomaticImports() {
		return AUTOMATIC_IMPORTS;
	}

	public static Optional<URI> resolveQualifiedName(QualifiedName qualifiedName) {
		for (var service : SERVICE_LOADER) {
			var result = service.resolveQualifiedName(qualifiedName);
			if (result.isPresent()) {
				return result;
			}
		}
		return Optional.empty();
	}
}
