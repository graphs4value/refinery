package tools.refinery.language.web.xtext;

import java.util.Map;
import java.util.Set;

import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.ISession;
import org.eclipse.xtext.web.server.InvalidRequestException;

import com.google.common.collect.ImmutableMap;

record SimpleServiceContext(ISession session, Map<String, String> parameters) implements IServiceContext {

	public static final String RESOURCE_NAME_PARAMETER = "resource";

	public static final String CONTENT_TYPE_PARAMETER = "contentType";

	public static final String STATE_ID_PARAMETER = "requiredStateId";

	public SimpleServiceContext(ISession session, XtextWebSocketRequest request, String stateId, int index) {
		this(session, addPerTransactionData(request, stateId, request.getRequestData().get(index)));
	}

	@Override
	public Set<String> getParameterKeys() {
		return parameters.keySet();
	}

	@Override
	public String getParameter(String key) {
		return parameters.get(key);
	}

	@Override
	public ISession getSession() {
		return session;
	}

	private static Map<String, String> addPerTransactionData(XtextWebSocketRequest request, String stateId,
			Map<String, String> parameters) {
		checkParameters(parameters, RESOURCE_NAME_PARAMETER);
		checkParameters(parameters, CONTENT_TYPE_PARAMETER);
		checkParameters(parameters, STATE_ID_PARAMETER);
		var builder = ImmutableMap.<String, String>builder();
		builder.putAll(parameters);
		if (request.getResourceName() != null) {
			builder.put(RESOURCE_NAME_PARAMETER, request.getResourceName());
		}
		if (request.getContentType() != null) {
			builder.put(CONTENT_TYPE_PARAMETER, request.getContentType());
		}
		if (stateId != null) {
			builder.put(STATE_ID_PARAMETER, stateId);
		}
		return builder.build();
	}

	private static void checkParameters(Map<String, String> parameters, String perTransactionParameter) {
		if (parameters.containsKey(perTransactionParameter)) {
			throw new InvalidRequestException.InvalidParametersException(
					"Parameters map must not contain '" + perTransactionParameter + "' parameter.");
		}
	}
}
