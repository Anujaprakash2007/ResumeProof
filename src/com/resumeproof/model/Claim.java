package com.resumeproof.model;

import java.util.List;

public class Claim {
    private String claimText;
    private EvidenceStrength strength;
    private double verificationScore;
    private List<String> checklist;
    private String recommendedAction;

    public Claim(String claimText, EvidenceStrength strength, double verificationScore, List<String> checklist, String recommendedAction) {
        this.claimText = claimText;
        this.strength = strength;
        this.verificationScore = verificationScore;
        this.checklist = checklist;
        this.recommendedAction = recommendedAction;
    }

    public String getClaimText() { return claimText; }
    public EvidenceStrength getStrength() { return strength; }
    public double getVerificationScore() { return verificationScore; }
    public List<String> getChecklist() { return checklist; }
    public String getRecommendedAction() { return recommendedAction; }
}
