package com.resumeproof.model;

public class InterviewQuestion {
    private String targetRequirementOrClaim;
    private String questionText;
    private String category;
    private int priority;

    public InterviewQuestion(String targetRequirementOrClaim, String questionText, String category, int priority) {
        this.targetRequirementOrClaim = targetRequirementOrClaim;
        this.questionText = questionText;
        this.category = category;
        this.priority = priority;
    }

    public String getTargetRequirementOrClaim() { return targetRequirementOrClaim; }
    public String getQuestionText() { return questionText; }
    public String getCategory() { return category; }
    public int getPriority() { return priority; }
}
