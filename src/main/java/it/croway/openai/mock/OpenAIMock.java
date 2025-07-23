package it.croway.openai.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main mock server for OpenAI API testing.
 * Implements JUnit 5 extension lifecycle methods.
 */
public class OpenAIMock implements BeforeEachCallback, AfterEachCallback {
    private static final Logger log = LoggerFactory.getLogger(OpenAIMock.class);

    private MockWebServer server;
    private final List<MockExpectation> expectations;
    private final OpenAIMockBuilder builder;
    private final ObjectMapper objectMapper;

    public OpenAIMock() {
        this.expectations = new ArrayList<>();
        this.objectMapper = new ObjectMapper();
        this.builder = new OpenAIMockBuilder(this, this.expectations);
    }

    public OpenAIMockBuilder builder() {
        return this.builder;
    }

    public String getBaseUrl() {
        if (server == null) {
            throw new IllegalStateException("Mock server not started. Call beforeEach() first.");
        }
        return server.url("/").toString();
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        server = new MockWebServer();
        server.start();
        server.setDispatcher(new OpenAIMockServerDispatcher(expectations, objectMapper));

        log.info("Mock web server started on {}", server.url("/"));
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (server != null) {
            server.shutdown();
            log.info("Mock web server shut down");
        }
    }
}