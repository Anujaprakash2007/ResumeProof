package com.resumeproof.engine;

import com.resumeproof.model.CandidateProfile;
import com.resumeproof.model.CareerPreferences;
import com.resumeproof.model.JobDescription;

public class PreferenceEngine {

    public static String calculateAlignment(CandidateProfile candidate, JobDescription jd) {
        CareerPreferences prefs = candidate.getPreferences();
        if (prefs == null) return "MEDIUM";

        int matches = 0;
        int total = 3;

        // Check 1: Role Preference
        for (String role : prefs.getPreferredRoles()) {
            if (jd.getTitle().toLowerCase().contains(role.toLowerCase()) || role.toLowerCase().contains(jd.getTitle().toLowerCase())) {
                matches++;
                break;
            }
        }

        // Check 2: Domain Preference
        for (String dom : prefs.getPreferredDomains()) {
            if (jd.getDomain().toLowerCase().contains(dom.toLowerCase()) || dom.toLowerCase().contains(jd.getDomain().toLowerCase())) {
                matches++;
                break;
            }
        }

        // Check 3: Dream Company
        for (String company : prefs.getDreamCompanies()) {
            if (jd.getCompanyName().toLowerCase().contains(company.toLowerCase()) || company.toLowerCase().contains(jd.getCompanyName().toLowerCase())) {
                matches++;
                break;
            }
        }

        if (matches >= 2) return "HIGH ALIGNMENT";
        if (matches == 1) return "MEDIUM ALIGNMENT";
        return "LOW ALIGNMENT";
    }
}
