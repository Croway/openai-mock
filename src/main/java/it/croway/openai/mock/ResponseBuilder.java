package it.croway.openai.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Builder class for creating different types of OpenAI API mock responses.
 */
public class ResponseBuilder {
    private final ObjectMapper objectMapper;

    public ResponseBuilder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public MockResponse createSimpleTextResponse(String content) throws Exception {
        Map<String, Object> responseMessage = createBaseMessage();
        responseMessage.put("content", content);

        Map<String, Object> choice = createBaseChoice("stop", responseMessage);
        Map<String, Object> chatCompletion = createBaseChatCompletion(choice);

        return new MockResponse().setBody(objectMapper.writeValueAsString(chatCompletion));
    }

    public MockResponse createToolCallResponse(String content, List<ToolCallDefinition> toolCalls) throws Exception {
        Map<String, Object> responseMessage = createBaseMessage();
        responseMessage.put("content", content);
        responseMessage.put("tool_calls", buildToolCallsList(toolCalls));

        Map<String, Object> choice = createBaseChoice("tool_calls", responseMessage);
        Map<String, Object> chatCompletion = createBaseChatCompletion(choice);

        return new MockResponse().setBody(objectMapper.writeValueAsString(chatCompletion));
    }

    public MockResponse createFinalToolResponse(JsonNode messagesNode, String fallbackContent) throws Exception {
        Map<String, Object> responseMessage = createBaseMessage();

        String content = extractLastToolContent(messagesNode)
                .orElse(fallbackContent != null ? fallbackContent : "All tools processed");
        responseMessage.put("content", content);

        Map<String, Object> choice = createBaseChoice("stop", responseMessage);
        Map<String, Object> chatCompletion = createBaseChatCompletion(choice);
        chatCompletion.put("history", messagesNode);

        return new MockResponse().setBody(objectMapper.writeValueAsString(chatCompletion));
    }

    public MockResponse createErrorResponse(int statusCode, String errorMessage) {
        String errorBody = String.format("{\"error\": \"%s\"}", errorMessage);
        return new MockResponse().setResponseCode(statusCode).setBody(errorBody);
    }

    private Map<String, Object> createBaseMessage() {
        Map<String, Object> message = new HashMap<>();
        message.put("role", "assistant");
        message.put("refusal", null);
        return message;
    }

    private Map<String, Object> createBaseChoice(String finishReason, Map<String, Object> message) {
        Map<String, Object> choice = new HashMap<>();
        choice.put("finish_reason", finishReason);
        choice.put("index", 0);
        choice.put("message", message);
        return choice;
    }

    private Map<String, Object> createBaseChatCompletion(Map<String, Object> choice) {
        Map<String, Object> chatCompletion = new HashMap<>();
        chatCompletion.put("id", UUID.randomUUID().toString());
        chatCompletion.put("choices", Collections.singletonList(choice));
        chatCompletion.put("created", System.currentTimeMillis() / 1000L);
        chatCompletion.put("model", "gpt-3.5-turbo");
        chatCompletion.put("object", "chat.completion");
        return chatCompletion;
    }

    private List<Map<String, Object>> buildToolCallsList(List<ToolCallDefinition> toolCalls) throws Exception {
        List<Map<String, Object>> toolCallsList = new ArrayList<>();

        for (ToolCallDefinition toolCall : toolCalls) {
            String argumentsJson = objectMapper.writeValueAsString(toolCall.getArguments());

            Map<String, Object> functionObject = new HashMap<>();
            functionObject.put("name", toolCall.getName());
            functionObject.put("arguments", argumentsJson);

            Map<String, Object> toolCallItem = new HashMap<>();
            toolCallItem.put("id", UUID.randomUUID().toString());
            toolCallItem.put("type", "function");
            toolCallItem.put("function", functionObject);

            toolCallsList.add(toolCallItem);
        }

        return toolCallsList;
    }

    private Optional<String> extractLastToolContent(JsonNode messagesNode) {
        return StreamSupport.stream(messagesNode.spliterator(), false)
                .filter(entry -> entry.has("role") && "tool".equals(entry.get("role").asText()))
                .reduce((first, second) -> second) // Get the last one
                .map(entry -> entry.get("content"))
                .map(JsonNode::asText);
    }
}