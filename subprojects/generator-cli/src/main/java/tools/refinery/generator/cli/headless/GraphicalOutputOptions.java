/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.headless;

import tools.refinery.generator.cli.output.OutputOptions;
import tools.refinery.generator.cli.output.OutputTheme;

record GraphicalOutputOptions(GraphicalOutputFormat outputFormat, boolean transparent, Boolean embedFonts,
                              OutputTheme theme, Integer scale) {
	public static GraphicalOutputOptions of(OutputOptions outputOptions) {
		return switch (outputOptions.getFormat()) {
			case SVG -> new GraphicalOutputOptions(GraphicalOutputFormat.SVG, outputOptions.isTransparent(),
					outputOptions.getTheme().supportsEmbedFonts() && outputOptions.isEmbedFonts(),
					outputOptions.getTheme(), null);
			case PDF -> new GraphicalOutputOptions(GraphicalOutputFormat.PDF, outputOptions.isTransparent(),
					outputOptions.isEmbedFonts(), outputOptions.getTheme().asStaticTheme(), null);
			case PNG -> new GraphicalOutputOptions(GraphicalOutputFormat.PNG, outputOptions.isTransparent(), null,
					outputOptions.getTheme().asStaticTheme(), outputOptions.getScale());
			case null, default ->
					throw new IllegalArgumentException("Not a graphical output format: " + outputOptions.getFormat());
		};
	}
}
