package com.resumeproof.engine;

import com.resumeproof.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EvidenceEngine {

    public static List<Evidence> extractEvidence(CandidateProfile candidate, JobDescription jd) {
        List<Evidence> evidenceList = new ArrayList<>();
        List<Requirement> allReqs = jd.getAllRequirements();

        for (Requirement req : allReqs) {
            Evidence evidence = findEvidenceForRequirement(candidate, req);
            evidenceList.add(evidence);
        }

        return evidenceList;
    }

    public static Evidence findEvidenceForRequirement(CandidateProfile candidate, Requirement req) {
        String reqName = req.getName();
        List<String> keywords = new ArrayList<>();
        keywords.add(reqName.toLowerCase());
        for (String syn : req.getSynonyms()) {
            keywords.add(syn.toLowerCase());
        }

        Map<String, String> sections = candidate.getParsedSections();

        // Priority order of section inspection
        String[] sectionKeys = {"PROJECTS", "WORK_EXPERIENCE", "SKILLS", "SUMMARY", "EDUCATION", "CERTIFICATIONS"};

        EvidenceStrength bestStrength = EvidenceStrength.NOT_FOUND;
        String bestSnippet = "No relevant evidence found in candidate profile.";
        String bestSection = "None";
        String bestNote = "Requirement missing from resume text.";
        double confidence = 0.0;

        for (String secKey : sectionKeys) {
            String content = sections.get(secKey);
            if (content == null || content.isEmpty()) continue;

            String[] sentences = content.split("\n|\\.");
            for (String sentence : sentences) {
                String trimmed = sentence.trim();
                if (trimmed.isEmpty()) continue;

                for (String kw : keywords) {
                    if (trimmed.toLowerCase().contains(kw)) {
                        EvidenceStrength strength = ContextEngine.classifyContext(trimmed, secKey, kw);

                        // Select the strongest evidence found
                        if (strength.getMultiplier() > bestStrength.getMultiplier() || (bestStrength == EvidenceStrength.NOT_FOUND && strength == EvidenceStrength.NEGATIVE)) {
                            bestStrength = strength;
                            bestSnippet = trimmed;
                            bestSection = formatSectionName(secKey);
                            bestNote = generateContextNote(reqName, strength, secKey);
                            confidence = calculateConfidence(strength, secKey);
                        }
                    }
                }
            }
        }

        return new Evidence(reqName, bestStrength, bestSnippet, bestSection, bestNote, confidence);
    }

    private static String formatSectionName(String key) {
        switch (key) {
            case "PROJECTS": return "Project Experience";
            case "WORK_EXPERIENCE": return "Work Experience";
            case "SKILLS": return "Technical Skills";
            case "EDUCATION": return "Education History";
            case "CERTIFICATIONS": return "Certifications";
            default: return "Profile Summary";
        }
    }

    private static String generateContextNote(String reqName, EvidenceStrength strength, String section) {
        switch (strength) {
            case STRONG:
                return "Explicit contextual proof found in " + formatSectionName(section) + " with action verb.";
            case PARTIAL:
                return "Academic or coursework context identified in " + formatSectionName(section) + ".";
            case UNCLEAR:
                return "Passive or vague mention found in " + formatSectionName(section) + ". Verification recommended.";
            case NEGATIVE:
                return "Explicit negation phrase detected regarding " + reqName + ".";
            default:
                return "No evidence found.";
        }
    }

    private static double calculateConfidence(EvidenceStrength strength, String section) {
        double base = 0.50;
        if (strength == EvidenceStrength.STRONG) base = 0.95;
        else if (strength == EvidenceStrength.PARTIAL) base = 0.75;
        else if (strength == EvidenceStrength.UNCLEAR) base = 0.55;
        else if (strength == EvidenceStrength.NEGATIVE) base = 0.90;

        if ("PROJECTS".equals(section) || "WORK_EXPERIENCE".equals(section)) {
            base = Math.min(1.0, base + 0.05);
        }
        return base;
    }
}
