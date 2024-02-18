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

import java.util.Collection;
import java.util.List;

class CompositeSelectable implements ISelectable {
	private static final CompositeSelectable EMPTY = new CompositeSelectable(List.of());

	private final List<? extends ISelectable> children;

	private CompositeSelectable(List<? extends ISelectable> children) {

		this.children = children;
	}

	@Override
	public boolean isEmpty() {
		return children.isEmpty();
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjects() {
		return Iterables.concat(Iterables.transform(children, ISelectable::getExportedObjects));
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjects(EClass type, QualifiedName name, boolean ignoreCase) {
		return Iterables.concat(Iterables.transform(children, child -> child.getExportedObjects(type, name,
				ignoreCase)));
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByType(EClass type) {
		return Iterables.concat(Iterables.transform(children, child -> child.getExportedObjectsByType(type)));
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByObject(EObject object) {
		return Iterables.concat(Iterables.transform(children, child -> child.getExportedObjectsByObject(object)));
	}

	public static ISelectable of(Collection<? extends ISelectable> children) {
		var filteredChildren = children.stream().filter(selectable -> !selectable.isEmpty()).toList();
		return switch (filteredChildren.size()) {
			case 0 -> EMPTY;
			case 1 -> filteredChildren.getFirst();
			default -> new CompositeSelectable(filteredChildren);
		};
	}
}
