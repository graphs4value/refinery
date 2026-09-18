/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.output;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.ParametersDelegate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.refinery.generator.cli.utils.CliUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests {@link OutputOptions} and {@link OutputOptionsValidator} as they are used by the CLI, i.e., as a
 * {@link ParametersDelegate} of a command parsed by {@link JCommander}.
 */
class OutputOptionsTest {
	/**
	 * Stands in for a command like {@code GenerateCommand} or {@code RenderCommand}, which expose their output
	 * options as a parameter delegate. Delegation matters, because {@code JCommander} only picks up the
	 * {@code parametersValidators} of {@link OutputOptions} once it has descended into the delegate.
	 *
	 * @param <T> The type of the output options under test.
	 */
	// We use a plain {@code class} to have precise control over the annotations.
	@SuppressWarnings("ClassCanBeRecord")
	private static final class CommandStub<T extends OutputOptions> {
		@ParametersDelegate
		private final T outputOptions;

		public CommandStub(T outputOptions) {
			this.outputOptions = outputOptions;
		}
	}

	private static <T extends OutputOptions> T parse(T outputOptions, String... args) {
		JCommander.newBuilder()
				.programName("test")
				.addObject(new CommandStub<>(outputOptions))
				.build()
				.parse(args);
		return outputOptions;
	}

	private static String parseFailure(OutputOptions outputOptions, String... args) {
		var exception = assertThrows(ParameterException.class, () -> parse(outputOptions, args));
		return exception.getMessage();
	}

	private static OutputOptions newOptions(String command) {
		return switch (command) {
			case "refinery" -> new OutputOptions.Refinery();
			case "json" -> new OutputOptions.Json();
			case "png" -> new OutputOptions.Png();
			default -> throw new IllegalArgumentException("Unknown command: " + command);
		};
	}

	@Nested
	class DefaultsTest {
		@ParameterizedTest
		@CsvSource({
				"refinery, REFINERY",
				"json, JSON",
				"png, PNG"
		})
		void defaultFormatTest(String command, OutputFormat expectedFormat) {
			var options = parse(newOptions(command));
			assertThat(options.getFormat(), is(expectedFormat));
		}

		@ParameterizedTest
		@ValueSource(strings = {"refinery", "json", "png"})
		void defaultOutputPathIsStandardStreamTest(String command) {
			var options = parse(newOptions(command));
			assertThat(options.getOutputPath(), is(CliUtils.STANDARD_OUTPUT_PATH));
			assertThat(options.isStandardStream(), is(true));
		}

		@ParameterizedTest
		@ValueSource(strings = {"refinery", "json", "png"})
		void defaultGraphicalOptionsTest(String command) {
			var options = parse(newOptions(command));
			assertThat(options.isTransparent(), is(true));
			assertThat(options.getTheme(), is(OutputTheme.LIGHT));
			assertThat(options.getScale(), is(1));
		}

		@ParameterizedTest
		@CsvSource({
				"refinery, false",
				"json, false",
				"svg, false",
				"pdf, true",
				"png, false"
		})
		void defaultEmbedFontsTest(String format, boolean expectedEmbedFonts) {
			// Fonts are embedded by default for pdf output only.
			var options = parse(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format);
			assertThat(options.isEmbedFonts(), is(expectedEmbedFonts));
		}

		@Test
		void explicitEmbedFontsOverridesDefaultTest() {
			var options = parse(new OutputOptions.Refinery(), "-o", "output.pdf", "-embed-fonts", "false");
			assertThat(options.getFormat(), is(OutputFormat.PDF));
			assertThat(options.isEmbedFonts(), is(false));
		}
	}

	@Nested
	class FormatSelectionTest {
		@ParameterizedTest
		@EnumSource(OutputFormat.class)
		void explicitFormatTest(OutputFormat format) {
			// The {@code refinery} command supports every output format.
			var options = parse(new OutputOptions.Refinery(), "-o", "output.txt", "-format", format.toString());
			assertThat(options.getFormat(), is(format));
		}

