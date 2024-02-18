/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.resource.ISelectable;
import tools.refinery.language.naming.NamingUtil;

import java.util.List;

public class NoFullyQualifiedNamesSelectable implements ISelectable {
	private final ISelectable delegateSelectable;

	// {@link com.google.common.base.Predicate} required by Xtext API.
	@SuppressWarnings("squid:S4738")
	private final Predicate<IEObjectDescription> filter =
			eObjectDescription -> !NamingUtil.isFullyQualified(eObjectDescription.getName());

    public NoFullyQualifiedNamesSelectable(ISelectable delegateSelectable) {
        this.delegateSelectable = delegateSelectable;
    }

    @Override
	public boolean isEmpty() {
		return delegateSelectable.isEmpty();
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjects() {
		return filter(delegateSelectable.getExportedObjects());
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjects(EClass type, QualifiedName name, boolean ignoreCase) {
		if (NamingUtil.isFullyQualified(name)) {
			return List.of();
		}
		return delegateSelectable.getExportedObjects(type, name, ignoreCase);
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByType(EClass type) {
		return filter(delegateSelectable.getExportedObjectsByType(type));
	}

	@Override
	public Iterable<IEObjectDescription> getExportedObjectsByObject(EObject object) {
		return filter(delegateSelectable.getExportedObjectsByObject(object));
	}

	private Iterable<IEObjectDescription> filter(Iterable<IEObjectDescription> eObjectDescriptions) {
		return Iterables.filter(eObjectDescriptions, filter);
	}
}
