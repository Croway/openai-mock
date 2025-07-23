package it.croway.openai.mock;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OpenAIMockFailuresTest {

    @RegisterExtension
    public OpenAIMock openAIMock = new OpenAIMock();

    @Test
    public void testBadRequest() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(openAIMock.getBaseUrl() + "v1/chat/completions");
            request.setEntity(new StringEntity("{\"messages\": [{\"role\": \"assistant\", \"content\": \"any sentence\"}]}"));
            request.setHeader("Content-type", "application/json");

            HttpResponse response = client.execute(request);
            assertEquals(500, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testNotFound() throws Exception {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(openAIMock.getBaseUrl() + "v1/chat/completions");
            request.setEntity(new StringEntity("{\"messages\": [{\"role\": \"user\", \"content\": \"not found sentence\"}]}"));
            request.setHeader("Content-type", "application/json");

            HttpResponse response = client.execute(request);
            assertEquals(500, response.getStatusLine().getStatusCode());
        }
    }

    @Test
    public void testBuilderExceptions() {
        OpenAIMockBuilder builder = new OpenAIMock().builder();
        assertThrows(IllegalStateException.class, () -> builder.replyWith("test"));
        assertThrows(IllegalStateException.class, () -> builder.invokeTool("test"));
        assertThrows(IllegalStateException.class, () -> builder.withParam("key", "value"));
        assertThrows(IllegalStateException.class, () -> builder.thenRespondWith((req, in) -> null));
        assertThrows(IllegalStateException.class, () -> builder.end());

        builder.when("sentence");
        assertThrows(IllegalStateException.class, () -> builder.withParam("key", "value"));
    }
}