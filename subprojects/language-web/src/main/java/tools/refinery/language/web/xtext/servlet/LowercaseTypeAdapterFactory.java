/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class LowercaseTypeAdapterFactory<T extends Enum<T>> implements TypeAdapterFactory {
	private final Class<T> enumClass;
	private final LowercaseTypeAdapter<T> typeAdapter;

	public LowercaseTypeAdapterFactory(Class<T> enumClass) {
		this.enumClass = enumClass;
		typeAdapter = new LowercaseTypeAdapter<>(enumClass);
	}

	@Override
	public <U> TypeAdapter<U> create(Gson gson, TypeToken<U> type) {
		var rawType = type.getRawType();
		TypeAdapter<?> checkedTypeAdapter;
		if (enumClass.equals(rawType)) {
			checkedTypeAdapter = typeAdapter;
		} else if (enumClass.isAssignableFrom(rawType)) {
			checkedTypeAdapter = new CheckedTypeAdapter<>(rawType);
		} else {
			return null;
		}
		// We just checked that this cast is safe if U has no generic arguments.
		@SuppressWarnings("unchecked")
		var unsafeTypeAdapter = (TypeAdapter<U>) checkedTypeAdapter;
		return unsafeTypeAdapter;
	}

	private class CheckedTypeAdapter<U> extends TypeAdapter<U> {
		private final Class<U> checkedClass;

		public CheckedTypeAdapter(Class<U> checkedClass) {
			this.checkedClass = checkedClass;
		}

		@Override
		public void write(JsonWriter out, U value) throws IOException {
			typeAdapter.write(out, enumClass.cast(value));
		}

		@Override
		public U read(JsonReader in) throws IOException {
			var value = typeAdapter.read(in);
			if (value == null) {
				return null;
			}
			if (!checkedClass.isInstance(value)) {
				throw new JsonParseException("Expected enum constant of runtime type %s but got %s"
						.formatted(checkedClass.getName(), value.getClass().getName()));
			}
			// We just checked this is valid.
			@SuppressWarnings("unchecked")
			var unsafeValue = (U) value;
			return unsafeValue;
		}
	}
}
