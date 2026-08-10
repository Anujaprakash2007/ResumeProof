package com.resumeproof.model;

import java.util.ArrayList;
import java.util.List;

public class JobDescription {
    private String id;
    private String title;
    private String companyName;
    private String domain;
    private String rawText;
    private List<Requirement> mandatoryRequirements;
    private List<Requirement> preferredRequirements;
    private double minExperienceYears;
    private double maxExperienceYears;

    public JobDescription(String id, String title, String companyName, String domain, String rawText) {
        this.id = id;
        this.title = title;
        this.companyName = companyName;
        this.domain = domain;
        this.rawText = rawText;
        this.mandatoryRequirements = new ArrayList<>();
        this.preferredRequirements = new ArrayList<>();
        this.minExperienceYears = 0.0;
        this.maxExperienceYears = 2.0;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCompanyName() { return companyName; }
    public String getDomain() { return domain; }
    public String getRawText() { return rawText; }
    public List<Requirement> getMandatoryRequirements() { return mandatoryRequirements; }
    public List<Requirement> getPreferredRequirements() { return preferredRequirements; }
    public double getMinExperienceYears() { return minExperienceYears; }
    public double getMaxExperienceYears() { return maxExperienceYears; }

    public void setMandatoryRequirements(List<Requirement> mandatoryRequirements) { this.mandatoryRequirements = mandatoryRequirements; }
    public void setPreferredRequirements(List<Requirement> preferredRequirements) { this.preferredRequirements = preferredRequirements; }
    public void setMinExperienceYears(double minExperienceYears) { this.minExperienceYears = minExperienceYears; }
    public void setMaxExperienceYears(double maxExperienceYears) { this.maxExperienceYears = maxExperienceYears; }

    public List<Requirement> getAllRequirements() {
        List<Requirement> all = new ArrayList<>(mandatoryRequirements);
        all.addAll(preferredRequirements);
        return all;
    }
}
