package com.resumeproof.model;

public enum RequirementCategory {
    SKILL_TECH("Technical Skill", 1.2),
    SKILL_SOFT("Soft Skill", 0.8),
    EXPERIENCE("Experience", 1.1),
    EDUCATION("Education", 1.0),
    PROJECT("Project Experience", 1.2),
    CERTIFICATION("Certification", 0.9);

    private final String label;
    private final double categoryWeight;

    RequirementCategory(String label, double categoryWeight) {
        this.label = label;
        this.categoryWeight = categoryWeight;
    }

    public String getLabel() { return label; }
    public double getCategoryWeight() { return categoryWeight; }
}
