package it.croway.openai.mock;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Context object that parses and provides easy access to request information.
 */
public class RequestContext {
    private final JsonNode rootNode;
    private final JsonNode messagesNode;

    public RequestContext(JsonNode rootNode) {
        this.rootNode = rootNode;
        this.messagesNode = rootNode.path("messages");
    }

    public boolean hasToolRole() {
        if (!messagesNode.isArray()) {
            return false;
        }

        for (JsonNode messageNode : messagesNode) {
            String role = messageNode.path("role").asText();
            if ("tool".equals(role)) {
                return true;
            }
        }
        return false;
    }

    public String getFirstUserMessage() {
        if (!messagesNode.isArray()) {
            return null;
        }

        for (JsonNode messageNode : messagesNode) {
            String role = messageNode.path("role").asText();
            if ("user".equals(role)) {
                return messageNode.path("content").asText();
            }
        }
        return null;
    }

    public JsonNode getMessagesNode() {
        return messagesNode;
    }

    public JsonNode getRootNode() {
        return rootNode;
    }
}