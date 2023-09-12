/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.server.push;

import com.google.common.collect.ImmutableList;
import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.model.AbstractCachedService;
import org.eclipse.xtext.web.server.model.DocumentSynchronizer;
import org.eclipse.xtext.web.server.model.XtextWebDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.web.generator.ModelGenerationManager;
import tools.refinery.language.web.xtext.server.ResponseHandlerException;

import java.util.ArrayList;
import java.util.List;

public class PushWebDocument extends XtextWebDocument {
	private static final Logger LOG = LoggerFactory.getLogger(PushWebDocument.class);

	private final List<PrecomputationListener> precomputationListeners = new ArrayList<>();

	private final ModelGenerationManager modelGenerationManager = new ModelGenerationManager();

	private final DocumentSynchronizer synchronizer;

	public PushWebDocument(String resourceId, DocumentSynchronizer synchronizer) {
		super(resourceId, synchronizer);
		this.synchronizer = synchronizer;
	}

	public ModelGenerationManager getModelGenerationManager() {
		return modelGenerationManager;
	}

	public void addPrecomputationListener(PrecomputationListener listener) {
		synchronized (precomputationListeners) {
			if (precomputationListeners.contains(listener)) {
				return;
			}
			precomputationListeners.add(listener);
			listener.onSubscribeToPrecomputationEvents(getResourceId(), this);
		}
	}

	public void removePrecomputationListener(PrecomputationListener listener) {
		synchronized (precomputationListeners) {
			precomputationListeners.remove(listener);
		}
	}

	public <T extends IServiceResult> void precomputeServiceResult(AbstractCachedService<T> service, String serviceName,
			CancelIndicator cancelIndicator, boolean logCacheMiss) {
		var result = getCachedServiceResult(service, cancelIndicator, logCacheMiss);
		if (result != null) {
			notifyPrecomputationListeners(serviceName, result);
		}
	}

	public <T extends IServiceResult> void notifyPrecomputationListeners(String serviceName, T result) {
		var resourceId = getResourceId();
		if (resourceId == null) {
			return;
		}
		var stateId = getStateId();
		List<PrecomputationListener> copyOfListeners;
		synchronized (precomputationListeners) {
			copyOfListeners = ImmutableList.copyOf(precomputationListeners);
		}
		var toRemove = new ArrayList<PrecomputationListener>();
		for (var listener : copyOfListeners) {
			try {
				listener.onPrecomputedServiceResult(resourceId, stateId, serviceName, result);
			} catch (ResponseHandlerException e) {
				LOG.error("Delivering precomputation push message failed", e);
				toRemove.add(listener);
			}
		}
		if (!toRemove.isEmpty()) {
			synchronized (precomputationListeners) {
				precomputationListeners.removeAll(toRemove);
			}
		}
	}

	public void cancelModelGeneration() {
		modelGenerationManager.cancel();
	}

	public void dispose() {
		synchronizer.setCanceled(true);
		modelGenerationManager.dispose();
	}
}
