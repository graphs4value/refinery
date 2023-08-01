package tools.refinery.visualization.internal;

import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.ModelVisualizerStoreAdapter;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class ModelVisualizerAdapterImpl implements ModelVisualizerAdapter {
	private final Model model;
	private final ModelVisualizerStoreAdapter storeAdapter;
	private final Map<AnySymbol, Interpretation<?>> interpretations;
	private final StringBuilder designSpaceBuilder = new StringBuilder();
	private int transitionCounter = 0;

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
		designSpaceBuilder.append("digraph designSpace {\n");
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

	@Override
	public void addTransition(Long from, Long to, String action) {
		designSpaceBuilder.append(from).append(" -> ").append(to).append(" [label=\"").append(transitionCounter++)
				.append(": ").append(action).append("\"]\n");

	}

	@Override
	public void addTransition(Long from, Long to, String action, Tuple activation) {
		designSpaceBuilder.append(from).append(" -> ").append(to).append(" [label=\"").append(transitionCounter++)
				.append(": ").append(action).append(" / ");


		for (int i = 0; i < activation.getSize(); i++) {
			designSpaceBuilder.append(activation.get(i));
			if (i < activation.getSize() - 1) {
				designSpaceBuilder.append(", ");
			}
		}
		designSpaceBuilder.append("\"]\n");
	}

	@Override
	public void addSolution(Long state) {
		designSpaceBuilder.append(state).append(" [shape = doublecircle]\n");
	}

	private String buildDesignSpaceDot() {
		for (var state : storeAdapter.getStore().getStates()) {
			designSpaceBuilder.append(state).append(" [URL=\"./").append(state).append(".svg\"]\n");
		}
		designSpaceBuilder.append("}");
		return designSpaceBuilder.toString();
	}

	@Override
	public boolean saveDesignSpace(String path) {
		saveDot(buildDesignSpaceDot(), path + "/designSpace.dot");
		for (var state : storeAdapter.getStore().getStates()) {
			saveDot(createDotForModelState(state), path + "/" + state + ".dot");
		}
		return true;
	}

	@Override
	public boolean renderDesignSpace(String path) {
		return renderDesignSpace(path, FileFormat.SVG);
	}

	@Override
	public boolean renderDesignSpace(String path, FileFormat format) {
		for (var state : storeAdapter.getStore().getStates()) {
			var stateDot = createDotForModelState(state);
			saveDot(stateDot, path + "/" + state + ".dot");
			renderDot(stateDot, path + "/" + state + "." + format.getFormat());
		}
		var designSpaceDot = buildDesignSpaceDot();
		saveDot(designSpaceDot, path + "/designSpace.dot");
		return renderDot(designSpaceDot, format, path + "/designSpace." + format.getFormat());
	}
}
