package tools.refinery.language.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.exceptions.UpgradeException;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.xtext.testing.GlobalRegistries;
import org.eclipse.xtext.testing.GlobalRegistries.GlobalStateMemento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.refinery.language.web.tests.WebSocketIntegrationTestClient;
import tools.refinery.language.web.xtext.servlet.XtextStatusCode;
import tools.refinery.language.web.xtext.servlet.XtextWebSocketServlet;

class ProblemWebSocketServletIntegrationTest {
	private static int SERVER_PORT = 28080;

	private static String SERVLET_URI = "/xtext-service";

	private GlobalStateMemento stateBeforeInjectorCreation;

	private Server server;

	private WebSocketClient client;

	@BeforeEach
	void beforeEach() throws Exception {
		stateBeforeInjectorCreation = GlobalRegistries.makeCopyOfGlobalState();
		client = new WebSocketClient();
		client.start();
	}

	@AfterEach
	void afterEach() throws Exception {
		client.stop();
		client = null;
		if (server != null) {
			server.stop();
			server = null;
		}
		stateBeforeInjectorCreation.restoreGlobalState();
		stateBeforeInjectorCreation = null;
	}

	@Test
	void updateTest() {
		startServer(null);
		var clientSocket = new UpdateTestClient();
		var session = connect(clientSocket, null, XtextWebSocketServlet.XTEXT_SUBPROTOCOL_V1);
		assertThat(session.getUpgradeResponse().getAcceptedSubProtocol(),
				equalTo(XtextWebSocketServlet.XTEXT_SUBPROTOCOL_V1));
		clientSocket.waitForTestResult();
		assertThat(clientSocket.getCloseStatusCode(), equalTo(StatusCode.NORMAL));
		var responses = clientSocket.getResponses();
		assertThat(responses, hasSize(5));
		assertThat(responses.get(0), equalTo("{\"id\":\"foo\",\"response\":{\"stateId\":\"-80000000\"}}"));
		assertThat(responses.get(1), startsWith(
				"{\"resource\":\"test.problem\",\"stateId\":\"-80000000\",\"service\":\"highlight\",\"push\":{\"regions\":["));
		assertThat(responses.get(2), equalTo(
				"{\"resource\":\"test.problem\",\"stateId\":\"-80000000\",\"service\":\"validate\",\"push\":{\"issues\":[]}}"));
		assertThat(responses.get(3), equalTo("{\"id\":\"bar\",\"response\":{\"stateId\":\"-7fffffff\"}}"));
		assertThat(responses.get(4), startsWith(
				"{\"resource\":\"test.problem\",\"stateId\":\"-7fffffff\",\"service\":\"highlight\",\"push\":{\"regions\":["));
	}

	@WebSocket
	public static class UpdateTestClient extends WebSocketIntegrationTestClient {
		@Override
		protected void arrange(Session session, int responsesReceived) throws IOException {
			switch (responsesReceived) {
			case 0 -> session.getRemote().sendString(
					"{\"id\":\"foo\",\"request\":{\"resource\":\"test.problem\",\"serviceType\":\"update\",\"fullText\":\"class Person.\n\"}}");
			case 3 -> session.getRemote().sendString(
					"{\"id\":\"bar\",\"request\":{\"resource\":\"test.problem\",\"serviceType\":\"update\",\"requiredStateId\":\"-80000000\",\"deltaText\":\"individual q.\nnode(q).\n\",\"deltaOffset\":\"0\",\"deltaReplaceLength\":\"0\"}}");
			case 5 -> session.close();
			}
		}
	}

	@Test
	void badSubProtocolTest() {
		startServer(null);
		var clientSocket = new CloseImmediatelyTestClient();
		var session = connect(clientSocket, null, "<invalid sub-protocol>");
		assertThat(session.getUpgradeResponse().getAcceptedSubProtocol(), equalTo(null));
		clientSocket.waitForTestResult();
		assertThat(clientSocket.getCloseStatusCode(), equalTo(StatusCode.NORMAL));
	}

