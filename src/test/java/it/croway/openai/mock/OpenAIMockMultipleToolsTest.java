package it.croway.openai.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class OpenAIMockMultipleToolsTest {

    @RegisterExtension
    OpenAIMock openAIMock = new OpenAIMock().builder()
            .when("What is the weather in london?")
            .invokeTool("FindsTheLatitudeAndLongitudeOfAGivenCity")
            .withParam("name", "London")
            .andThenInvokeTool("ForecastsTheWeatherForTheGivenLatitudeAndLongitude")
            .withParam("latitude", "51.50758961965397")
            .withParam("longitude", "-0.13388057363742217")
            .build();

    @Test
    void testInvokeToolAndThenInvokeTool() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            ObjectMapper objectMapper = new ObjectMapper();

            // First request: User asks for weather in London
            HttpPost request1 = new HttpPost(openAIMock.getBaseUrl() + "v1/chat/completions");
            request1.setEntity(new StringEntity("{\"messages\": [{\"role\": \"user\", \"content\": \"What is the weather in london?\"}]}"));
            request1.setHeader("Content-type", "application/json");

            HttpResponse response1 = client.execute(request1);
            String responseBody1 = EntityUtils.toString(response1.getEntity());
            JsonNode responseJson1 = objectMapper.readTree(responseBody1);

            JsonNode choice1 = responseJson1.path("choices").get(0);
            JsonNode message1 = choice1.path("message");

            // Assert first tool call
            Assertions.assertEquals("assistant", message1.path("role").asText());
            JsonNode toolCalls1 = message1.path("tool_calls");
            Assertions.assertEquals(1, toolCalls1.size());
            JsonNode toolCall1 = toolCalls1.get(0);
            Assertions.assertEquals("FindsTheLatitudeAndLongitudeOfAGivenCity", toolCall1.path("function").path("name").asText());
            Assertions.assertEquals("{\"name\":\"London\"}", toolCall1.path("function").path("arguments").asText());
            String toolCallId1 = toolCall1.path("id").asText();

            // Second request: LLM provides tool output for the first tool call
            String secondRequestBody = String.format("{\"messages\": [{\"role\": \"user\", \"content\": \"What is the weather in london?\"}, {\"role\":\"assistant\", \"tool_calls\": [{\"id\":\"%s\", \"type\":\"function\", \"function\":{\"name\":\"FindsTheLatitudeAndLongitudeOfAGivenCity\", \"arguments\":\"{\\\"name\\\":\\\"London\\\"}\"}}]}, {\"role\":\"tool\", \"tool_call_id\":\"%s\", \"content\":\"{\\\"latitude\\\": \\\"51.50758961965397\\\", \\\"longitude\\\": \\\"-0.13388057363742217\\\"}\"}]}", toolCallId1, toolCallId1);
            HttpPost request2 = new HttpPost(openAIMock.getBaseUrl() + "v1/chat/completions");
            request2.setEntity(new StringEntity(secondRequestBody));
            request2.setHeader("Content-type", "application/json");

            HttpResponse response2 = client.execute(request2);
            String responseBody2 = EntityUtils.toString(response2.getEntity());
            JsonNode responseJson2 = objectMapper.readTree(responseBody2);

            JsonNode choice2 = responseJson2.path("choices").get(0);
            JsonNode message2 = choice2.path("message");

            // Assert second tool call
            Assertions.assertEquals("assistant", message2.path("role").asText());
            JsonNode toolCalls2 = message2.path("tool_calls");
            Assertions.assertEquals(1, toolCalls2.size());
            JsonNode toolCall2 = toolCalls2.get(0);
            Assertions.assertEquals("ForecastsTheWeatherForTheGivenLatitudeAndLongitude", toolCall2.path("function").path("name").asText());
            Assertions.assertEquals("{\"latitude\":\"51.50758961965397\",\"longitude\":\"-0.13388057363742217\"}", toolCall2.path("function").path("arguments").asText());
            String toolCallId2 = toolCall2.path("id").asText();

            // Third request: LLM provides tool output for the second tool call
            String thirdRequestBody = String.format("{\"messages\": [{\"role\": \"user\", \"content\": \"What is the weather in london?\"}, {\"role\":\"assistant\", \"tool_calls\": [{\"id\":\"%s\", \"type\":\"function\", \"function\":{\"name\":\"FindsTheLatitudeAndLongitudeOfAGivenCity\", \"arguments\":\"{\\\"name\\\":\\\"London\\\"}\"}}]}, {\"role\":\"tool\", \"tool_call_id\":\"%s\", \"content\":\"{\\\"latitude\\\": \\\"51.50758961965397\\\", \\\"longitude\\\": \\\"-0.13388057363742217\\\"}\"}, {\"role\":\"assistant\", \"tool_calls\": [{\"id\":\"%s\", \"type\":\"function\", \"function\":{\"name\":\"ForecastsTheWeatherForTheGivenLatitudeAndLongitude\", \"arguments\":\"{\\\"latitude\\\":\\\"51.50758961965397\\\",\\\"longitude\\\":\\\"-0.13388057363742217\\\"}\"}}]}, {\"role\":\"tool\", \"tool_call_id\":\"%s\", \"content\":\"{\\\"forecast\\\": \\\"Sunny with a chance of showers\\\"}\"}]}", toolCallId1, toolCallId1, toolCallId2, toolCallId2);
            HttpPost request3 = new HttpPost(openAIMock.getBaseUrl() + "v1/chat/completions");
            request3.setEntity(new StringEntity(thirdRequestBody));
            request3.setHeader("Content-type", "application/json");

            HttpResponse response3 = client.execute(request3);
            String responseBody3 = EntityUtils.toString(response3.getEntity());
            JsonNode responseJson3 = objectMapper.readTree(responseBody3);

            JsonNode choice3 = responseJson3.path("choices").get(0);
            JsonNode message3 = choice3.path("message");

            // Assert final response
            Assertions.assertEquals("assistant", message3.path("role").asText());
            Assertions.assertFalse(message3.path("content").isNull());
            Assertions.assertEquals("{\"forecast\": \"Sunny with a chance of showers\"}", message3.path("content").asText());
            Assertions.assertEquals("stop", choice3.path("finish_reason").asText());
        }
    }
}
