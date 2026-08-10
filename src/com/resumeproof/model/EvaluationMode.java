package com.resumeproof.model;

public enum EvaluationMode {
    STRICT("Strict Mode", "Mandatory requirements are hard filters. Candidates missing mandatory requirements are not shortlisted."),
    BALANCED("Balanced Mode", "Mandatory requirements act as primary filters. Preferred requirements and evidence strength drive ranking."),
    BEST_MATCH("Best Match Mode", "No hard elimination. Ranks candidates holistically based on overall suitability.");

    private final String title;
    private final String description;

    EvaluationMode(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
}
