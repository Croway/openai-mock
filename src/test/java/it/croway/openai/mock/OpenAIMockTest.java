package it.croway.openai.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpenAIMockTest {

    @RegisterExtension
    public OpenAIMock openAIMock = new OpenAIMock().builder()
            .when("any sentence")
                .invokeTool("toolName")
                .withParam("param1", "value1")
            .end()
            .when("another sentence")
                .replyWith("hello World")
            .end()
            .when("multiple tools")
                .invokeTool("tool1")
                .withParam("p1", "v1")
                .andInvokeTool("tool2")
                .withParam("p2", "v2")
                .withParam("p3", "v3")
            .end()
            .when("custom response")
                .thenRespondWith((request, input) ->
                        new MockResponse().setBody("Custom response for: " + input).setResponseCode(200))
            .end()
            .when("assert request")
                .assertRequest(request -> {
                    Assertions.assertEquals("test", request);
                })
                .replyWith("Request asserted successfully")
            .build();

    @Test
    public void testToolResponse() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(openAIMock.getBaseUrl() + "v1/chat/completions");
            request.setEntity(new StringEntity("{\"messages\": [{\"role\": \"user\", \"content\": \"any sentence\"}]}"));
            request.setHeader("Content-type", "application/json");

            HttpResponse response = client.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(responseBody);

            JsonNode choice = responseJson.path("choices").get(0);
            JsonNode message = choice.path("message");

            assertEquals("assistant", message.path("role").asText());
            assertEquals(true, message.path("content").isNull());
            assertEquals(true, message.path("refusal").isNull());

            JsonNode toolCalls = message.path("tool_calls");
            assertEquals(1, toolCalls.size());

            JsonNode toolCall = toolCalls.get(0);
            assertEquals("function", toolCall.path("type").asText());
            assertEquals("toolName", toolCall.path("function").path("name").asText());
            assertEquals("{\"param1\":\"value1\"}", toolCall.path("function").path("arguments").asText());
        }
    }

    @Test
    public void testChatResponse() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(openAIMock.getBaseUrl() + "v1/chat/completions");
            request.setEntity(new StringEntity("{\"messages\": [{\"role\": \"user\", \"content\": \"another sentence\"}]}"));
            request.setHeader("Content-type", "application/json");

            HttpResponse response = client.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(responseBody);

            JsonNode choice = responseJson.path("choices").get(0);
            JsonNode message = choice.path("message");

            assertEquals("assistant", message.path("role").asText());
            assertEquals("hello World", message.path("content").asText());
            assertEquals(true, message.path("refusal").isNull());
            assertEquals(true, message.path("tool_calls").isMissingNode());
        }
    }

    @Test
    public void testMultipleToolCallsResponse() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(openAIMock.getBaseUrl() + "v1/chat/completions");
            request.setEntity(new StringEntity("{\"messages\": [{\"role\": \"user\", \"content\": \"multiple tools\"}]}"));
            request.setHeader("Content-type", "application/json");

            HttpResponse response = client.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(responseBody);

            JsonNode choice = responseJson.path("choices").get(0);
            JsonNode message = choice.path("message");

            assertEquals("assistant", message.path("role").asText());
            assertEquals(true, message.path("content").isNull());
            assertEquals(true, message.path("refusal").isNull());

            JsonNode toolCalls = message.path("tool_calls");
            assertEquals(2, toolCalls.size());

            JsonNode toolCall1 = toolCalls.get(0);
            assertEquals("function", toolCall1.path("type").asText());
            assertEquals("tool1", toolCall1.path("function").path("name").asText());
            assertEquals("{\"p1\":\"v1\"}", toolCall1.path("function").path("arguments").asText());

            JsonNode toolCall2 = toolCalls.get(1);
            assertEquals("function", toolCall2.path("type").asText());
            assertEquals("tool2", toolCall2.path("function").path("name").asText());
            assertEquals("{\"p2\":\"v2\",\"p3\":\"v3\"}", toolCall2.path("function").path("arguments").asText());
        }
    }

    @Test
    public void testCustomResponse() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(openAIMock.getBaseUrl() + "v1/chat/completions");
            request.setEntity(new StringEntity("{\"messages\": [{\"role\": \"user\", \"content\": \"custom response\"}]}"));
            request.setHeader("Content-type", "application/json");

            HttpResponse response = client.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            assertEquals("Custom response for: custom response", responseBody);
        }
    }

    @Test
    public void testToolResponseAndStop() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(openAIMock.getBaseUrl() + "v1/chat/completions");
            request.setEntity(new StringEntity("{\"messages\": [{\"role\": \"user\", \"content\": \"any sentence\"}]}"));
            request.setHeader("Content-type", "application/json");

            HttpResponse response = client.execute(request);
            String responseBody = EntityUtils.toString(response.getEntity());

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(responseBody);

            JsonNode choice = responseJson.path("choices").get(0);
            JsonNode message = choice.path("message");

            assertEquals("assistant", message.path("role").asText());
            assertEquals(true, message.path("content").isNull());
            assertEquals(true, message.path("refusal").isNull());

            JsonNode toolCalls = message.path("tool_calls");
            assertEquals(1, toolCalls.size());

            JsonNode toolCall = toolCalls.get(0);
            String toolCallId = toolCall.path("id").asText();
            assertEquals("function", toolCall.path("type").asText());
            assertEquals("toolName", toolCall.path("function").path("name").asText());
            assertEquals("{\"param1\":\"value1\"}", toolCall.path("function").path("arguments").asText());

            // Second request with tool result
            String secondRequestBody = String.format("{\"messages\": [{\"role\": \"user\", \"content\": \"any sentence\"}, {\"role\":\"tool\", \"tool_call_id\":\"%s\", \"content\":\"{\\\"name\\\": \\\"pippo\\\"}\"}]}", toolCallId);
            request.setEntity(new StringEntity(secondRequestBody));
            response = client.execute(request);
            responseBody = EntityUtils.toString(response.getEntity());
            responseJson = objectMapper.readTree(responseBody);

            choice = responseJson.path("choices").get(0);
            assertEquals("stop", choice.path("finish_reason").asText());
        }
    }
}