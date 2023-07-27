package tools.refinery.visualization.internal;

import guru.nidi.graphviz.attribute.GraphAttr;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.ModelVisualizerStoreAdapter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static guru.nidi.graphviz.model.Factory.*;

public class ModelVisualizerAdapterImpl implements ModelVisualizerAdapter {
	private final Model model;
	private final ModelVisualizerStoreAdapter storeAdapter;
	private final Map<AnySymbol, Interpretation<?>> interpretations;
	public ModelVisualizerAdapterImpl(Model model, ModelVisualizerStoreAdapter storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		this.interpretations = new HashMap<>();
		for (var symbol : storeAdapter.getStore().getSymbols()) {
			var arity = symbol.arity();
			if (arity < 1 || arity > 2) {
				continue;
			}
			var interpretation = model.getInterpretation(symbol);
			var valueType = symbol.valueType();
			Interpretation<?> castInterpretation;
			if (valueType == Boolean.class) {
				castInterpretation = (Interpretation<Boolean>) interpretation;
			}
			// TODO: support TruthValue
//			else if (valueType == TruthValue.class) {
//				castInterpretation = (Interpretation<TruthValue>) interpretation;
//			}
			else {
				continue;
			}
			interpretations.put(symbol, castInterpretation);
		}
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public ModelVisualizerStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}

	@Override
	public MutableGraph createVisualizationForCurrentModelState() {
		var graph = mutGraph("model").setDirected(true).graphAttrs().add(GraphAttr.dpi(100));
		for (var entry : interpretations.entrySet()) {
			var key = entry.getKey();
			var arity = key.arity();
			var cursor = entry.getValue().getAll();
			while (cursor.move()) {
				if (arity == 1) {
					var id = cursor.getKey().get(0);
					graph.add(mutNode(String.valueOf(id)).add("label", key.name() + ": " + id));
				} else {
					var from = cursor.getKey().get(0);
					var to = cursor.getKey().get(1);
					graph.add(mutNode(String.valueOf(from)).addLink(to(mutNode(String.valueOf(to))).with(Label.of(key.name()))));
				}
			}
		}
		return graph;
	}

	@Override
	public MutableGraph createVisualizationForModelState(Long version) {
		var currentVersion = model.getState();
		model.restore(version);
		var graph = createVisualizationForCurrentModelState();
		model.restore(currentVersion);
		return graph;
	}

	@Override
	public String createDotForCurrentModelState() {
		var sb = new StringBuilder();
		sb.append("digraph model {\n");
		for (var entry : interpretations.entrySet()) {
			var key = entry.getKey();
			var arity = key.arity();
			var cursor = entry.getValue().getAll();
			while (cursor.move()) {
				if (arity == 1) {
					var id = cursor.getKey().get(0);
					sb.append("\t").append(id).append(" [label=\"").append(key.name()).append(": ").append(id)
							.append("\"]\n");
				} else {
					var from = cursor.getKey().get(0);
					var to = cursor.getKey().get(1);
					sb.append("\t").append(from).append(" -> ").append(to).append(" [label=\"").append(key.name())
							.append("\"]\n");
				}
			}
		}
		sb.append("}");
		return sb.toString();
	}

	@Override
	public String createDotForModelState(Long version) {
		var currentVersion = model.getState();
		model.restore(version);
		var graph = createDotForCurrentModelState();
		model.restore(currentVersion);
		return graph;
	}

	@Override
	public boolean saveVisualization(MutableGraph graph, String path) {
		return saveVisualization(graph, Format.PNG, path);
	}

	@Override
	public boolean saveVisualization(MutableGraph graph, Format format, String path) {
		try {
			Graphviz.fromGraph(graph).render(format).toFile(new File(path));
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean saveDot(String dot, String filePath) {
		File file = new File(filePath);
		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(dot);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean renderDot(String dot, String filePath) {
		return renderDot(dot, FileFormat.SVG, filePath);
	}

	@Override
	public boolean renderDot(String dot, FileFormat format, String filePath) {
		try {
			Process process = new ProcessBuilder("dot", "-T" + format.getFormat(), "-o", filePath).start();

			OutputStream osToProcess = process.getOutputStream();
			PrintWriter pwToProcess = new PrintWriter(osToProcess);
			pwToProcess.write(dot);
			pwToProcess.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
