package tools.refinery.language.web;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.eclipse.xtext.testing.GlobalRegistries;
import org.eclipse.xtext.testing.GlobalRegistries.GlobalStateMemento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.refinery.language.web.xtext.servlet.XtextWebSocketServlet;

class ProblemWebSocketServletIntegrationTest {
	private static int SERVER_PORT = 28080;

	private static long TIMEOUT_MILLIS = Duration.ofSeconds(1).toMillis();

	private GlobalStateMemento stateBeforeInjectorCreation;

	private Server server;

	private WebSocketClient client;

	@BeforeEach
	void startServer() throws Exception {
		stateBeforeInjectorCreation = GlobalRegistries.makeCopyOfGlobalState();
		server = new Server(new InetSocketAddress(SERVER_PORT));
		var handler = new ServletContextHandler();
		handler.addServlet(ProblemWebSocketServlet.class, "/xtext-service/*");
		JettyWebSocketServletContainerInitializer.configure(handler, null);
		server.setHandler(handler);
		server.start();
		client = new WebSocketClient();
		client.start();
	}

	@AfterEach
	void stopServer() throws Exception {
		client.stop();
		server.stop();
		stateBeforeInjectorCreation.restoreGlobalState();
	}

	@Test
	void updateTest() throws IOException {
		var clientSocket = new UpdateTestClient();
		var upgradeRequest = new ClientUpgradeRequest();
		upgradeRequest.setSubProtocols(XtextWebSocketServlet.XTEXT_SUBPROTOCOL_V1);
		var sessionFuture = client.connect(clientSocket, URI.create("ws://localhost:" + SERVER_PORT + "/xtext-service"),
				upgradeRequest);
		var session = sessionFuture.join();
		assertThat(session.getUpgradeResponse().getAcceptedSubProtocol(),
				equalTo(XtextWebSocketServlet.XTEXT_SUBPROTOCOL_V1));
		clientSocket.waitForTestResult();
	}

	@WebSocket
	public static class UpdateTestClient {
		private boolean finished = false;

		private Object lock = new Object();

		private Throwable error;

		private int closeStatusCode;

		private String closeReason;

		private List<String> responses = new ArrayList<>();

		@OnWebSocketConnect
		public void onConnect(Session session) {
			try {
				session.getRemote().sendString(
						"{\"id\":\"foo\",\"request\":{\"resource\":\"test.problem\",\"serviceType\":\"update\",\"fullText\":\"class Person.\n\"}}");
			} catch (IOException e) {
				finishedWithError(e);
			}
		}

		@OnWebSocketClose
		public void onClose(int statusCode, String reason) {
			closeStatusCode = statusCode;
			closeReason = reason;
			testFinished();
		}

		@OnWebSocketError
		public void onError(Throwable error) {
			finishedWithError(error);
		}

		@OnWebSocketMessage
		public void onMessage(Session session, String message) {
			try {
				responses.add(message);
				switch (responses.size()) {
				case 3 -> session.getRemote().sendString(
						"{\"id\":\"bar\",\"request\":{\"resource\":\"test.problem\",\"serviceType\":\"update\",\"requiredStateId\":\"-80000000\",\"deltaText\":\"class Car.\n\",\"deltaOffset\":\"0\",\"deltaReplaceLength\":\"0\"}}");
				case 5 -> session.close();
				}
			} catch (IOException e) {
				finishedWithError(e);
			}
		}

		private void finishedWithError(Throwable t) {
			error = t;
			testFinished();
		}

		private void testFinished() {
			synchronized (lock) {
				finished = true;
				lock.notify();
			}
		}

		public void waitForTestResult() {
			synchronized (lock) {
				if (!finished) {
					try {
						lock.wait(TIMEOUT_MILLIS);
					} catch (InterruptedException e) {
						fail("Unexpected InterruptedException", e);
					}
				}
			}
			if (!finished) {
				fail("Test still not finished after timeout");
			}
			if (error != null) {
				fail("Unexpected exception in websocket thread", error);
			}
			if (closeStatusCode != StatusCode.NORMAL) {
				fail("Abnormal close status " + closeStatusCode + ": " + closeReason);
			}
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
	}
}
