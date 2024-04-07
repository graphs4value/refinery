/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.scoping.imports;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.jetbrains.annotations.NotNull;
import tools.refinery.language.expressions.TermInterpreter;
import tools.refinery.language.utils.BuiltinSymbols;

@Singleton
public class ImportAdapterProvider {
	@Inject
	private Provider<ImportAdapter> delegateProvider;

	public BuiltinSymbols getBuiltinSymbols(@NotNull EObject context) {
		var adapter = getOrInstall(context);
		return adapter.getBuiltinSymbols();
	}

	public BuiltinSymbols getBuiltinSymbols(@NotNull Resource context) {
		var adapter = getOrInstall(context);
		return adapter.getBuiltinSymbols();
	}

	public TermInterpreter getTermInterpreter(@NotNull EObject context) {
		var adapter = getOrInstall(context);
		return adapter.getTermInterpreter();
	}

	public ImportAdapter getOrInstall(@NotNull EObject context) {
		var resource = context.eResource();
		if (resource == null) {
			throw new IllegalArgumentException("context is not in a resource");
		}
		return getOrInstall(resource);
	}

	public ImportAdapter getOrInstall(@NotNull Resource context) {
		var resourceSet = context.getResourceSet();
		if (resourceSet == null) {
			throw new IllegalArgumentException("context is not in a resource set");
		}
		return getOrInstall(resourceSet);
	}

	public ImportAdapter getOrInstall(@NotNull ResourceSet resourceSet) {
		var adapter = getAdapter(resourceSet);
		if (adapter == null) {
			adapter = delegateProvider.get();
			adapter.setResourceSet(resourceSet);
			resourceSet.eAdapters().add(adapter);
		}
		return adapter;
	}

	public static ImportAdapter getAdapter(@NotNull ResourceSet resourceSet) {
		return (ImportAdapter) EcoreUtil.getAdapter(resourceSet.eAdapters(), ImportAdapter.class);
	}
}
