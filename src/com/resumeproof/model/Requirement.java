package com.resumeproof.model;

import java.util.ArrayList;
import java.util.List;

public class Requirement {
    private String name;
    private RequirementCategory category;
    private Importance importance;
    private boolean isMandatory;
    private List<String> synonyms;

    public Requirement(String name, RequirementCategory category, Importance importance, boolean isMandatory) {
        this.name = name;
        this.category = category;
        this.importance = importance;
        this.isMandatory = isMandatory;
        this.synonyms = new ArrayList<>();
    }

    public Requirement(String name, RequirementCategory category, Importance importance, boolean isMandatory, List<String> synonyms) {
        this.name = name;
        this.category = category;
        this.importance = importance;
        this.isMandatory = isMandatory;
        this.synonyms = synonyms != null ? synonyms : new ArrayList<>();
    }

    public String getName() { return name; }
    public RequirementCategory getCategory() { return category; }
    public Importance getImportance() { return importance; }
    public boolean isMandatory() { return isMandatory; }
    public List<String> getSynonyms() { return synonyms; }

    public void setMandatory(boolean mandatory) { isMandatory = mandatory; }
    public void setImportance(Importance importance) { this.importance = importance; }
}
