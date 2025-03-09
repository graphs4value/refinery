/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.tests;

import com.google.inject.Singleton;
import org.eclipse.xtext.ide.ExecutorServiceProvider;

import java.util.concurrent.ExecutorService;

@Singleton
public class DelegatingExecutorServiceProvider extends ExecutorServiceProvider {
	private ExecutorServiceProvider delegate;

	public void setDelegate(ExecutorServiceProvider delegate) {
		this.delegate = delegate;
	}

	@Override
	public ExecutorService get() {
		return delegate.get();
	}

	@Override
	public ExecutorService get(String key) {
		return delegate.get(key);
	}

	@Override
	protected ExecutorService createInstance(String key) {
		throw new IllegalStateException("Use the delegate executor service provider instead");
	}

	@Override
	public void dispose() {
		super.dispose();
		delegate.dispose();
	}
}
