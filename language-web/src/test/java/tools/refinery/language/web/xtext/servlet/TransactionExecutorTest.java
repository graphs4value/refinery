package tools.refinery.language.web.xtext.servlet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.web.server.ServiceConflictResult;
import org.eclipse.xtext.web.server.model.DocumentStateResult;
import org.eclipse.xtext.web.server.validation.ValidationResult;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.inject.Inject;

@ExtendWith(MockitoExtension.class)
@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemWebInjectorProvider.class)
class TransactionExecutorTest {
	private static final String RESOURCE_NAME = "test.problem";

	private static final String INVALID_STATE_ID = "<invalid_state>";

	private static final String TEST_PROBLEM = """
				class Person {
				Person friend[0..*] opposite friend
			}

			friend(a, b).
			""";

	private static final Map<String, String> UPDATE_FULL_TEXT_PARAMS = Map.of("serviceType", "update", "fullText",
			TEST_PROBLEM);

	private static final Map<String, String> VALIDATE_PARAMS = Map.of("serviceType", "validate");

	@Inject
	private IResourceServiceProvider.Registry resourceServiceProviderRegistry;

	private TransactionExecutor transactionExecutor;

	@BeforeEach
	void beforeEach() {
		transactionExecutor = new TransactionExecutor(new SimpleSession(), resourceServiceProviderRegistry);
	}
	
	@Test
	void emptyBatchTest() {
		performBatchRequest(null);
	}

	@Test
	void fullTextUpdateTest() {
		var response = performSingleRequest(null, UPDATE_FULL_TEXT_PARAMS);
		assertThat(response, hasResponseData(instanceOf(DocumentStateResult.class)));
	}

	@Test
	void validationAfterFullTextUpdateInSameBatchTest() {
		var response = performBatchRequest(null, UPDATE_FULL_TEXT_PARAMS, VALIDATE_PARAMS).get(1);
		assertThat(response, hasResponseData(instanceOf(ValidationResult.class)));
	}

	@Test
	void validationAfterFullTextUpdateInDifferentBatchTest() {
		var stateId = updateFullText();
		var validateResponse = performSingleRequest(stateId, VALIDATE_PARAMS);
		assertThat(validateResponse, hasResponseData(instanceOf(ValidationResult.class)));
	}

	@Test
	void conflictTest() {
		updateFullText();
		var response = performSingleRequest(INVALID_STATE_ID, VALIDATE_PARAMS);
		assertThat(response, hasResponseData(instanceOf(ServiceConflictResult.class)));
	}

	@Test
	void transactionCancelledDueToConflictTest() {
		updateFullText();
		var response = performBatchRequest(INVALID_STATE_ID, VALIDATE_PARAMS, VALIDATE_PARAMS).get(1);
		assertThat(response, hasErrorKind(equalTo(XtextWebSocketErrorKind.TRANSACTION_CANCELLED)));
	}

	@SafeVarargs
	private List<XtextWebSocketResponse> performBatchRequest(String requiredStateId, Map<String, String>... params) {
		var id = UUID.randomUUID().toString();
		var request = new XtextWebSocketRequest(id, RESOURCE_NAME, null, requiredStateId, List.of(params));
		
		var responseHandler = mock(ResponseHandler.class);
		try {
			transactionExecutor.handleRequest(request, responseHandler);
		} catch (IOException e) {
			fail("Unexpected IOException", e);
		}
		
		var captor = ArgumentCaptor.forClass(XtextWebSocketResponse.class);
		int nParams = params.length;
		try {
			verify(responseHandler, times(nParams)).onResponse(captor.capture());
		} catch (IOException e) {
			throw new RuntimeException("Mockito threw unexcepted exception", e);
		}
		var allResponses = captor.getAllValues();
		for (int i = 0; i < nParams; i++) {
			var response = allResponses.get(i);
			assertThat(response, hasProperty("id", equalTo(id)));
			assertThat(response, hasProperty("index", equalTo(i)));
		}
		return allResponses;
	}

	private XtextWebSocketResponse performSingleRequest(String requiredStateId, Map<String, String> param) {
		return performBatchRequest(requiredStateId, param).get(0);
	}

	private String updateFullText() {
		var updateResponse = (XtextWebSocketOkResponse) performSingleRequest(null, UPDATE_FULL_TEXT_PARAMS);
		var documentStateResult = (DocumentStateResult) updateResponse.getResponseData();
		var stateId = documentStateResult.getStateId();
		if (INVALID_STATE_ID.equals(stateId)) {
			throw new RuntimeException("Service returned unexpected stateId: " + stateId);
		}
		return stateId;
	}

	private static Matcher<XtextWebSocketResponse> hasResponseData(Matcher<?> responseDataMatcher) {
		return hasProperty("responseData", responseDataMatcher);
	}

	private static Matcher<XtextWebSocketResponse> hasErrorKind(
			Matcher<? extends XtextWebSocketErrorKind> errorKindMatcher) {
		return hasProperty("errorKind", errorKindMatcher);
	}
}
