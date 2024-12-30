/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.gson;

import com.google.gson.Gson;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Provides support for reading and writing JSON in Jersey using Gson.
 * <p>
 * Since Xtext relies on Gson, we can't use the usual Jackson provider for JSON to avoid using two different JSON
 * libraries at the same time.
 * </p>
 * <p>
 * This implementation is loosely based on <a href="https://stackoverflow.com/a/26829468">this StackOverflow</a>
 * answer, but we take advantage of the new {@code default} interface methods in Jackson and only read and write the
 * {@code application/json} media type.
 * </p>
 */
@Provider
public class GsonJerseyProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
	private final Gson gson = GsonUtil.getGson();

	@Override
	public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
		return isJson(mediaType);
	}

	@Override
	public Object readFrom(Class<Object> aClass, Type type, Annotation[] annotations, MediaType mediaType,
						   MultivaluedMap<String, String> multivaluedMap, InputStream inputStream)
			throws IOException, WebApplicationException {
		try (var reader = new BufferedReader(new InputStreamReader(inputStream))) {
			return gson.fromJson(reader, type);
		}
	}

	@Override
	public boolean isWriteable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
		return isJson(mediaType);
	}

	@Override
	public void writeTo(Object o, Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType,
						MultivaluedMap<String, Object> multivaluedMap, OutputStream outputStream)
			throws IOException, WebApplicationException {
		try (var writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
			gson.toJson(o, writer);
		}
	}

	private static boolean isJson(MediaType mediaType) {
		return MediaType.APPLICATION_JSON_TYPE.isCompatible(mediaType);
	}
}