		@ParameterizedTest
		@EnumSource(OutputFormat.class)
		void explicitFormatShortNameTest(OutputFormat format) {
			var options = parse(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format.toString());
			assertThat(options.getFormat(), is(format));
		}

		@Test
		void explicitFormatIsCaseInsensitiveTest() {
			var options = parse(new OutputOptions.Refinery(), "-o", "output.txt", "-f", "SVG");
			assertThat(options.getFormat(), is(OutputFormat.SVG));
		}

		@Test
		void unknownFormatTest() {
			var message = parseFailure(new OutputOptions.Refinery(), "-f", "xml");
			// Invalid value for -f parameter. Allowed values:[refinery, json, svg, pdf, png]
			assertThat(message, allOf(containsString("Invalid value"), containsString("-f")));
		}

		@ParameterizedTest
		@CsvSource({
				"output.refinery, REFINERY",
				"output.problem, REFINERY",
				"output.json, JSON",
				"output.svg, SVG",
				"output.pdf, PDF",
				"output.png, PNG",
				"OUTPUT.PNG, PNG",
				"some.directory/output.svg, SVG"
		})
		void formatFromOutputPathTest(String outputPath, OutputFormat expectedFormat) {
			var options = parse(new OutputOptions.Refinery(), "-o", outputPath);
			assertThat(options.getOutputPath(), is(outputPath));
			assertThat(options.getFormat(), is(expectedFormat));
		}

		@ParameterizedTest
		@ValueSource(strings = {"output.txt", "output", "output.svg.txt", "-"})
		void unrecognizedOutputPathKeepsDefaultFormatTest(String outputPath) {
			var options = parse(new OutputOptions.Refinery(), "-o", outputPath);
			assertThat(options.getFormat(), is(OutputFormat.REFINERY));
		}

		@Test
		void formatFromOutputPathShortNameTest() {
			var options = parse(new OutputOptions.Refinery(), "-output", "output.json");
			assertThat(options.getFormat(), is(OutputFormat.JSON));
		}

		@Test
		void explicitFormatBeforeOutputPathWinsTest() {
			var options = parse(new OutputOptions.Refinery(), "-f", "json", "-o", "output.svg");
			assertThat(options.getFormat(), is(OutputFormat.JSON));
		}

		@Test
		void explicitFormatAfterOutputPathWinsTest() {
			var options = parse(new OutputOptions.Refinery(), "-o", "output.svg", "-f", "json");
			assertThat(options.getFormat(), is(OutputFormat.JSON));
		}

		@ParameterizedTest
		@CsvSource({
				"json, output.refinery, JSON",
				"json, output.problem, JSON",
				"png, output.refinery, PNG",
				"png, output.json, PNG"
		})
		void outputPathWithUnsupportedFormatKeepsDefaultFormatTest(String command, String outputPath,
																   OutputFormat expectedFormat) {
			// Extensions of formats that the command does not support must not be detected.
			var options = parse(newOptions(command), "-o", outputPath);
			assertThat(options.getFormat(), is(expectedFormat));
		}

		@ParameterizedTest
		@CsvSource({
				"json, refinery",
				"png, refinery",
				"png, json"
		})
		void unsupportedExplicitFormatTest(String command, String format) {
			var message = parseFailure(newOptions(command), "-f", format);
			// This command does not support <format> output
			assertThat(message, allOf(containsString("does not support"), containsString(format)));
		}

		@ParameterizedTest
		@CsvSource({
				"json, json",
				"json, svg",
				"json, pdf",
				"json, png",
				"png, svg",
				"png, pdf",
				"png, png"
		})
		void supportedExplicitFormatTest(String command, String format) {
			var options = parse(newOptions(command), "-o", "output.txt", "-f", format);
			assertThat(options.getFormat().toString(), is(format));
		}
	}

	@Nested
	class ParameterConversionTest {
		@ParameterizedTest
		@ValueSource(booleans = {false, true})
		void transparentTest(boolean transparent) {
			var options = parse(new OutputOptions.Png(), "-transparent", Boolean.toString(transparent));
			assertThat(options.isTransparent(), is(transparent));
		}

