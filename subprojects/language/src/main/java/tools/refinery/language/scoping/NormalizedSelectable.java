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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

class NormalizedSelectable implements ISelectable {
	private final ISelectable delegateSelectable;
	private final QualifiedName originalPrefix;
	private final QualifiedName normalizedPrefix;

	public NormalizedSelectable(ISelectable delegateSelectable, QualifiedName originalPrefix,
								QualifiedName normalizedPrefix) {
		if (originalPrefix.equals(QualifiedName.EMPTY)) {
			throw new IllegalArgumentException("Cannot normalize empty qualified name prefix");
		}
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
		return getAliasedElements(delegateIterable);
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjects(EClass type, QualifiedName name, boolean ignoreCase) {
		boolean startsWith = ignoreCase ? name.startsWithIgnoreCase(normalizedPrefix) :
				name.startsWith(normalizedPrefix);
		if (startsWith && name.getSegmentCount() > normalizedPrefix.getSegmentCount()) {
			var originalName = originalPrefix.append(name.skipFirst(normalizedPrefix.getSegmentCount()));
			return Iterables.transform(
					delegateSelectable.getExportedObjects(type, originalName, ignoreCase),
					description -> {
						var normalizedName = normalizedPrefix.append(
								description.getName().skipFirst(originalPrefix.getSegmentCount()));
						return new AliasedEObjectDescription(normalizedName, description);
					});
		}
		return List.of();
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByType(EClass type) {
		var delegateIterable = delegateSelectable.getExportedObjectsByType(type);
		return getAliasedElements(delegateIterable);
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByObject(EObject object) {
		var delegateIterable = delegateSelectable.getExportedObjectsByObject(object);
		return getAliasedElements(delegateIterable);
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
}
