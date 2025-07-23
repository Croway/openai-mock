package it.croway.openai.mock;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Fluent builder for creating OpenAI mock expectations.
 */
public class OpenAIMockBuilder {
    private static final Logger log = LoggerFactory.getLogger(OpenAIMockBuilder.class);

    private final OpenAIMock mock;
    private final List<MockExpectation> expectations;
    private MockExpectation currentExpectation;

    public OpenAIMockBuilder(OpenAIMock mock, List<MockExpectation> expectations) {
        this.mock = mock;
        this.expectations = expectations;
    }

    public OpenAIMockBuilder when(String expectedInput) {
        log.debug("Setting up expectation for input: {}", expectedInput);
        currentExpectation = new MockExpectation(expectedInput);
        return this;
    }

    public OpenAIMockBuilder replyWith(String expectedResponse) {
        validateCurrentExpectation("replyWith()");
        log.debug("Setting expected response: {}", expectedResponse);
        currentExpectation.setExpectedResponse(expectedResponse);
        return this;
    }

    public OpenAIMockBuilder invokeTool(String toolName) {
        validateCurrentExpectation("invokeTool()");
        log.debug("Adding new tool execution step with tool: {}", toolName);

        ToolExecutionStep newStep = new ToolExecutionStep();
        newStep.addToolCall(new ToolCallDefinition(toolName));
        currentExpectation.addToolExecutionStep(newStep);

        return this;
    }

    public OpenAIMockBuilder andInvokeTool(String toolName) {
        validateCurrentExpectation("andInvokeTool()");
        validateHasToolSteps("andInvokeTool()");

        log.debug("Adding parallel tool to current step: {}", toolName);
        ToolExecutionStep currentStep = currentExpectation.getCurrentToolStep();
        currentStep.addToolCall(new ToolCallDefinition(toolName));

        return this;
    }

    public OpenAIMockBuilder withParam(String key, Object value) {
        validateCurrentExpectation("withParam()");
        validateHasToolSteps("withParam()");

        ToolExecutionStep currentStep = currentExpectation.getCurrentToolStep();
        if (currentStep.isEmpty()) {
            throw new IllegalStateException("No tool calls in current step to add parameters to");
        }

        ToolCallDefinition lastTool = currentStep.getLastToolCall();
        log.debug("Adding parameter {} = {} to tool: {}", key, value, lastTool.getName());
        lastTool.addArgument(key, value);

        return this;
    }

    public OpenAIMockBuilder thenRespondWith(BiFunction<RecordedRequest, String, MockResponse> responseFunction) {
        validateCurrentExpectation("thenRespondWith()");
        log.debug("Setting custom response function");
        currentExpectation.setCustomResponseFunction(responseFunction);
        return this;
    }

    public OpenAIMockBuilder assertRequest(Consumer<String> requestAssertion) {
        validateCurrentExpectation("assertRequest()");
        log.debug("Setting request assertion");
        currentExpectation.setRequestAssertion(requestAssertion);
        return this;
    }

    public OpenAIMockBuilder andThenInvokeTool(String toolName) {
        validateCurrentExpectation("andThenInvokeTool()");
        validateHasToolSteps("andThenInvokeTool()");

        log.debug("Creating new sequential step with tool: {}", toolName);
        ToolExecutionStep newStep = new ToolExecutionStep();
        newStep.addToolCall(new ToolCallDefinition(toolName));
        currentExpectation.addToolExecutionStep(newStep);
        currentExpectation.advanceToNextToolStep();

        return this;
    }

    public OpenAIMockBuilder end() {
        validateCurrentExpectation("end()");
        log.debug("Finalizing expectation for input: {}", currentExpectation.getExpectedInput());
        expectations.add(currentExpectation);
        currentExpectation = null;
        return this;
    }

    public OpenAIMock build() {
        if (currentExpectation != null) {
            log.debug("Auto-finalizing current expectation during build");
            expectations.add(currentExpectation);
            currentExpectation = null;
        }
        log.info("Built OpenAIMock with {} expectations", expectations.size());
        return mock;
    }

    private void validateCurrentExpectation(String methodName) {
        if (currentExpectation == null) {
            throw new IllegalStateException("Call when() before " + methodName);
        }
    }

    private void validateHasToolSteps(String methodName) {
        if (currentExpectation.getToolSequence().isEmpty()) {
            throw new IllegalStateException("Call invokeTool() before " + methodName);
        }
    }
}