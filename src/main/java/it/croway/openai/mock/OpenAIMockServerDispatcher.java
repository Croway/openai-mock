package it.croway.openai.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.List;

/**
 * Dispatcher that routes incoming requests to the appropriate request handler.
 */
public class OpenAIMockServerDispatcher extends Dispatcher {
    private final RequestHandler requestHandler;

    public OpenAIMockServerDispatcher(List<MockExpectation> expectations, ObjectMapper objectMapper) {
        this.requestHandler = new RequestHandler(expectations, objectMapper);
    }

    @Override
    public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
        return requestHandler.handleRequest(request);
    }
}