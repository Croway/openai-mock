package it.croway.openai.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single step in a tool execution sequence.
 * A step can contain multiple tools that should be executed in parallel.
 */
public class ToolExecutionStep {
    private final List<ToolCallDefinition> toolCalls;

    public ToolExecutionStep() {
        this.toolCalls = new ArrayList<>();
    }

    public void addToolCall(ToolCallDefinition toolCall) {
        this.toolCalls.add(Objects.requireNonNull(toolCall, "Tool call cannot be null"));
    }

    public List<ToolCallDefinition> getToolCalls() {
        return new ArrayList<>(toolCalls); // Return defensive copy
    }

    public boolean isEmpty() {
        return toolCalls.isEmpty();
    }

    public int size() {
        return toolCalls.size();
    }

    public ToolCallDefinition getLastToolCall() {
        if (toolCalls.isEmpty()) {
            throw new IllegalStateException("No tool calls in this step");
        }
        return toolCalls.get(toolCalls.size() - 1);
    }

    @Override
    public String toString() {
        return String.format("ToolExecutionStep{toolCalls=%s}", toolCalls);
    }
}