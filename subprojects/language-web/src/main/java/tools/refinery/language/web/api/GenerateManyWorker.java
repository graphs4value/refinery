/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.api;

import org.jetbrains.annotations.Nullable;
import tools.refinery.generator.ModelGenerator;
import tools.refinery.language.web.api.dto.*;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.SolutionStoreListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GenerateManyWorker extends AbstractGenerateWorker<GenerateManyRequest> {
	@Override
	protected void runModelGenerator(ModelGenerator generator) {
		try (var listener = new GenerateManySolutionListener(generator, this)) {
			listener.startNextModel();
			var stopReason = generator.tryGenerate();
			setResponse(new RefineryResponse.Success(new GenerateManySuccessResult(stopReason, listener.getModels())));
		}
	}

	private static class GenerateManySolutionListener implements SolutionStoreListener, AutoCloseable {
		private final ModelGenerator generator;
		private final GenerateManyWorker worker;
		private final @Nullable List<GenerateSuccessResult> models;
		private int i = 0;

		private GenerateManySolutionListener(ModelGenerator generator, GenerateManyWorker worker) {
			this.generator = generator;
			this.worker = worker;
			if (worker.hasStatusReporting()) {
				models = null;
			} else {
				models = new ArrayList<>(worker.getRequest().getCount());
			}
			generator.addListener(this);
		}

		public @Nullable List<GenerateSuccessResult> getModels() {
			return models;
		}

		public void startNextModel() {
			i++;
			if (!generator.hasEnoughSolutions()) {
				worker.updateStatusString("Generating model %s/%s".formatted(i, generator.getMaxNumberOfSolutions()));
			}
		}

		private void startSave() {
			worker.updateStatusString(
					"Saving generated model %s/%s".formatted(i, generator.getMaxNumberOfSolutions()));
		}

		@Override
		public void solutionAdded(VersionWithObjectiveValue version) {
			generator.getModel().restore(version.version());
			GenerateSuccessResult model;
			startSave();
			try {
				model = worker.saveModel(generator);
			} catch (IOException e) {
				throw new RuntimeException(
						"Failed to save model %s/%s".formatted(i, generator.getMaxNumberOfSolutions()), e);
			}
			if (models == null) {
				worker.updateStatus(new GenerateManyStatus(model));
			} else {
				models.add(model);
			}
			startNextModel();
		}

		@Override
		public void close() {
			generator.removeListener(this);
		}
	}
}
