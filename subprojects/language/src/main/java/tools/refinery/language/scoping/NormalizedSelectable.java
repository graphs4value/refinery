/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping;

import com.google.common.collect.Iterables;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.ISelectable;
import org.eclipse.xtext.resource.impl.AliasedEObjectDescription;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class NormalizedSelectable implements ISelectable {
	private final ISelectable delegateSelectable;
	private final QualifiedName originalPrefix;
	private final QualifiedName normalizedPrefix;

	private NormalizedSelectable(ISelectable delegateSelectable, QualifiedName originalPrefix,
								 QualifiedName normalizedPrefix) {
		this.delegateSelectable = delegateSelectable;
		this.originalPrefix = originalPrefix;
		this.normalizedPrefix = normalizedPrefix;
	}

	@Override
	public boolean isEmpty() {
		return delegateSelectable.isEmpty();
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjects() {
		var delegateIterable = delegateSelectable.getExportedObjects();
		var aliasedIterable = getAliasedElements(delegateIterable);
		return Iterables.concat(delegateIterable, aliasedIterable);
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjects(EClass type, QualifiedName name, boolean ignoreCase) {
		var delegateIterable = delegateSelectable.getExportedObjects(type, name, ignoreCase);
		boolean startsWith = ignoreCase ? name.startsWithIgnoreCase(normalizedPrefix) :
				name.startsWith(normalizedPrefix);
		if (startsWith && name.getSegmentCount() > normalizedPrefix.getSegmentCount()) {
			var originalName = originalPrefix.append(name.skipFirst(normalizedPrefix.getSegmentCount()));
			var originalIterable = Iterables.transform(
					delegateSelectable.getExportedObjects(type, originalName, ignoreCase),
					description -> {
						var normalizedName = normalizedPrefix.append(
								description.getName().skipFirst(originalPrefix.getSegmentCount()));
						return new AliasedEObjectDescription(normalizedName, description);
					});
			return Iterables.concat(originalIterable, delegateIterable);
		}
		return delegateIterable;
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByType(EClass type) {
		var delegateIterable = delegateSelectable.getExportedObjectsByType(type);
		var aliasedIterable = getAliasedElements(delegateIterable);
		return Iterables.concat(delegateIterable, aliasedIterable);
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByObject(EObject object) {
		var delegateIterable = delegateSelectable.getExportedObjectsByObject(object);
		var aliasedIterable = getAliasedElements(delegateIterable);
		return Iterables.concat(delegateIterable, aliasedIterable);
	}

	private Iterable<IEObjectDescription> getAliasedElements(Iterable<IEObjectDescription> delegateIterable) {
		return () -> new Iterator<>() {
			private final Iterator<IEObjectDescription> delegateIterator = delegateIterable.iterator();
			private IEObjectDescription next = computeNext();

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@Override
			public IEObjectDescription next() {
				if (!hasNext()) {
					throw new NoSuchElementException();
				}
				var current = next;
				next = computeNext();
				return current;
			}

			private IEObjectDescription computeNext() {
				while (delegateIterator.hasNext()) {
					var description = delegateIterator.next();
					var qualifiedName = description.getName();
					if (qualifiedName.startsWith(originalPrefix) &&
							qualifiedName.getSegmentCount() > originalPrefix.getSegmentCount()) {
						var alias = normalizedPrefix.append(qualifiedName.skipFirst(originalPrefix.getSegmentCount()));
						return new AliasedEObjectDescription(alias, description);
					}
				}
				return null;
			}
		};
	}

	public static ISelectable of(@NotNull ISelectable delegateSelectable, @NotNull QualifiedName originalPrefix,
								 @NotNull QualifiedName normalizedPrefix) {
		if (originalPrefix.equals(normalizedPrefix)) {
			return delegateSelectable;
		}
		if (originalPrefix.equals(QualifiedName.EMPTY)) {
			throw new IllegalArgumentException("Cannot normalize empty qualified name prefix");
		}
		return new NormalizedSelectable(delegateSelectable, originalPrefix, normalizedPrefix);
	}
}
