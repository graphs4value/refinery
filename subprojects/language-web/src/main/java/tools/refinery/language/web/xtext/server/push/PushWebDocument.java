package tools.refinery.language.web.xtext.server.push;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.model.AbstractCachedService;
import org.eclipse.xtext.web.server.model.DocumentSynchronizer;
import org.eclipse.xtext.web.server.model.XtextWebDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import tools.refinery.language.web.xtext.server.ResponseHandlerException;

public class PushWebDocument extends XtextWebDocument {
	private static final Logger LOG = LoggerFactory.getLogger(PushWebDocument.class);

	private final List<PrecomputationListener> precomputationListeners = new ArrayList<>();

	private final Map<Class<?>, IServiceResult> precomputedServices = new HashMap<>();

	public PushWebDocument(String resourceId, DocumentSynchronizer synchronizer) {
		super(resourceId, synchronizer);
		if (resourceId == null) {
			throw new IllegalArgumentException("resourceId must not be null");
		}
	}

	public boolean addPrecomputationListener(PrecomputationListener listener) {
		synchronized (precomputationListeners) {
			if (precomputationListeners.contains(listener)) {
				return false;
			}
			precomputationListeners.add(listener);
			listener.onSubscribeToPrecomputationEvents(getResourceId(), this);
			return true;
		}
	}

	public boolean removePrecomputationListener(PrecomputationListener listener) {
		synchronized (precomputationListeners) {
			return precomputationListeners.remove(listener);
		}
	}

	public <T extends IServiceResult> void precomputeServiceResult(AbstractCachedService<T> service, String serviceName,
			CancelIndicator cancelIndicator, boolean logCacheMiss) {
		var result = getCachedServiceResult(service, cancelIndicator, logCacheMiss);
		if (result == null) {
			LOG.error("{} service returned null result", serviceName);
			return;
		}
		var serviceClass = service.getClass();
		var previousResult = precomputedServices.get(serviceClass);
		if (previousResult != null && previousResult.equals(result)) {
			return;
		}
		precomputedServices.put(serviceClass, result);
		notifyPrecomputationListeners(serviceName, result);
	}

	private <T extends IServiceResult> void notifyPrecomputationListeners(String serviceName, T result) {
		var resourceId = getResourceId();
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
}
