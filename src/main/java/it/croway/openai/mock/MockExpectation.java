package it.croway.openai.mock;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Represents a mock expectation for a specific user input.
 * Contains the expected input, tool execution sequence, and response configuration.
 */
public class MockExpectation {
    private final String expectedInput;
    private final ToolExecutionSequence toolSequence;
    private String expectedResponse;
    private BiFunction<RecordedRequest, String, MockResponse> customResponseFunction;
    private Consumer<String> requestAssertion;

    public MockExpectation(String expectedInput) {
        this.expectedInput = expectedInput;
        this.toolSequence = new ToolExecutionSequence();
    }

    // Getters
    public String getExpectedInput() {
        return expectedInput;
    }

    public String getExpectedResponse() {
        return expectedResponse;
    }

    public BiFunction<RecordedRequest, String, MockResponse> getCustomResponseFunction() {
        return customResponseFunction;
    }

    public Consumer<String> getRequestAssertion() {
        return requestAssertion;
    }

    public ToolExecutionSequence getToolSequence() {
        return toolSequence;
    }

    // Setters
    public void setExpectedResponse(String expectedResponse) {
        this.expectedResponse = expectedResponse;
    }

    public void setCustomResponseFunction(BiFunction<RecordedRequest, String, MockResponse> customResponseFunction) {
        this.customResponseFunction = customResponseFunction;
    }

    public void setRequestAssertion(Consumer<String> requestAssertion) {
        this.requestAssertion = requestAssertion;
    }

    // Tool sequence delegation methods
    public void addToolExecutionStep(ToolExecutionStep step) {
        toolSequence.addStep(step);
    }

    public ToolExecutionStep getCurrentToolStep() {
        return toolSequence.getCurrentStep();
    }

    public void advanceToNextToolStep() {
        toolSequence.advanceToNextStep();
    }

    public boolean hasMoreToolSteps() {
        return toolSequence.hasMoreSteps();
    }

    public boolean isInToolSequence() {
        return toolSequence.isInProgress();
    }

    public void resetToolSequence() {
        toolSequence.reset();
    }

    // Response type determination
    public MockResponseType getResponseType() {
        if (customResponseFunction != null) {
            return MockResponseType.CUSTOM_FUNCTION;
        }

        if (!toolSequence.isEmpty() && toolSequence.hasCurrentStep()) {
            return MockResponseType.TOOL_CALLS;
        }

        return MockResponseType.SIMPLE_TEXT;
    }

    public boolean matches(String input) {
        return expectedInput.equals(input);
    }

    @Override
    public String toString() {
        return String.format("MockExpectation{input='%s', response='%s', toolSteps=%d}",
                expectedInput, expectedResponse, toolSequence.getTotalSteps());
    }
}