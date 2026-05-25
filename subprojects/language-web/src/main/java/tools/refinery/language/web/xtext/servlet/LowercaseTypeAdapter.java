/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.servlet;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LowercaseTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
	private final Class<T> enumClass;
	private final Map<T, String> nameMap;
	private final Map<String, T> reverseMap;

	public LowercaseTypeAdapter(Class<T> enumClass) {
		this.enumClass = enumClass;
		var values = enumClass.getEnumConstants();
		int size = values.length;
		nameMap = HashMap.newHashMap(size);
		reverseMap = HashMap.newHashMap(size);
		var converter = CaseFormat.UPPER_UNDERSCORE.converterTo(CaseFormat.LOWER_CAMEL);
		for (var value : values) {
			var name = converter.convert(value.name());
			nameMap.put(value, name);
			var oldValue = reverseMap.put(name, value);
			if (oldValue != null) {
				throw new IllegalArgumentException("Enum constants %s and %s of %s have the same lowercase name %s"
						.formatted(value.name(), oldValue.name(), enumClass.getName(), name));
			}
		}
	}

	@Override
	public void write(JsonWriter out, T value) throws IOException {
		if (value == null) {
			out.nullValue();
			return;
		}
		var name = nameMap.get(value);
		if (name == null) {
			throw new IllegalArgumentException("Unexpected enum value %s for enum type %s"
					.formatted(value, enumClass.getName()));
		}
		out.value(name);
	}

	@Override
	public T read(JsonReader in) throws IOException {
		if (in.peek() == JsonToken.NULL) {
			in.nextNull();
			return null;
		}
		var string = in.nextString();
		var value = reverseMap.get(string);
		if (value == null) {
			throw new JsonParseException("No enum constant with converted name %s for enum type %s"
					.formatted(string, enumClass.getSimpleName()));
		}
		return value;
	}
}
