/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.ModelVisualizerStoreAdapter;
import tools.refinery.visualization.statespace.VisualizationStore;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ModelVisualizerAdapterImpl implements ModelVisualizerAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(ModelVisualizerAdapterImpl.class);

	private final Model model;
	private final ModelVisualizerStoreAdapterImpl storeAdapter;
	private final Map<AnySymbol, Interpretation<?>> allInterpretations;
	private final StringBuilder designSpaceBuilder = new StringBuilder();
	private final Map<Version, Integer> states = new HashMap<>();
	private final String outputPath;
	private final Set<FileFormat> formats;
	private final boolean renderDesignSpace;
	private final boolean renderStates;

	private static final Map<Object, String> truthValueToDot = Map.of(
			TruthValue.TRUE, "1",
			TruthValue.FALSE, "0",
			TruthValue.UNKNOWN, "½",
			TruthValue.ERROR, "E",
			true, "1",
			false, "0"
	);

	public ModelVisualizerAdapterImpl(Model model, ModelVisualizerStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		this.outputPath = storeAdapter.getOutputPath();
		this.formats = storeAdapter.getFormats();
		if (formats.isEmpty()) {
			formats.add(FileFormat.SVG);
		}
		this.renderDesignSpace = storeAdapter.isRenderDesignSpace();
		this.renderStates = storeAdapter.isRenderStates();

		this.allInterpretations = new HashMap<>();
		for (var symbol : storeAdapter.getStore().getSymbols()) {
			var arity = symbol.arity();
			if (arity < 1 || arity > 2) {
				continue;
			}
			var interpretation = (Interpretation<?>) model.getInterpretation(symbol);
			allInterpretations.put(symbol, interpretation);
		}
		designSpaceBuilder.append("digraph designSpace {\n");
		designSpaceBuilder.append("""
				nodesep=0
				ranksep=5
				node[
				\tstyle=filled
				\tfillcolor=white
				]
				""");
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public ModelVisualizerStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}

	private String createDotForCurrentModelState() {

		var unaryTupleToInterpretationsMap = new HashMap<Tuple, LinkedHashSet<Interpretation<?>>>();

		var sb = new StringBuilder();

		sb.append("digraph model {\n");
		sb.append("""
				node [
				\tstyle="filled, rounded"
				\tshape=plain
				\tpencolor="#00000088"
				\tfontname="Helvetica"
				]
				""");
		sb.append("""
				edge [
				\tlabeldistance=3
				\tfontname="Helvetica"
				]
				""");

		for (var entry : allInterpretations.entrySet()) {
			var key = entry.getKey();
			var arity = key.arity();
			var cursor = entry.getValue().getAll();
			if (arity == 1) {
				while (cursor.move()) {
					unaryTupleToInterpretationsMap.computeIfAbsent(cursor.getKey(), k -> new LinkedHashSet<>())
							.add(entry.getValue());
				}
			} else if (arity == 2) {
				while (cursor.move()) {
					var tuple = cursor.getKey();
					for (var i = 0; i < tuple.getSize(); i++) {
						var id = tuple.get(i);
						unaryTupleToInterpretationsMap.computeIfAbsent(Tuple.of(id), k -> new LinkedHashSet<>());
					}
					sb.append(drawEdge(cursor.getKey(), key, entry.getValue()));
				}
			}
		}
		for (var entry : unaryTupleToInterpretationsMap.entrySet()) {
			sb.append(drawElement(entry));
		}
		sb.append("}");
		return sb.toString();
	}

	private StringBuilder drawElement(Map.Entry<Tuple, LinkedHashSet<Interpretation<?>>> entry) {
		var sb = new StringBuilder();

		var tableStyle =  " CELLSPACING=\"0\" BORDER=\"2\" CELLBORDER=\"0\" CELLPADDING=\"4\" STYLE=\"ROUNDED\"";

		var key = entry.getKey();
		var id = key.get(0);
		var mainLabel = String.valueOf(id);
		var interpretations = entry.getValue();
		var backgroundColor = toBackgroundColorString(averageColor(interpretations));

		sb.append(id);
		sb.append(" [\n");
		sb.append("\tfillcolor=\"").append(backgroundColor).append("\"\n");
		sb.append("\tlabel=");
		if (interpretations.isEmpty()) {
			sb.append("<<TABLE").append(tableStyle).append(">\n\t<TR><TD>").append(mainLabel).append("</TD></TR>");
		}
		else {
			sb.append("<<TABLE").append(tableStyle).append(">\n\t\t<TR><TD COLSPAN=\"3\" BORDER=\"2\" SIDES=\"B\">")
					.append(mainLabel).append("</TD></TR>\n");
			for (var interpretation : interpretations) {
				var rawValue = interpretation.get(key);

				if (rawValue == null || rawValue.equals(TruthValue.FALSE) || rawValue.equals(false)) {
					continue;
				}
				var color = "black";
				if (rawValue.equals(TruthValue.ERROR)) {
					color = "red";
				}
				var value = truthValueToDot.getOrDefault(rawValue, rawValue.toString());
				var symbol = interpretation.getSymbol();

				if (symbol.valueType() == String.class) {
					value = "\"" + value + "\"";
				}
				sb.append("\t\t<TR><TD><FONT COLOR=\"").append(color).append("\">")
						.append(interpretation.getSymbol().name())
						.append("</FONT></TD><TD><FONT COLOR=\"").append(color).append("\">")
						.append("=</FONT></TD><TD><FONT COLOR=\"").append(color).append("\">").append(value)
						.append("</FONT></TD></TR>\n");
			}
		}
		sb.append("\t\t</TABLE>>\n");
		sb.append("]\n");

		return sb;
	}

	private String drawEdge(Tuple edge, AnySymbol symbol, Interpretation<?> interpretation) {
		var value = interpretation.get(edge);

		if (value == null || value.equals(TruthValue.FALSE) || value.equals(false)) {
			return "";
		}

		var sb = new StringBuilder();
		var style = "solid";
		var color = "black";
		if (value.equals(TruthValue.UNKNOWN)) {
			style = "dotted";
		}
		else if (value.equals(TruthValue.ERROR)) {
			style = "dashed";
			color = "red";
		}

		var from = edge.get(0);
		var to = edge.get(1);
		var name = symbol.name();
		sb.append(from).append(" -> ").append(to)
				.append(" [\n\tstyle=").append(style)
				.append("\n\tcolor=").append(color)
				.append("\n\tfontcolor=").append(color)
				.append("\n\tlabel=\"").append(name)
				.append("\"]\n");
		return sb.toString();
	}

	private String toBackgroundColorString(Integer[] backgroundColor) {
		if (backgroundColor.length == 3)
			return String.format("#%02x%02x%02x", backgroundColor[0], backgroundColor[1], backgroundColor[2]);
		else if (backgroundColor.length == 4)
			return String.format("#%02x%02x%02x%02x", backgroundColor[0], backgroundColor[1], backgroundColor[2],
					backgroundColor[3]);
		return null;
	}

	private Integer[] typeColor(String name) {
		@SuppressWarnings("squid:S2245")
		var random = new Random(name.hashCode());
		return new Integer[] { random.nextInt(128) + 128, random.nextInt(128) + 128, random.nextInt(128) + 128 };
	}

	private Integer[] averageColor(Set<Interpretation<?>> interpretations) {
		if(interpretations.isEmpty()) {
			return new Integer[]{256, 256, 256};
		}
		// TODO: Only use interpretations where the value is not false (or unknown)
		var symbols = interpretations.stream()
				.map(i -> typeColor(i.getSymbol().name())).toArray(Integer[][]::new);



		return new Integer[] {
				Arrays.stream(symbols).map(i -> i[0]).collect(Collectors.averagingInt(Integer::intValue)).intValue(),
				Arrays.stream(symbols).map(i -> i[1]).collect(Collectors.averagingInt(Integer::intValue)).intValue(),
				Arrays.stream(symbols).map(i -> i[2]).collect(Collectors.averagingInt(Integer::intValue)).intValue()
		};
	}

	private String createDotForModelState(Version version) {
		var currentVersion = model.getState();
		model.restore(version);
		var graph = createDotForCurrentModelState();
		model.restore(currentVersion);
		return graph;
	}

	private boolean saveDot(String dot, String filePath) {
		File file = new File(filePath);
		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(dot);
		} catch (IOException e) {
			LOG.error("Failed to write dot file", e);
			return false;
		}
		return true;
	}

	private boolean renderDot(String dot, String filePath) {
		return renderDot(dot, FileFormat.SVG, filePath);
	}

	private boolean renderDot(String dot, FileFormat format, String filePath) {
		try {
			Process process = new ProcessBuilder(storeAdapter.getDotBinaryPath(), "-T" + format.getFormat(),
					"-o", filePath).start();

			OutputStream osToProcess = process.getOutputStream();
			PrintWriter pwToProcess = new PrintWriter(osToProcess);
			pwToProcess.write(dot);
			pwToProcess.close();
		} catch (IOException e) {
			LOG.error("Failed to render dot", e);
			return false;
		}
		return true;
	}

	private String buildDesignSpaceDot() {
		designSpaceBuilder.append("}");
		return designSpaceBuilder.toString();
	}

	private boolean saveDesignSpace(String path) {
		saveDot(buildDesignSpaceDot(), path + "/designSpace.dot");
		for (var entry : states.entrySet()) {
			saveDot(createDotForModelState(entry.getKey()), path + "/" + entry.getValue() + ".dot");
		}
		return true;
	}

	private void renderDesignSpace(String path, Set<FileFormat> formats) {
		File filePath = new File(path);
		filePath.mkdirs();
		if (renderStates) {
			for (var entry : states.entrySet()) {
				var stateId = entry.getValue();
				var stateDot = createDotForModelState(entry.getKey());
				for (var format : formats) {
					if (format == FileFormat.DOT) {
						saveDot(stateDot, path + "/" + stateId + ".dot");
					}
					else {
						renderDot(stateDot, format, path + "/" + stateId + "." + format.getFormat());
					}
				}
			}
		}
		if (renderDesignSpace) {
			var designSpaceDot = buildDesignSpaceDot();
			for (var format : formats) {
				if (format == FileFormat.DOT) {
					saveDot(designSpaceDot, path + "/designSpace.dot");
				}
				else {
					renderDot(designSpaceDot, format, path + "/designSpace." + format.getFormat());
				}
			}
		}
	}

	@Override
	public void visualize(VisualizationStore visualizationStore) {
		this.designSpaceBuilder.append(visualizationStore.getDesignSpaceStringBuilder());
		this.states.putAll(visualizationStore.getStates());
		renderDesignSpace(outputPath, formats);
	}
}
