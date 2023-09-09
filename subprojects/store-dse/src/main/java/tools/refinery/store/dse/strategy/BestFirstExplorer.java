/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

import tools.refinery.store.model.Model;

import java.util.Random;

public class BestFirstExplorer extends BestFirstWorker {
	final int id;
	Random random;

	public BestFirstExplorer(BestFirstStoreManager storeManager, Model model, int id) {
		super(storeManager, model);
		this.id = id;
		this.random = new Random(id);
	}

	private boolean interrupted = false;

	public void interrupt() {
		this.interrupted = true;
	}

	private boolean shouldRun() {
		return !interrupted && !hasEnoughSolution();
	}

	public void explore() {
		var lastBest = submit().newVersion();
		while (shouldRun()) {
			if (lastBest == null) {
				if (random.nextInt(10) == 0) {
					lastBest = restoreToRandom(random);
				} else {
					lastBest = restoreToBest();
				}
				if (lastBest == null) {
					return;
				}
			}
			boolean tryActivation = true;
			while (tryActivation && shouldRun()) {
				var randomVisitResult = this.visitRandomUnvisited(random);
				tryActivation = randomVisitResult.shouldRetry();
				var newSubmit = randomVisitResult.submitResult();
				if (newSubmit != null) {
					if (!newSubmit.include()) {
						restoreToLast();
					} else {
						var newVisit = newSubmit.newVersion();
						int compareResult = compare(lastBest, newVisit);
						if (compareResult >= 0)  {
							lastBest = newVisit;
						} else {
							lastBest = null;
						}
						break;
					}
				} else {
					lastBest = null;
					break;
				}
			}
		}
	}
}
