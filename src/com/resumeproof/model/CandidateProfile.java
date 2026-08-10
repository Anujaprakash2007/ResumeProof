package com.resumeproof.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CandidateProfile {
    private String id;
    private String name;
    private String email;
    private String phone;
    private String rawResumeText;
    private Map<String, String> parsedSections;
    private List<String> extractedSkills;
    private List<String> extractedProjects;
    private double experienceYears;
    private CareerPreferences preferences;

    public CandidateProfile(String id, String name, String email, String phone, String rawResumeText) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.rawResumeText = rawResumeText;
        this.parsedSections = new HashMap<>();
        this.extractedSkills = new ArrayList<>();
        this.extractedProjects = new ArrayList<>();
        this.experienceYears = 0.0;
        this.preferences = new CareerPreferences();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRawResumeText() { return rawResumeText; }
    public Map<String, String> getParsedSections() { return parsedSections; }
    public List<String> getExtractedSkills() { return extractedSkills; }
    public List<String> getExtractedProjects() { return extractedProjects; }
    public double getExperienceYears() { return experienceYears; }
    public CareerPreferences getPreferences() { return preferences; }

    public void setParsedSections(Map<String, String> parsedSections) { this.parsedSections = parsedSections; }
    public void setExtractedSkills(List<String> extractedSkills) { this.extractedSkills = extractedSkills; }
    public void setExtractedProjects(List<String> extractedProjects) { this.extractedProjects = extractedProjects; }
    public void setExperienceYears(double experienceYears) { this.experienceYears = experienceYears; }
    public void setPreferences(CareerPreferences preferences) { this.preferences = preferences; }
}
