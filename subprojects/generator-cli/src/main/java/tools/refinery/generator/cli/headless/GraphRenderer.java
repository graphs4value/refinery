/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.headless;

import com.beust.jcommander.ParameterException;
import com.google.gson.JsonObject;
import tools.refinery.generator.cli.output.OutputOptions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class GraphRenderer {
	public byte[] render(OutputOptions options, byte[] serializedJson) throws IOException {
		if (System.getenv(HeadlessRefinery.ENDPOINT_ENV_VAR) == null) {
			throw new ParameterException("Graphical output is only supported in the desktop application");
		}
		var header = GraphicalOutputOptions.of(options);
		var headerBytes = HeadlessGsonUtil.getGson().toJson(header).getBytes(StandardCharsets.UTF_8);
		var message = new byte[4 + headerBytes.length + serializedJson.length];
		HeadlessRefinery.writeLength(message, headerBytes.length);
		System.arraycopy(headerBytes, 0, message, 4, headerBytes.length);
		System.arraycopy(serializedJson, 0, message, 4 + headerBytes.length, serializedJson.length);
		byte[] response;
		try (var headless = HeadlessRefinery.connect()) {
			response = headless.interact(message);
		}
		int responseHeaderLength = (int) HeadlessRefinery.readLength(response);
		JsonObject responseHeader;
		try (var inputStream = new ByteArrayInputStream(response, 4, responseHeaderLength);
		     var streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			responseHeader = HeadlessGsonUtil.getGson().fromJson(streamReader, JsonObject.class);
		}
		if (responseHeader == null) {
			throw new IllegalStateException("Unexpected end of response");
		}
		var result = responseHeader.get("result");
		if (result == null || !result.isJsonPrimitive()) {
			throw new IllegalStateException("Invalid response header");
		}
		if ("success".equals(result.getAsString())) {
			int offset = 4 + responseHeaderLength;
			return Arrays.copyOfRange(response, offset, response.length);
		}
		var error = responseHeader.get("error");
		if (error == null || !error.isJsonPrimitive()) {
			throw new IllegalStateException("Unknown error");
		}
		throw new IllegalStateException(error.getAsString());
	}
}
