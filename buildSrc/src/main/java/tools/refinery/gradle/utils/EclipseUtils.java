/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle.utils;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.plugins.ide.api.XmlFileContentMerger;
import org.gradle.plugins.ide.eclipse.model.AbstractClasspathEntry;
import org.gradle.plugins.ide.eclipse.model.Classpath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class EclipseUtils {
	private static final String GRADLE_USED_BY_SCOPE_ATTRIBUTE = "gradle_used_by_scope";
	private static final String GRADLE_USED_BY_SCOPE_SEPARATOR = ",";

	private EclipseUtils() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static void patchClasspathEntries(EclipseModel eclipseModel, Consumer<AbstractClasspathEntry> consumer) {
		whenClasspathFileMerged(eclipseModel.getClasspath().getFile(),
				classpath -> patchClasspathEntries(classpath, consumer));
	}

	public static void patchClasspathEntries(Classpath eclipseClasspath, Consumer<AbstractClasspathEntry> consumer) {
		for (var entry : eclipseClasspath.getEntries()) {
			if (entry instanceof AbstractClasspathEntry abstractClasspathEntry) {
				consumer.accept(abstractClasspathEntry);
			}
		}
	}

	/**
	 * Avoids ambiguous call to ({@link XmlFileContentMerger#whenMerged(Closure)} versus
	 * {@link XmlFileContentMerger#whenMerged(Action)}) in Kotlin build scripts.
	 * <p>
	 * The {@code Closure} variant will use the build script itself as {@code this}, and Kotlin will consider any
	 * type ascription as a cast of {@code this} (instead of the argument of the {@code Action<?>}). This results in
	 * a mysterious {@link ClassCastException}, since the class generated from the build script doesn't extend from
	 * {@link Classpath}. Using this helper method selects the correct call and applies the cast properly.
	 *
	 * @param file     The Eclipse classpath file.
	 * @param consumer The lambda to run on when the classpath file is merged.
	 */
	public static void whenClasspathFileMerged(XmlFileContentMerger file, Consumer<Classpath> consumer) {
		file.whenMerged(untypedClasspath -> {
			var classpath = (Classpath) untypedClasspath;
			consumer.accept(classpath);
		});
	}

	public static void patchGradleUsedByScope(AbstractClasspathEntry entry, Consumer<Set<String>> consumer) {
		var entryAttributes = entry.getEntryAttributes();
		var usedByValue = entryAttributes.get(GRADLE_USED_BY_SCOPE_ATTRIBUTE);
		Set<String> usedBySet;
		if (usedByValue instanceof String usedByString) {
			usedBySet = new LinkedHashSet<>(List.of(usedByString.split(GRADLE_USED_BY_SCOPE_SEPARATOR)));
		} else {
			usedBySet = new LinkedHashSet<>();
		}
		consumer.accept(usedBySet);
		if (usedBySet.isEmpty()) {
			entryAttributes.remove(GRADLE_USED_BY_SCOPE_ATTRIBUTE);
		} else {
			var newUsedByString = String.join(GRADLE_USED_BY_SCOPE_SEPARATOR, usedBySet);
			entryAttributes.put(GRADLE_USED_BY_SCOPE_ATTRIBUTE, newUsedByString);
		}
	}
}
