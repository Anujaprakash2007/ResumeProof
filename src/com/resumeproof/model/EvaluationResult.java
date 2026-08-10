package com.resumeproof.model;

import java.util.ArrayList;
import java.util.List;

public class EvaluationResult {
    private String candidateId;
    private String candidateName;
    private EvaluationMode mode;
    private double qualificationScore;
    private boolean isShortlisted;
    private String rejectionReason;
    private List<Evidence> evidenceList;
    private List<Claim> claimList;
    private List<String> strongAreas;
    private List<String> partialAreas;
    private List<String> missingRequirements;
    private List<String> verificationFlags;
    private List<InterviewQuestion> interviewQuestions;
    private String preferenceAlignment;
    private String explanationText;

    public EvaluationResult(String candidateId, String candidateName, EvaluationMode mode) {
        this.candidateId = candidateId;
        this.candidateName = candidateName;
        this.mode = mode;
        this.qualificationScore = 0.0;
        this.isShortlisted = true;
        this.rejectionReason = "";
        this.evidenceList = new ArrayList<>();
        this.claimList = new ArrayList<>();
        this.strongAreas = new ArrayList<>();
        this.partialAreas = new ArrayList<>();
        this.missingRequirements = new ArrayList<>();
        this.verificationFlags = new ArrayList<>();
        this.interviewQuestions = new ArrayList<>();
        this.preferenceAlignment = "MEDIUM";
        this.explanationText = "";
    }

    public String getCandidateId() { return candidateId; }
    public String getCandidateName() { return candidateName; }
    public EvaluationMode getMode() { return mode; }
    public double getQualificationScore() { return qualificationScore; }
    public boolean isShortlisted() { return isShortlisted; }
    public String getRejectionReason() { return rejectionReason; }
    public List<Evidence> getEvidenceList() { return evidenceList; }
    public List<Claim> getClaimList() { return claimList; }
    public List<String> getStrongAreas() { return strongAreas; }
    public List<String> getPartialAreas() { return partialAreas; }
    public List<String> getMissingRequirements() { return missingRequirements; }
    public List<String> getVerificationFlags() { return verificationFlags; }
    public List<InterviewQuestion> getInterviewQuestions() { return interviewQuestions; }
    public String getPreferenceAlignment() { return preferenceAlignment; }
    public String getExplanationText() { return explanationText; }

    public void setQualificationScore(double qualificationScore) { this.qualificationScore = qualificationScore; }
    public void setShortlisted(boolean shortlisted) { isShortlisted = shortlisted; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public void setEvidenceList(List<Evidence> evidenceList) { this.evidenceList = evidenceList; }
    public void setClaimList(List<Claim> claimList) { this.claimList = claimList; }
    public void setStrongAreas(List<String> strongAreas) { this.strongAreas = strongAreas; }
    public void setPartialAreas(List<String> partialAreas) { this.partialAreas = partialAreas; }
    public void setMissingRequirements(List<String> missingRequirements) { this.missingRequirements = missingRequirements; }
    public void setVerificationFlags(List<String> verificationFlags) { this.verificationFlags = verificationFlags; }
    public void setInterviewQuestions(List<InterviewQuestion> interviewQuestions) { this.interviewQuestions = interviewQuestions; }
    public void setPreferenceAlignment(String preferenceAlignment) { this.preferenceAlignment = preferenceAlignment; }
    public void setExplanationText(String explanationText) { this.explanationText = explanationText; }
}
