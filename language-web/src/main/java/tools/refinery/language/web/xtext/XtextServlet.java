/**
 * Copyright (c) 2015, 2020 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext;

import java.io.IOException;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.web.server.IServiceContext;
import org.eclipse.xtext.web.server.IServiceResult;
import org.eclipse.xtext.web.server.IUnwrappableServiceResult;
import org.eclipse.xtext.web.server.InvalidRequestException;
import org.eclipse.xtext.web.server.XtextServiceDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.inject.Injector;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * An HTTP servlet for publishing the Xtext services. Include this into your web
 * server by creating a subclass that executes the standalone setups of your
 * languages in its {@link #init()} method:
 * 
 * <pre>
 * &#64;WebServlet(name = "Xtext Services", urlPatterns = "/xtext-service/*")
 * class MyXtextServlet extends XtextServlet {
 * 	override init() {
 * 		super.init();
 * 		MyDslWebSetup.doSetup();
 * 	}
 * }
 * </pre>
 * 
 * Use the {@code WebServlet} annotation to register your servlet. The default
 * URL pattern for Xtext services is {@code "/xtext-service/*"}.
 */
public class XtextServlet extends HttpServlet {

	private static final long serialVersionUID = 7784324070547781918L;

	private static final IResourceServiceProvider.Registry SERVICE_PROVIDER_REGISTRY = IResourceServiceProvider.Registry.INSTANCE;

	private static final String ENCODING = "UTF-8";

	private static final String INVALID_REQUEST_MESSAGE = "Invalid request ({}): {}";

	private final transient Logger log = LoggerFactory.getLogger(this.getClass());

	private final transient Gson gson = new Gson();

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			super.service(req, resp);
		} catch (InvalidRequestException.ResourceNotFoundException exception) {
			log.trace(INVALID_REQUEST_MESSAGE, req.getRequestURI(), exception.getMessage());
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, exception.getMessage());
		} catch (InvalidRequestException.InvalidDocumentStateException exception) {
			log.trace(INVALID_REQUEST_MESSAGE, req.getRequestURI(), exception.getMessage());
			resp.sendError(HttpServletResponse.SC_CONFLICT, exception.getMessage());
		} catch (InvalidRequestException.PermissionDeniedException exception) {
			log.trace(INVALID_REQUEST_MESSAGE, req.getRequestURI(), exception.getMessage());
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, exception.getMessage());
		} catch (InvalidRequestException exception) {
			log.trace(INVALID_REQUEST_MESSAGE, req.getRequestURI(), exception.getMessage());
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		XtextServiceDispatcher.ServiceDescriptor service = getService(req);
		if (!service.isHasConflict() && (service.isHasSideEffects() || hasTextInput(service))) {
			super.doGet(req, resp);
		} else {
			doService(service, resp);
		}
	}

	@Override
	protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		XtextServiceDispatcher.ServiceDescriptor service = getService(req);
		String type = service.getContext().getParameter(IServiceContext.SERVICE_TYPE);
		if (!service.isHasConflict() && !Objects.equal(type, "update")) {
			super.doPut(req, resp);
		} else {
			doService(service, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		XtextServiceDispatcher.ServiceDescriptor service = getService(req);
		String type = service.getContext().getParameter(IServiceContext.SERVICE_TYPE);
		if (!service.isHasConflict()
				&& (!service.isHasSideEffects() && !hasTextInput(service) || Objects.equal(type, "update"))) {
			super.doPost(req, resp);
		} else {
			doService(service, resp);
		}
	}

	protected boolean hasTextInput(XtextServiceDispatcher.ServiceDescriptor service) {
		Set<String> parameterKeys = service.getContext().getParameterKeys();
		return parameterKeys.contains("fullText") || parameterKeys.contains("deltaText");
	}

	/**
	 * Retrieve the service metadata for the given request. This involves resolving
	 * the Guice injector for the respective language, querying the
	 * {@link XtextServiceDispatcher}, and checking the permission to invoke the
	 * service.
	 */
	protected XtextServiceDispatcher.ServiceDescriptor getService(HttpServletRequest request) throws IOException {
		HttpServiceContext serviceContext = new HttpServiceContext(request);
		Injector injector = getInjector(serviceContext);
		XtextServiceDispatcher serviceDispatcher = injector.getInstance(XtextServiceDispatcher.class);
		return serviceDispatcher.getService(serviceContext);
	}

	/**
	 * Invoke the service function of the given service descriptor and write its
	 * result to the servlet response in Json format. An exception is made for
	 * {@link IUnwrappableServiceResult}: here the document itself is written into
	 * the response instead of wrapping it into a Json object.
	 */
	protected void doService(XtextServiceDispatcher.ServiceDescriptor service, HttpServletResponse response)
			throws IOException {
		IServiceResult result = service.getService().apply();
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding(ENCODING);
		response.setHeader("Cache-Control", "no-cache");
		if (result instanceof IUnwrappableServiceResult unwrapResult && unwrapResult.getContent() != null) {
			String contentType = null;
			if (unwrapResult.getContentType() != null) {
				contentType = unwrapResult.getContentType();
			} else {
				contentType = "text/plain";
			}
			response.setContentType(contentType);
			response.getWriter().write(unwrapResult.getContent());
		} else {
			response.setContentType("text/x-json");
			gson.toJson(result, response.getWriter());
		}
	}

	/**
	 * Resolve the Guice injector for the language associated with the given
	 * context.
	 */
	protected Injector getInjector(HttpServiceContext serviceContext)
			throws InvalidRequestException.UnknownLanguageException {
		IResourceServiceProvider resourceServiceProvider = null;
		String parameter = serviceContext.getParameter("resource");
		if (parameter == null) {
			parameter = "";
		}
		URI emfURI = URI.createURI(parameter);
		String contentType = serviceContext.getParameter("contentType");
		if (Strings.isNullOrEmpty(contentType)) {
			resourceServiceProvider = SERVICE_PROVIDER_REGISTRY.getResourceServiceProvider(emfURI);
			if (resourceServiceProvider == null) {
				if (emfURI.toString().isEmpty()) {
					throw new InvalidRequestException.UnknownLanguageException(
							"Unable to identify the Xtext language: missing parameter 'resource' or 'contentType'.");
				} else {
					throw new InvalidRequestException.UnknownLanguageException(
							"Unable to identify the Xtext language for resource " + emfURI + ".");
				}
			}
		} else {
			resourceServiceProvider = SERVICE_PROVIDER_REGISTRY.getResourceServiceProvider(emfURI, contentType);
			if (resourceServiceProvider == null) {
				throw new InvalidRequestException.UnknownLanguageException(
						"Unable to identify the Xtext language for contentType " + contentType + ".");
			}
		}
		return resourceServiceProvider.get(Injector.class);
	}
}