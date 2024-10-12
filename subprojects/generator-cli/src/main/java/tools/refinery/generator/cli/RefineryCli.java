/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.generator.cli.commands.Command;
import tools.refinery.generator.cli.commands.ConcretizeCommand;
import tools.refinery.generator.cli.commands.GenerateCommand;
import tools.refinery.generator.standalone.StandaloneRefinery;

import java.io.IOException;

public class RefineryCli {
	private static final Logger LOGGER = LoggerFactory.getLogger(RefineryCli.class);

	@Inject
	private ConcretizeCommand concretizeCommand;

	@Inject
	private GenerateCommand generateCommand;

	private JCommander jCommander;

	private JCommander getjCommander() {
		if (jCommander == null) {
			jCommander = JCommander.newBuilder()
					.programName("refinery")
					.addObject(this)
					.addCommand("generate", generateCommand, "g")
					.addCommand("concretize", concretizeCommand, "c")
					.build();
		}
		return jCommander;
	}

	public Command parseArguments(String... args) {
		var jc = getjCommander();
		jc.parse(args);
		var parsedCommand = jc.getParsedCommand();
		if (parsedCommand == null) {
			return null;
		}
		var commandParser = jc.getCommands().get(parsedCommand);
		if (commandParser == null) {
			throw new IllegalStateException("Command parsed but not registered: " + parsedCommand);
		}
		return (Command) commandParser.getObjects().stream()
				.filter(Command.class::isInstance)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Not an executable command: " + parsedCommand));
	}

	public void showUsageAndExit() {
		jCommander.usage();
		System.exit(1);
	}

	public static void main(String[] args) throws IOException {
		var cli = StandaloneRefinery.getInjector().getInstance(RefineryCli.class);
		Command command = null;
		try {
			command = cli.parseArguments(args);
		} catch (ParameterException e) {
			LOGGER.error("Error while parsing arguments", e);
		}
		if (command == null) {
			cli.showUsageAndExit();
		} else {
			try {
				command.run();
			} catch (RuntimeException e) {
				LOGGER.error("Error while executing command", e);
			}
		}
	}
}
