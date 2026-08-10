package com.resumeproof.model;

public class Evidence {
    private String requirementName;
    private EvidenceStrength strength;
    private String matchedSnippet;
    private String sourceSection;
    private String contextNote;
    private double confidenceScore;

    public Evidence(String requirementName, EvidenceStrength strength, String matchedSnippet, String sourceSection, String contextNote, double confidenceScore) {
        this.requirementName = requirementName;
        this.strength = strength;
        this.matchedSnippet = matchedSnippet;
        this.sourceSection = sourceSection;
        this.contextNote = contextNote;
        this.confidenceScore = confidenceScore;
    }

    public String getRequirementName() { return requirementName; }
    public EvidenceStrength getStrength() { return strength; }
    public String getMatchedSnippet() { return matchedSnippet; }
    public String getSourceSection() { return sourceSection; }
    public String getContextNote() { return contextNote; }
    public double getConfidenceScore() { return confidenceScore; }
}
