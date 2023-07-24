/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.servlet;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.xtext.web.server.ISession;
import org.eclipse.xtext.xbase.lib.Functions.Function0;

public class SimpleSession implements ISession {
	private Map<Object, Object> map = new HashMap<>();

	@Override
	public <T> T get(Object key) {
		@SuppressWarnings("unchecked")
		var value = (T) map.get(key);
		return value;
	}

	@Override
	public <T> T get(Object key, Function0<? extends T> factory) {
		@SuppressWarnings("unchecked")
		var value = (T) map.computeIfAbsent(key, absentKey -> factory.apply());
		return value;
	}

	@Override
	public void put(Object key, Object value) {
		map.put(key, value);
	}

	@Override
	public void remove(Object key) {
		map.remove(key);
	}
}
