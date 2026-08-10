package com.resumeproof.model;

import java.util.ArrayList;
import java.util.List;

public class CareerPreferences {
    private List<String> dreamCompanies;
    private List<String> preferredRoles;
    private List<String> preferredDomains;

    public CareerPreferences() {
        this.dreamCompanies = new ArrayList<>();
        this.preferredRoles = new ArrayList<>();
        this.preferredDomains = new ArrayList<>();
    }

    public CareerPreferences(List<String> dreamCompanies, List<String> preferredRoles, List<String> preferredDomains) {
        this.dreamCompanies = dreamCompanies != null ? dreamCompanies : new ArrayList<>();
        this.preferredRoles = preferredRoles != null ? preferredRoles : new ArrayList<>();
        this.preferredDomains = preferredDomains != null ? preferredDomains : new ArrayList<>();
    }

    public List<String> getDreamCompanies() { return dreamCompanies; }
    public List<String> getPreferredRoles() { return preferredRoles; }
    public List<String> getPreferredDomains() { return preferredDomains; }
}
