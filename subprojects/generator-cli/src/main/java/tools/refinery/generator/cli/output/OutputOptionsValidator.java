/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.output;

import com.beust.jcommander.IParametersValidator;
import com.beust.jcommander.ParameterException;
import org.jetbrains.annotations.VisibleForTesting;
import tools.refinery.generator.cli.utils.CliUtils;

import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public sealed abstract class OutputOptionsValidator implements IParametersValidator {
	@VisibleForTesting
	static final BooleanSupplier DEFAULT_TERMINAL_DETECTOR = () -> {
		var console = System.console();
		return console != null && console.isTerminal();
	};

	@VisibleForTesting
	static BooleanSupplier terminalDetector = DEFAULT_TERMINAL_DETECTOR;

	private final OutputOptions outputOptions;

	protected OutputOptionsValidator(OutputOptions outputOptions) {
		this.outputOptions = outputOptions;
	}

	@Override
	public void validate(Map<String, Object> parameters) throws ParameterException {
		var outputPath = (String) parameters.get("-output");
		var format = Optional.ofNullable((OutputFormat) parameters.get("-format"))
				.or(() -> outputOptions.getOutputFormatFromOutputPath(outputPath))
				.orElse(outputOptions.getFormat());
		var transparent = (Boolean) parameters.get("-transparent");
		var embedFonts = (Boolean) parameters.get("-embed-fonts");
		var theme = (OutputTheme) parameters.get("-theme");
		var scale = (Integer) parameters.get("-scale");
		boolean isTerminal = terminalDetector.getAsBoolean();
		if (!isValidOutputFormat(format)) {
			throw new ParameterException("This command does not support %s output".formatted(format));
		}
		if (CliUtils.isStandardStream(outputPath) && isTerminal && !format.isConsoleOutputAllowed()) {
			throw new ParameterException("Output format %s cannot be printed to the console".formatted(format));
		}
		boolean isGraphical = format.isGraphical();
		if (!isGraphical && Boolean.TRUE.equals(transparent)) {
			throw new ParameterException("Output format %s cannot have a transparent background".formatted(format));
		}
		boolean canEmbedFonts = format == OutputFormat.PDF ||
				(format == OutputFormat.SVG && (theme == null || theme.supportsEmbedFonts()));
		if (!canEmbedFonts && Boolean.TRUE.equals(embedFonts)) {
			throw new ParameterException("The selected output configuration cannot embed fonts.");
		}
		boolean canHaveSolidBackground = theme == null || theme.supportsSolidBackground();
		if (!canHaveSolidBackground && Boolean.FALSE.equals(transparent)) {
			throw new ParameterException("The selected output configuration cannot have a solid background.");
		}
		if (!isGraphical && theme != null) {
			throw new ParameterException("Output format %s cannot set an output theme.".formatted(format));
		}
		if (format != OutputFormat.SVG && theme != null && !theme.isStatic()) {
			throw new ParameterException("Output format %s cannot set an automatic output theme.".formatted(format));
		}
		if (format != OutputFormat.PNG && scale != null) {
			throw new ParameterException("Output format %s cannot set a scale factor.".formatted(format));
		}
	}

	public boolean isValidOutputFormat(OutputFormat outputFormat) {
		return outputOptions.isValidOutputFormat(outputFormat);
	}

	public static final class Refinery extends OutputOptionsValidator {
		public Refinery() {
			super(new OutputOptions.Refinery());
		}
	}

	public static final class Json extends OutputOptionsValidator {
		public Json() {
			super(new OutputOptions.Json());
		}
	}

	public static final class Png extends OutputOptionsValidator {
		public Png() {
			super(new OutputOptions.Png());
		}
	}
}