	@WebSocket
	public static class CloseImmediatelyTestClient extends WebSocketIntegrationTestClient {
		@Override
		protected void arrange(Session session, int responsesReceived) throws IOException {
			session.close();
		}
	}

	@Test
	void subProtocolNegotiationTest() {
		startServer(null);
		var clientSocket = new CloseImmediatelyTestClient();
		var session = connect(clientSocket, null, "<invalid sub-protocol>", XtextWebSocketServlet.XTEXT_SUBPROTOCOL_V1);
		assertThat(session.getUpgradeResponse().getAcceptedSubProtocol(),
				equalTo(XtextWebSocketServlet.XTEXT_SUBPROTOCOL_V1));
		clientSocket.waitForTestResult();
		assertThat(clientSocket.getCloseStatusCode(), equalTo(StatusCode.NORMAL));
	}

	@Test
	void invalidJsonTest() {
		startServer(null);
		var clientSocket = new InvalidJsonTestClient();
		connect(clientSocket, null, XtextWebSocketServlet.XTEXT_SUBPROTOCOL_V1);
		clientSocket.waitForTestResult();
		assertThat(clientSocket.getCloseStatusCode(), equalTo(XtextStatusCode.INVALID_JSON));
	}

	@WebSocket
	public static class InvalidJsonTestClient extends WebSocketIntegrationTestClient {
		@Override
		protected void arrange(Session session, int responsesReceived) throws IOException {
			session.getRemote().sendString("<invalid json>");
		}
	}

	@ParameterizedTest(name = "Origin: {0}")
	@ValueSource(strings = { "https://refinery.example", "https://refinery.example:443", "HTTPS://REFINERY.EXAMPLE" })
	void validOriginTest(String origin) {
		startServer("https://refinery.example;https://refinery.example:443");
		var clientSocket = new CloseImmediatelyTestClient();
		connect(clientSocket, origin, XtextWebSocketServlet.XTEXT_SUBPROTOCOL_V1);
		clientSocket.waitForTestResult();
		assertThat(clientSocket.getCloseStatusCode(), equalTo(StatusCode.NORMAL));
	}

	@Test
	void invalidOriginTest() {
		startServer("https://refinery.example;https://refinery.example:443");
		var clientSocket = new CloseImmediatelyTestClient();
		var exception = assertThrows(CompletionException.class,
				() -> connect(clientSocket, "https://invalid.example", XtextWebSocketServlet.XTEXT_SUBPROTOCOL_V1));
		var innerException = exception.getCause();
		assertThat(innerException, instanceOf(UpgradeException.class));
		assertThat(((UpgradeException) innerException).getResponseStatusCode(), equalTo(HttpStatus.FORBIDDEN_403));
	}

	private void startServer(String allowedOrigins) {
		server = new Server(new InetSocketAddress(SERVER_PORT));
		var handler = new ServletContextHandler();
		var holder = new ServletHolder(ProblemWebSocketServlet.class);
		if (allowedOrigins != null) {
			holder.setInitParameter(ProblemWebSocketServlet.ALLOWED_ORIGINS_INIT_PARAM, allowedOrigins);
		}
		handler.addServlet(holder, SERVLET_URI);
		JettyWebSocketServletContainerInitializer.configure(handler, null);
		server.setHandler(handler);
		try {
			server.start();
		} catch (Exception e) {
			throw new RuntimeException("Failed to start websocket server");
		}
	}

	private Session connect(Object webSocketClient, String origin, String... subProtocols) {
		var upgradeRequest = new ClientUpgradeRequest();
		if (origin != null) {
			upgradeRequest.setHeader(HttpHeader.ORIGIN.name(), origin);
		}
		upgradeRequest.setSubProtocols(subProtocols);
		CompletableFuture<Session> sessionFuture;
		try {
			sessionFuture = client.connect(webSocketClient, URI.create("ws://localhost:" + SERVER_PORT + SERVLET_URI),
					upgradeRequest);
		} catch (IOException e) {
			throw new AssertionError("Unexpected exception while connection to websocket", e);
		}
		return sessionFuture.join();
	}
}
