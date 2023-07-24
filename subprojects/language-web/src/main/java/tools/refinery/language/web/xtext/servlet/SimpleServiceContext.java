/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.servlet;

import java.util.Map;
import java.util.Set;

import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.ISession;

import com.google.common.collect.ImmutableSet;

public record SimpleServiceContext(ISession session, Map<String, String> parameters) implements IServiceContext {
	@Override
	public Set<String> getParameterKeys() {
		return ImmutableSet.copyOf(parameters.keySet());
	}

	@Override
	public String getParameter(String key) {
		return parameters.get(key);
	}

	@Override
	public ISession getSession() {
		return session;
	}
}
