/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.adapter;

import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract non-sealed class ModelAdapterType<T1 extends ModelAdapter, T2 extends ModelStoreAdapter,
		T3 extends ModelAdapterBuilder> implements AnyModelAdapterType {
	private final Class<? extends T1> modelAdapterClass;
	private final Class<? extends T2> modelStoreAdapterClass;
	private final Class<? extends T3> modelAdapterBuilderClass;
	private final Set<AnyModelAdapterType> supportedAdapters = new HashSet<>();

	protected ModelAdapterType(Class<T1> modelAdapterClass, Class<T2> modelStoreAdapterClass,
							   Class<T3> modelAdapterBuilderClass) {
		checkReturnType(modelAdapterClass, modelStoreAdapterClass, "createModelAdapter", Model.class);
		checkReturnType(modelStoreAdapterClass, modelAdapterBuilderClass, "createStoreAdapter", ModelStore.class);
		this.modelAdapterClass = modelAdapterClass;
		this.modelStoreAdapterClass = modelStoreAdapterClass;
		this.modelAdapterBuilderClass = modelAdapterBuilderClass;
		supportedAdapters.add(this);
	}

	private void checkReturnType(Class<?> expectedReturnType, Class<?> ownerClass, String methodName,
								 Class<?>... argumentTypes) {
		Method method;
		try {
			method = ownerClass.getMethod(methodName, argumentTypes);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Invalid %s: %s#%s method is required"
					.formatted(this, ownerClass.getName(), methodName), e);
		}
		var returnType = method.getReturnType();
		if (!expectedReturnType.isAssignableFrom(returnType)) {
			throw new IllegalStateException("Invalid %s: %s is not assignable from the return type %s of %s#%s"
					.formatted(this, expectedReturnType.getName(), returnType.getCanonicalName(),
							ownerClass.getName(), methodName));
		}
	}

	protected void extendsAdapter(ModelAdapterType<? super T1, ? super T2, ? super T3> superAdapter) {
		supportedAdapters.addAll(superAdapter.supportedAdapters);
	}

	@Override
	public final Class<? extends T1> getModelAdapterClass() {
		return modelAdapterClass;
	}

	@Override
	public final Class<? extends T2> getModelStoreAdapterClass() {
		return modelStoreAdapterClass;
	}

	@Override
	public final Class<? extends T3> getModelAdapterBuilderClass() {
		return modelAdapterBuilderClass;
	}

	@Override
	public Collection<AnyModelAdapterType> getSupportedAdapterTypes() {
		return Collections.unmodifiableCollection(supportedAdapters);
	}

	@Override
	public String getName() {
		return "%s.ADAPTER".formatted(this.getClass().getName());
	}

	@Override
	public String toString() {
		return getName();
	}
}
