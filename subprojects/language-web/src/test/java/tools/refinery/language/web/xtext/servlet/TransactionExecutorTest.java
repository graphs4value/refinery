/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.web.xtext.servlet;

import com.google.inject.Inject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.web.server.model.DocumentStateResult;
import org.eclipse.xtext.web.server.syntaxcoloring.HighlightingResult;
import org.eclipse.xtext.web.server.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.refinery.language.web.semantics.SemanticsService;
import tools.refinery.language.web.tests.AwaitTerminationExecutorServiceProvider;
import tools.refinery.language.web.tests.ProblemWebInjectorProvider;
import tools.refinery.language.web.xtext.server.ResponseHandler;
import tools.refinery.language.web.xtext.server.ResponseHandlerException;
import tools.refinery.language.web.xtext.server.TransactionExecutor;
import tools.refinery.language.web.xtext.server.message.XtextWebOkResponse;
import tools.refinery.language.web.xtext.server.message.XtextWebRequest;
import tools.refinery.language.web.xtext.server.message.XtextWebResponse;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemWebInjectorProvider.class)
class TransactionExecutorTest {
	private static final String RESOURCE_NAME = "test.problem";

	private static final String PROBLEM_CONTENT_TYPE = "application/x-tools.refinery.problem";

	private static final String TEST_PROBLEM = """
			class Person {
				Person[0..*] friend opposite friend
			}

			friend(a, b).
			""";

	private static final Map<String, String> UPDATE_FULL_TEXT_PARAMS = Map.of("resource", RESOURCE_NAME, "serviceType",
			"update", "fullText", TEST_PROBLEM);

	@Inject
	private IResourceServiceProvider.Registry resourceServiceProviderRegistry;

	@Inject
	private AwaitTerminationExecutorServiceProvider executorServices;

	@Inject
	private SemanticsService semanticsService;

	private TransactionExecutor transactionExecutor;

	@BeforeEach
	void beforeEach() {
		transactionExecutor = new TransactionExecutor(new SimpleSession(), resourceServiceProviderRegistry);
		// Manually re-create the semantics analysis thread pool if it was disposed by the previous test.
		semanticsService.setExecutorServiceProvider(executorServices);
	}

	@Test
	void updateFullTextTest() throws ResponseHandlerException {
		var captor = newCaptor();
		var stateId = updateFullText(captor);
		assertThatPrecomputedMessagesAreReceived(stateId, captor.getAllValues());
	}

	@Test
	void updateDeltaTextHighlightAndValidationChange() throws ResponseHandlerException {
		var stateId = updateFullText();
		var responseHandler = sendRequestAndWaitForAllResponses(
				new XtextWebRequest("bar", Map.of("resource", RESOURCE_NAME, "serviceType", "update", "requiredStateId",
						stateId, "deltaText", "individual q.\nnode(q).\n<invalid text>\n", "deltaOffset", "0", "deltaReplaceLength", "0")));

		var captor = newCaptor();
		verify(responseHandler, times(3)).onResponse(captor.capture());
		var newStateId = getStateId("bar", captor.getAllValues().get(0));
		assertThatPrecomputedMessagesAreReceived(newStateId, captor.getAllValues());
	}

	@Test
	void updateDeltaTextHighlightChangeOnly() throws ResponseHandlerException {
		var stateId = updateFullText();
		var responseHandler = sendRequestAndWaitForAllResponses(
				new XtextWebRequest("bar", Map.of("resource", RESOURCE_NAME, "serviceType", "update", "requiredStateId",
						stateId, "deltaText", "atom q.\nnode(q).\n", "deltaOffset", "0", "deltaReplaceLength",
						"0")));

		var captor = newCaptor();
		verify(responseHandler, times(4)).onResponse(captor.capture());
		var newStateId = getStateId("bar", captor.getAllValues().get(0));
		assertHighlightingResponse(newStateId, captor.getAllValues().get(1));
	}

	@Test
	void fullTextWithoutResourceTest() throws ResponseHandlerException {
		var resourceServiceProvider = resourceServiceProviderRegistry
				.getResourceServiceProvider(URI.createFileURI(RESOURCE_NAME));
		resourceServiceProviderRegistry.getContentTypeToFactoryMap().put(PROBLEM_CONTENT_TYPE, resourceServiceProvider);
		var responseHandler = sendRequestAndWaitForAllResponses(new XtextWebRequest("foo",
				Map.of("contentType", PROBLEM_CONTENT_TYPE, "fullText", TEST_PROBLEM, "serviceType", "validate")));

		var captor = newCaptor();
		verify(responseHandler).onResponse(captor.capture());
		var response = captor.getValue();
		assertThat(response, hasProperty("id", equalTo("foo")));
		assertThat(response, hasProperty("responseData", instanceOf(ValidationResult.class)));
	}

	private ArgumentCaptor<XtextWebResponse> newCaptor() {
		return ArgumentCaptor.forClass(XtextWebResponse.class);
	}

	private String updateFullText() throws ResponseHandlerException {
		return updateFullText(newCaptor());
	}

	private String updateFullText(ArgumentCaptor<XtextWebResponse> captor) throws ResponseHandlerException {
		var responseHandler = sendRequestAndWaitForAllResponses(new XtextWebRequest("foo", UPDATE_FULL_TEXT_PARAMS));

		verify(responseHandler, times(4)).onResponse(captor.capture());
		return getStateId("foo", captor.getAllValues().get(0));
	}

	private ResponseHandler sendRequestAndWaitForAllResponses(XtextWebRequest request) throws ResponseHandlerException {
		var responseHandler = mock(ResponseHandler.class);
		transactionExecutor.setResponseHandler(responseHandler);
		transactionExecutor.handleRequest(request);
		executorServices.waitForAllTasksToFinish();
		return responseHandler;
	}

	private String getStateId(String requestId, XtextWebResponse okResponse) {
		assertThat(okResponse, hasProperty("id", equalTo(requestId)));
		assertThat(okResponse, hasProperty("responseData", instanceOf(DocumentStateResult.class)));
		return ((DocumentStateResult) ((XtextWebOkResponse) okResponse).getResponseData()).getStateId();
	}

	private void assertThatPrecomputedMessagesAreReceived(String stateId, List<XtextWebResponse> responses) {
		assertHighlightingResponse(stateId, responses.get(1));
		assertValidationResponse(stateId, responses.get(2));
	}

	private void assertHighlightingResponse(String stateId, XtextWebResponse highlightingResponse) {
		assertThat(highlightingResponse, hasProperty("resourceId", equalTo(RESOURCE_NAME)));
		assertThat(highlightingResponse, hasProperty("stateId", equalTo(stateId)));
		assertThat(highlightingResponse, hasProperty("service", equalTo("highlight")));
		assertThat(highlightingResponse, hasProperty("pushData", instanceOf(HighlightingResult.class)));
	}

	private void assertValidationResponse(String stateId, XtextWebResponse validationResponse) {
		assertThat(validationResponse, hasProperty("resourceId", equalTo(RESOURCE_NAME)));
		assertThat(validationResponse, hasProperty("stateId", equalTo(stateId)));
		assertThat(validationResponse, hasProperty("service", equalTo("validate")));
		assertThat(validationResponse, hasProperty("pushData", instanceOf(ValidationResult.class)));
	}
}