		@Test
		void transparentRequiresValueTest() {
			var message = parseFailure(new OutputOptions.Png(), "-transparent");
			// Expected a value after parameter -transparent
			assertThat(message, allOf(containsString("Expected a value"), containsString("-transparent")));
		}

		@ParameterizedTest
		@ValueSource(booleans = {false, true})
		void embedFontsTest(boolean embedFonts) {
			var options = parse(new OutputOptions.Refinery(), "-o", "output.pdf", "-embed-fonts",
					Boolean.toString(embedFonts));
			assertThat(options.isEmbedFonts(), is(embedFonts));
		}

		@Test
		void embedFontsRequiresValueTest() {
			var message = parseFailure(new OutputOptions.Refinery(), "-o", "output.pdf", "-embed-fonts");
			// Expected a value after parameter -embed-fonts
			assertThat(message, allOf(containsString("Expected a value"), containsString("-embed-fonts")));
		}

		@ParameterizedTest
		@EnumSource(OutputTheme.class)
		void themeTest(OutputTheme theme) {
			var options = parse(new OutputOptions.Refinery(), "-o", "output.svg", "-theme", theme.toString());
			assertThat(options.getTheme(), is(theme));
		}

		@Test
		void unknownThemeTest() {
			var message = parseFailure(new OutputOptions.Png(), "-theme", "sepia");
			// Invalid value for -theme parameter. Allowed values:[light, dark, auto]
			assertThat(message, allOf(containsString("Invalid value"), containsString("-theme")));
		}

		@ParameterizedTest
		@ValueSource(ints = {1, 2, 3, 4})
		void scaleTest(int scale) {
			var options = parse(new OutputOptions.Png(), "-scale", Integer.toString(scale));
			assertThat(options.getScale(), is(scale));
		}

		@ParameterizedTest
		@ValueSource(ints = {-1, 0, 5, 100})
		void invalidScaleTest(int scale) {
			var message = parseFailure(new OutputOptions.Png(), "-scale", Integer.toString(scale));
			// Scale must be between 1 and 4, inclusive.
			assertThat(message, allOf(containsString("Scale"), containsString("between 1 and 4")));
		}

		@Test
		void nonNumericScaleTest() {
			var message = parseFailure(new OutputOptions.Png(), "-scale", "large");
			// "-scale": couldn't convert "large" to an integer
			assertThat(message, allOf(containsString("-scale"), containsString("large"),
					containsString("integer")));
		}
	}

	@Nested
	class ValidationTest {
		@ParameterizedTest
		@ValueSource(strings = {"refinery", "json"})
		void transparentNotAllowedForTextualFormatTest(String format) {
			var message = parseFailure(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format,
					"-transparent", "true");
			// Output format <format> cannot have a transparent background
			assertThat(message, allOf(containsString(format), containsString("transparent background")));
		}

		@ParameterizedTest
		@ValueSource(strings = {"refinery", "json"})
		void opaqueIsAllowedForTextualFormatTest(String format) {
			// Only an explicit {@code -transparent true} is rejected, because the option defaults to {@code true}.
			var options = parse(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format, "-transparent",
					"false");
			assertThat(options.isTransparent(), is(false));
		}

		@ParameterizedTest
		@ValueSource(strings = {"svg", "pdf", "png"})
		void transparentIsAllowedForGraphicalFormatTest(String format) {
			var options = parse(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format, "-transparent",
					"true");
			assertThat(options.isTransparent(), is(true));
		}

		@ParameterizedTest
		@CsvSource({
				"pdf,",
				"pdf, light",
				"pdf, dark",
				"svg,",
				"svg, light",
				"svg, dark"
		})
		void embedFontsIsAllowedTest(String format, String theme) {
			var options = theme == null ?
					parse(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format, "-embed-fonts", "true") :
					parse(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format, "-theme", theme,
							"-embed-fonts", "true");
			assertThat(options.isEmbedFonts(), is(true));
		}

