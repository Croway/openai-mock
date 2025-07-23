package it.croway.openai.mock;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a sequence of tool execution steps.
 * Each step can contain multiple parallel tool calls.
 */
public class ToolExecutionSequence {
    private final List<ToolExecutionStep> steps;
    private int currentStepIndex;

    public ToolExecutionSequence() {
        this.steps = new ArrayList<>();
        this.currentStepIndex = 0;
    }

    public void addStep(ToolExecutionStep step) {
        steps.add(step);
    }

    public ToolExecutionStep getCurrentStep() {
        if (hasCurrentStep()) {
            return steps.get(currentStepIndex);
        }
        return new ToolExecutionStep(); // Return empty step if no current step
    }

    public void advanceToNextStep() {
        currentStepIndex++;
    }

    public boolean hasCurrentStep() {
        return currentStepIndex < steps.size();
    }

    public boolean hasMoreSteps() {
        return currentStepIndex < steps.size();
    }

    public boolean isInProgress() {
        return currentStepIndex > 0 && currentStepIndex < steps.size();
    }

    public void reset() {
        currentStepIndex = 0;
    }

    public boolean isEmpty() {
        return steps.isEmpty();
    }

    public int getTotalSteps() {
        return steps.size();
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }
}