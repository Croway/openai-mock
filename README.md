# OpenAI Mock

OpenAI Mock is a lightweight Java library for mocking OpenAI's chat completions API (`/v1/chat/completions`) within your unit tests. It allows you to simulate responses from the OpenAI API without making actual network calls, making your tests faster, more reliable, and independent of external services.

It uses `okhttp3.mockwebserver` to run a local web server that intercepts requests to the OpenAI API and returns predefined responses.

## Features

*   Fluent builder API for setting up mock expectations.
*   Mock simple text responses.
*   Mock tool calls with parameters.
*   Mock custom responses with a lambda function.
*   Easy integration with JUnit 5 using `@RegisterExtension`.

## Usage

Here's an example of how to use `OpenAIMock` in your JUnit 5 tests:

```java
import com.example.OpenAIMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
// ... other imports

public class MyOpenAIApiTest {

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
            .end()
            .when("custom response")
                .thenRespondWith((request, input) -> {
                    return new MockResponse().setBody("Custom response for: " + input).setResponseCode(200);
                })
            .build();

    @Test
    public void testMyApi() throws Exception {
        // Your code that calls the OpenAI API
        // Make sure your HTTP client is configured to use openAIMock.getBaseUrl()
        // For example:
        String baseUrl = openAIMock.getBaseUrl();
        // ... rest of your test code
    }
}
```

### How it works

1.  Add the `OpenAIMock` as a JUnit 5 extension using `@RegisterExtension`.
2.  Use the `builder()` to define your mock expectations.
3.  For each expectation, specify the user's input sentence with `when()`.
4.  Define the mock response using `replyWith()`, `invokeTool()`, or `thenRespondWith()`.
5.  Chain multiple expectations together.
6.  Call `build()` at the end of the chain.
7.  In your test, get the base URL of the mock server using `openAIMock.getBaseUrl()` and configure your API client to use it.

## Building from source

This project uses Apache Maven. To build the project, run the following command from the root directory:

```bash
mvn clean install
```
