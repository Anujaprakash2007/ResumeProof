package com.resumeproof.model;

public enum Importance {
    HIGH("High", 3.0),
    MEDIUM("Medium", 2.0),
    LOW("Low", 1.0);

    private final String label;
    private final double weight;

    Importance(String label, double weight) {
        this.label = label;
        this.weight = weight;
    }

    public String getLabel() { return label; }
    public double getWeight() { return weight; }
}
