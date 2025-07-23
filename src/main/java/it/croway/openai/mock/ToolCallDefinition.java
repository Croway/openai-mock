package it.croway.openai.mock;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a tool call with its name and parameters.
 */
public class ToolCallDefinition {
    private final String name;
    private final Map<String, Object> arguments;

    public ToolCallDefinition(String name) {
        this.name = Objects.requireNonNull(name, "Tool name cannot be null");
        this.arguments = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getArguments() {
        return new HashMap<>(arguments); // Return defensive copy
    }

    public void addArgument(String key, Object value) {
        arguments.put(key, value);
    }

    @Override
    public String toString() {
        return String.format("ToolCall{name='%s', arguments=%s}", name, arguments);
    }
}