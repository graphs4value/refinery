/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server;

import java.util.Set;

import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.ISession;

import tools.refinery.language.web.xtext.server.push.PrecomputationListener;

public record SubscribingServiceContext(IServiceContext delegate, PrecomputationListener subscriber)
		implements IServiceContext {
	@Override
	public Set<String> getParameterKeys() {
		return delegate.getParameterKeys();
	}

	@Override
	public String getParameter(String key) {
		return delegate.getParameter(key);
	}

	@Override
	public ISession getSession() {
		return delegate.getSession();
	}
}
