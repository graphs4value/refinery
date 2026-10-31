/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package tools.refinery.generator.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameters;
import tools.refinery.generator.cli.commands.OutputFormatCommand;
import tools.refinery.generator.cli.jcommander.FilteredUsageFormatter;
import tools.refinery.generator.cli.output.OutputFormat;
import tools.refinery.generator.cli.output.OutputOptionsValidator;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

class RefineryUsageFormatter extends FilteredUsageFormatter {
	private static final Set<String> OPTIONS_TO_HIDE = Set.of("-transparent", "-embed-fonts", "-theme", "-scale");

	private final boolean showGraphicalOutput;
	private final OutputOptionsValidator outputOptionsValidator;

	public RefineryUsageFormatter(JCommander commander, boolean showGraphicalOutput) {
		super(commander);
		this.showGraphicalOutput = showGraphicalOutput;
		outputOptionsValidator = getOutputOptionsValidator(commander);
	}

	private OutputOptionsValidator getOutputOptionsValidator(JCommander commander) {
		for (var object : commander.getObjects()) {
			if (!(object instanceof OutputFormatCommand outputFormatCommand)) {
				continue;
			}
			var optionsClass = outputFormatCommand.getOutputOptions().getClass();
			var validatorClass = Arrays.stream(optionsClass.getAnnotationsByType(Parameters.class))
					.flatMap(annotation -> Arrays.stream(annotation.parametersValidators()))
					.filter(OutputOptionsValidator.class::isAssignableFrom)
					.findFirst()
					.orElse(null);
			if (validatorClass != null) {
				try {
					return (OutputOptionsValidator) validatorClass.getConstructor().newInstance();
				} catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
				         InvocationTargetException e) {
					throw new RuntimeException("Failed to find output validator", e);
				}
			}
		}
		return new OutputOptionsValidator.Refinery();
	}

	@Override
	protected boolean isHidden(ParameterDescription parameterDescription) {
		return !showGraphicalOutput && Arrays.stream(parameterDescription.getParameter().names())
				.anyMatch(OPTIONS_TO_HIDE::contains);
	}

	@Override
	protected <E extends Enum<E>> EnumSet<E> getPossibleValues(Class<E> type) {
		if (OutputFormat.class.equals(type)) {
			// We just checked that this cast is valid.
			@SuppressWarnings("unchecked")
			var result = (EnumSet<E>) getPossibleOutputFormatValues();
			return result;
		}
		return super.getPossibleValues(type);
	}

	private EnumSet<OutputFormat> getPossibleOutputFormatValues() {
		return EnumSet.copyOf(Arrays.stream(OutputFormat.values())
				.filter(this::isOutputFormatSupported)
				.toList());
	}

	private boolean isOutputFormatSupported(OutputFormat format) {
		return (showGraphicalOutput || !format.isGraphical()) && outputOptionsValidator.isValidOutputFormat(format);
	}
}
