package com.resumeproof.parser;

import com.resumeproof.model.CandidateProfile;
import com.resumeproof.model.CareerPreferences;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResumeTextParser {

    public static CandidateProfile parseResume(String id, String rawText) {
        String text = TextNormalizer.normalize(rawText);
        String name = extractHeaderField(text, "NAME:", "Candidate " + id);
        String email = extractHeaderField(text, "EMAIL:", "candidate" + id + "@example.com");
        String phone = extractHeaderField(text, "PHONE:", "Not specified");

        CandidateProfile profile = new CandidateProfile(id, name, email, phone, text);
        Map<String, String> sections = extractSections(text);
        profile.setParsedSections(sections);

        CareerPreferences prefs = extractCareerPreferences(text);
        profile.setPreferences(prefs);

        profile.setExtractedSkills(extractSkillList(sections.getOrDefault("SKILLS", text)));
        profile.setExtractedProjects(extractProjectList(sections.getOrDefault("PROJECTS", "")));

        return profile;
    }

    private static String extractHeaderField(String text, String prefix, String defaultValue) {
        for (String line : text.split("\n")) {
            if (line.toUpperCase().startsWith(prefix.toUpperCase())) {
                String val = line.substring(prefix.length()).trim();
                if (!val.isEmpty()) return val;
            }
        }
        return defaultValue;
    }

    private static Map<String, String> extractSections(String text) {
        Map<String, String> sections = new HashMap<>();
        String[] lines = text.split("\n");

        String currentSection = "SUMMARY";
        StringBuilder sectionText = new StringBuilder();

        for (String line : lines) {
            String upper = line.trim().toUpperCase();
            String matchedSection = null;

            if (upper.startsWith("TECHNICAL SKILLS") || upper.equals("SKILLS")) {
                matchedSection = "SKILLS";
            } else if (upper.startsWith("PROJECTS") || upper.equals("KEY PROJECTS")) {
                matchedSection = "PROJECTS";
            } else if (upper.startsWith("WORK EXPERIENCE") || upper.startsWith("EXPERIENCE")) {
                matchedSection = "WORK_EXPERIENCE";
            } else if (upper.startsWith("EDUCATION")) {
                matchedSection = "EDUCATION";
            } else if (upper.startsWith("CERTIFICATIONS")) {
                matchedSection = "CERTIFICATIONS";
            }

            if (matchedSection != null) {
                if (sectionText.length() > 0) {
                    sections.put(currentSection, sectionText.toString().trim());
                }
                currentSection = matchedSection;
                sectionText = new StringBuilder();
            } else {
                sectionText.append(line).append("\n");
            }
        }

        if (sectionText.length() > 0) {
            sections.put(currentSection, sectionText.toString().trim());
        }

        return sections;
    }

    private static CareerPreferences extractCareerPreferences(String text) {
        List<String> dreamCompanies = new ArrayList<>();
        List<String> preferredRoles = new ArrayList<>();
        List<String> preferredDomains = new ArrayList<>();

        for (String line : text.split("\n")) {
            String upper = line.toUpperCase().trim();
            if (upper.startsWith("DREAM COMPANIES:")) {
                String[] parts = line.substring(line.indexOf(":") + 1).split(",");
                for (String p : parts) if (!p.trim().isEmpty()) dreamCompanies.add(p.trim());
            } else if (upper.startsWith("PREFERRED ROLE:") || upper.startsWith("PREFERRED ROLES:")) {
                String[] parts = line.substring(line.indexOf(":") + 1).split(",");
                for (String p : parts) if (!p.trim().isEmpty()) preferredRoles.add(p.trim());
            } else if (upper.startsWith("PREFERRED DOMAIN:") || upper.startsWith("PREFERRED DOMAINS:")) {
                String[] parts = line.substring(line.indexOf(":") + 1).split(",");
                for (String p : parts) if (!p.trim().isEmpty()) preferredDomains.add(p.trim());
            }
        }

        return new CareerPreferences(dreamCompanies, preferredRoles, preferredDomains);
    }

    private static List<String> extractSkillList(String skillsText) {
        List<String> skills = new ArrayList<>();
        if (skillsText == null) return skills;

        for (String line : skillsText.split("\n")) {
            if (line.contains(":")) {
                line = line.substring(line.indexOf(":") + 1);
            }
            String[] tokens = line.split("[,•\\-]");
            for (String t : tokens) {
                String cleaned = t.trim();
                if (!cleaned.isEmpty() && cleaned.length() < 30) {
                    skills.add(cleaned);
                }
            }
        }
        return skills;
    }

    private static List<String> extractProjectList(String projectsText) {
        List<String> projects = new ArrayList<>();
        if (projectsText == null) return projects;

        for (String line : projectsText.split("\n")) {
            line = line.trim();
            if (line.startsWith("-") || line.startsWith("•") || line.startsWith("Project")) {
                projects.add(line.replaceAll("^[-•]\\s*", ""));
            }
        }
        return projects;
    }
}