		@ParameterizedTest
		@ValueSource(strings = {"refinery", "json", "png"})
		void embedFontsNotAllowedForFormatTest(String format) {
			var message = parseFailure(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format,
					"-embed-fonts", "true");
			// The selected output configuration cannot embed fonts.
			assertThat(message, containsString("cannot embed fonts"));
		}

		@Test
		void embedFontsNotAllowedForAutoThemeTest() {
			var message = parseFailure(new OutputOptions.Refinery(), "-o", "output.txt", "-f", "svg", "-theme",
					"auto", "-embed-fonts", "true");
			// The selected output configuration cannot embed fonts.
			assertThat(message, containsString("cannot embed fonts"));
		}

		@Test
		void solidBackgroundNotAllowedForAutoThemeTest() {
			var message = parseFailure(new OutputOptions.Refinery(), "-o", "output.txt", "-f", "svg", "-theme",
					"auto", "-transparent", "false");
			// The selected output configuration cannot have a solid background.
			assertThat(message, containsString("cannot have a solid background"));
		}

		@ParameterizedTest
		@ValueSource(strings = {"refinery", "json", "png"})
		void disablingEmbedFontsIsAlwaysAllowedTest(String format) {
			var options = parse(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format, "-embed-fonts",
					"false");
			assertThat(options.isEmbedFonts(), is(false));
		}

		@ParameterizedTest
		@CsvSource({
				"refinery, light",
				"refinery, dark",
				"refinery, auto",
				"json, light",
				"json, dark",
				"json, auto"
		})
		void themeNotAllowedForTextualFormatTest(String format, String theme) {
			var message = parseFailure(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format, "-theme",
					theme);
			// Output format <format> cannot set an output theme.
			assertThat(message, allOf(containsString(format), containsString("output theme"),
					not(containsString("automatic"))));
		}

		@ParameterizedTest
		@ValueSource(strings = {"pdf", "png"})
		void autoThemeNotAllowedForRasterFormatTest(String format) {
			var message = parseFailure(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format, "-theme",
					"auto");
			// Output format <format> cannot set an automatic output theme.
			assertThat(message, allOf(containsString(format), containsString("automatic"),
					containsString("theme")));
		}

		@ParameterizedTest
		@CsvSource({
				"svg, LIGHT",
				"svg, DARK",
				"svg, AUTO",
				"pdf, LIGHT",
				"pdf, DARK",
				"png, LIGHT",
				"png, DARK"
		})
		void themeIsAllowedForGraphicalFormatTest(String format, OutputTheme theme) {
			var options = parse(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format, "-theme",
					theme.toString());
			assertThat(options.getTheme(), is(theme));
		}

		@ParameterizedTest
		@ValueSource(strings = {"refinery", "json", "svg", "pdf"})
		void scaleNotAllowedForFormatTest(String format) {
			var message = parseFailure(new OutputOptions.Refinery(), "-o", "output.txt", "-f", format, "-scale", "2");
			// Output format <format> cannot set a scale factor.
			assertThat(message, allOf(containsString(format), containsString("scale factor")));
		}

		@Test
		void scaleIsAllowedForPngTest() {
			var options = parse(new OutputOptions.Refinery(), "-o", "output.txt", "-f", "png", "-scale", "2");
			assertThat(options.getScale(), is(2));
		}

		@Test
		void validationUsesFormatFromOutputPathTest() {
			// The format is not passed to the validator explicitly, so it has to be inferred from the output path
			// instead of the {@code refinery} default of the command.
			var message = parseFailure(new OutputOptions.Refinery(), "-o", "output.svg", "-scale", "2");
			// Output format svg cannot set a scale factor.
			assertThat(message, allOf(containsString("svg"), containsString("scale factor"),
					not(containsString("refinery"))));
		}

