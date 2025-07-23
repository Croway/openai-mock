package it.croway.openai.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles incoming requests and matches them to appropriate mock expectations.
 */
public class RequestHandler {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private final List<MockExpectation> expectations;
    private final ResponseBuilder responseBuilder;
    private final ObjectMapper objectMapper;

    public RequestHandler(List<MockExpectation> expectations, ObjectMapper objectMapper) {
        this.expectations = expectations;
        this.objectMapper = objectMapper;
        this.responseBuilder = new ResponseBuilder(objectMapper);
    }

    public MockResponse handleRequest(RecordedRequest request) {
        try {
            String requestBody = request.getBody().readUtf8();
            log.debug("Processing request: {}", requestBody);

            JsonNode rootNode = objectMapper.readTree(requestBody);
            RequestContext context = new RequestContext(rootNode);

            if (context.hasToolRole()) {
                return handleToolSequenceResponse(context);
            } else {
                return handleUserInput(request, requestBody, context);
            }
        } catch (Exception e) {
            log.error("Error processing request", e);
            return responseBuilder.createErrorResponse(500, "Error processing request: " + e.getMessage());
        }
    }

    private MockResponse handleToolSequenceResponse(RequestContext context) throws Exception {
        String originalInput = context.getFirstUserMessage();
        if (originalInput == null) {
            log.warn("Could not find original user input in message history");
            return responseBuilder.createErrorResponse(400, "Original user input not found");
        }

        MockExpectation expectation = findExpectationByInput(originalInput);
        if (expectation == null) {
            log.warn("No matching expectation found for tool sequence with input: {}", originalInput);
            return responseBuilder.createErrorResponse(404, "No matching expectation found for tool sequence");
        }

        expectation.advanceToNextToolStep();

        if (expectation.hasMoreToolSteps()) {
            log.debug("Executing next tool step for expectation: {}", originalInput);
            return createToolCallResponse(expectation);
        } else {
            log.debug("Tool sequence completed for expectation: {}", originalInput);
            return responseBuilder.createFinalToolResponse(context.getMessagesNode(), expectation.getExpectedResponse());
        }
    }

    private MockResponse handleUserInput(RecordedRequest request, String requestBody, RequestContext context) throws Exception {
        String userInput = context.getFirstUserMessage();
        if (userInput == null) {
            log.warn("User message content not found in request");
            throw new IllegalArgumentException("User message content not found in request");
        }

        MockExpectation expectation = findExpectationByInput(userInput);

        expectation.resetToolSequence();

        // Execute request assertion if present
        if (expectation.getRequestAssertion() != null) {
            expectation.getRequestAssertion().accept(requestBody);
        }

        return createResponse(expectation, request, userInput);
    }

    private MockResponse createResponse(MockExpectation expectation, RecordedRequest request, String userInput) throws Exception {
        MockResponseType responseType = expectation.getResponseType();

        switch (responseType) {
            case CUSTOM_FUNCTION:
                log.debug("Using custom response function");
                return expectation.getCustomResponseFunction().apply(request, userInput);

            case TOOL_CALLS:
                return createToolCallResponse(expectation);

            case SIMPLE_TEXT:
            default:
                log.debug("Creating simple text response");
                return responseBuilder.createSimpleTextResponse(expectation.getExpectedResponse());
        }
    }

    private MockResponse createToolCallResponse(MockExpectation expectation) throws Exception {
        ToolExecutionStep currentStep = expectation.getCurrentToolStep();
        return responseBuilder.createToolCallResponse(
                expectation.getExpectedResponse(),
                currentStep.getToolCalls()
        );
    }

    private MockExpectation findExpectationByInput(String input) {
        return expectations.stream()
                .filter(expectation -> expectation.matches(input))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format("No matching mock expectation found for input: %s", input)));
    }
}