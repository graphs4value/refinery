/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.servlet;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Locale;

public class LowercaseTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
	private final Class<T> enumClass;

	public LowercaseTypeAdapter(Class<T> enumClass) {
		this.enumClass = enumClass;
	}

	@Override
	public void write(JsonWriter out, T value) throws IOException {
		if (value == null) {
			out.nullValue();
			return;
		}
		out.value(value.name().toLowerCase(Locale.ROOT));
	}

	@Override
	public T read(JsonReader in) throws IOException {
		return Enum.valueOf(enumClass, in.nextString().toUpperCase(Locale.ROOT));
	}
}
