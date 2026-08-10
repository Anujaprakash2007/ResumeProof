package com.resumeproof.model;

public enum EvidenceStrength {
    STRONG("Strong Evidence", "🟢", 1.00),
    PARTIAL("Partial Evidence", "🟡", 0.60),
    UNCLEAR("Needs Verification", "🟠", 0.30),
    NOT_FOUND("Not Found", "🔴", 0.00),
    NEGATIVE("Not Demonstrated", "⛔", -0.50);

    private final String label;
    private final String icon;
    private final double multiplier;

    EvidenceStrength(String label, String icon, double multiplier) {
        this.label = label;
        this.icon = icon;
        this.multiplier = multiplier;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public double getMultiplier() { return multiplier; }
}