		@Test
		void validationUsesDefaultFormatTest() {
			var message = parseFailure(new OutputOptions.Refinery(), "-scale", "2");
			// Output format refinery cannot set a scale factor.
			assertThat(message, allOf(containsString("refinery"), containsString("scale factor")));
		}

		@Test
		void validationUsesExplicitFormatInsteadOfOutputPathTest() {
			var message = parseFailure(new OutputOptions.Refinery(), "-o", "output.png", "-f", "pdf", "-scale", "2");
			// Output format pdf cannot set a scale factor.
			assertThat(message, allOf(containsString("pdf"), containsString("scale factor"),
					not(containsString("png"))));
		}

		@Test
		void validationUsesCommandDefaultFormatTest() {
			var options = parse(new OutputOptions.Png(), "-scale", "2");
			assertThat(options.getScale(), is(2));
		}

		@Test
		void fullyConfiguredGraphicalOutputTest() {
			var options = parse(new OutputOptions.Png(), "-o", "output.png", "-transparent", "false", "-theme",
					"dark", "-scale", "3");
			assertThat(options.getFormat(), is(OutputFormat.PNG));
			assertThat(options.getOutputPath(), is("output.png"));
			assertThat(options.isStandardStream(), is(false));
			assertThat(options.isTransparent(), is(false));
			assertThat(options.isEmbedFonts(), is(false));
			assertThat(options.getTheme(), is(OutputTheme.DARK));
			assertThat(options.getScale(), is(3));
		}
	}

	/**
	 * Tests the rule that rejects binary output on a terminal. Since {@link System#console()} is static and
	 * JCommander instantiates the validator reflectively, {@link OutputOptionsValidator#terminalDetector} acts as
	 * an injectable mock. It is global state, so it has to be restored after every test.
	 */
	@Nested
	@Isolated
	class ConsoleOutputTest {
		@AfterEach
		void restoreTerminalDetector() {
			OutputOptionsValidator.terminalDetector = OutputOptionsValidator.DEFAULT_TERMINAL_DETECTOR;
		}

		@ParameterizedTest
		@ValueSource(strings = {"pdf", "png"})
		void binaryFormatOnTerminalTest(String format) {
			OutputOptionsValidator.terminalDetector = () -> true;
			var message = parseFailure(new OutputOptions.Refinery(), "-f", format, "-o", "-");
			// Output format <format> cannot be printed to the console
			assertThat(message, allOf(containsString(format), containsString("printed to the console")));
		}

		@ParameterizedTest
		@ValueSource(strings = {"pdf", "png"})
		void binaryFormatOnTerminalWithDefaultOutputPathTest(String format) {
			// The output path defaults to the standard output even if {@code -output} is never passed, in which case
			// the validator sees a {@code null} output path.
			OutputOptionsValidator.terminalDetector = () -> true;
			var message = parseFailure(new OutputOptions.Refinery(), "-f", format);
			assertThat(message, allOf(containsString(format), containsString("printed to the console")));
		}

		@ParameterizedTest
		@ValueSource(strings = {"refinery", "json", "svg"})
		void textualFormatOnTerminalTest(String format) {
			OutputOptionsValidator.terminalDetector = () -> true;
			var options = parse(new OutputOptions.Refinery(), "-f", format, "-o", "-");
			assertThat(options.isStandardStream(), is(true));
		}

		@ParameterizedTest
		@ValueSource(strings = {"pdf", "png"})
		void binaryFormatToFileOnTerminalTest(String format) {
			OutputOptionsValidator.terminalDetector = () -> true;
			var options = parse(new OutputOptions.Refinery(), "-f", format, "-o", "output.txt");
			assertThat(options.isStandardStream(), is(false));
		}

		@ParameterizedTest
		@ValueSource(strings = {"pdf", "png"})
		void binaryFormatOnRedirectedStandardOutputTest(String format) {
			// Piping binary output into a file or another program is fine.
			OutputOptionsValidator.terminalDetector = () -> false;
			var options = parse(new OutputOptions.Refinery(), "-f", format, "-o", "-");
			assertThat(options.isStandardStream(), is(true));
		}
	}
}
