package com.resumeproof.parser;

import com.resumeproof.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DOMAIN-INDEPENDENT & JOB-ROLE-AGNOSTIC Requirement Extractor.
 * Extracts structured job title, domain, mandatory and preferred requirements
 * from recruiter inputs or raw job description text without domain bias.
 */
public class JDRequirementExtractor {

    public static JobDescription parseJD(String id, String rawText) {
        String text = TextNormalizer.normalize(rawText);
        String title = extractField(text, "POSITION:", extractField(text, "TITLE:", "Specialist Position"));
        String company = extractField(text, "COMPANY:", "Hiring Organization");
        String domain = extractField(text, "DOMAIN:", "General Industry");

        JobDescription jd = new JobDescription(id, title, company, domain, text);

        List<Requirement> mandatory = new ArrayList<>();
        List<Requirement> preferred = new ArrayList<>();

        boolean inMandatory = false;
        boolean inPreferred = false;

        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String upper = trimmed.toUpperCase();

            if (upper.startsWith("MANDATORY") || upper.startsWith("REQUIRED")) {
                inMandatory = true;
                inPreferred = false;
                // Check if inline comma list provided
                int colonIdx = trimmed.indexOf(":");
                if (colonIdx != -1 && colonIdx < trimmed.length() - 1) {
                    parseCommaSeparated(trimmed.substring(colonIdx + 1), true, mandatory);
                }
                continue;
            } else if (upper.startsWith("PREFERRED") || upper.startsWith("DESIRABLE")) {
                inMandatory = false;
                inPreferred = true;
                int colonIdx = trimmed.indexOf(":");
                if (colonIdx != -1 && colonIdx < trimmed.length() - 1) {
                    parseCommaSeparated(trimmed.substring(colonIdx + 1), false, preferred);
                }
                continue;
            } else if (upper.startsWith("POSITION:") || upper.startsWith("TITLE:") || upper.startsWith("COMPANY:") || upper.startsWith("DOMAIN:") || upper.startsWith("DESCRIPTION:")) {
                inMandatory = false;
                inPreferred = false;
                continue;
            }

            if (trimmed.startsWith("-") || trimmed.startsWith("•") || trimmed.startsWith("*")) {
                String reqName = trimmed.replaceAll("^[-•*]\\s*", "").trim();
                if (!reqName.isEmpty()) {
                    Requirement req = createRequirement(reqName, inMandatory);
                    if (inMandatory) {
                        mandatory.add(req);
                    } else if (inPreferred) {
                        preferred.add(req);
                    } else {
                        mandatory.add(req); // Default to mandatory if bullet list with no section
                    }
                }
            } else if (inMandatory || inPreferred) {
                // Handle unbulleted comma list lines under mandatory/preferred headers
                parseCommaSeparated(trimmed, inMandatory, inMandatory ? mandatory : preferred);
            }
        }

        // Generic domain-agnostic fallback if no explicit headers/bullets detected
        if (mandatory.isEmpty() && preferred.isEmpty()) {
            mandatory.add(createRequirement("Core Industry Competency", true));
            mandatory.add(createRequirement("Relevant Domain Experience", true));
            preferred.add(createRequirement("Advanced Industry Certification", false));
        }

        jd.setMandatoryRequirements(mandatory);
        jd.setPreferredRequirements(preferred);
        return jd;
    }

    public static Requirement createRequirement(String name, boolean isMandatory) {
        String cleanName = name.trim();
        String lower = cleanName.toLowerCase();

        RequirementCategory category = RequirementCategory.SKILL_TECH;
        Importance importance = isMandatory ? Importance.HIGH : Importance.MEDIUM;
        List<String> synonyms = new ArrayList<>();

        if (lower.contains("project") || lower.contains("system") || lower.contains("campaign") || lower.contains("portfolio")) {
            category = RequirementCategory.PROJECT;
            synonyms.add(cleanName.replace("Project", "System").replace("project", "system"));
        } else if (lower.contains("degree") || lower.contains("b.tech") || lower.contains("b.e") || lower.contains("b.com") || lower.contains("bba") || lower.contains("education") || lower.contains("bachelor") || lower.contains("master")) {
            category = RequirementCategory.EDUCATION;
        } else if (lower.contains("experience") || lower.contains("years") || lower.contains("internship")) {
            category = RequirementCategory.EXPERIENCE;
        } else if (lower.contains("certified") || lower.contains("certification") || lower.contains("license")) {
            category = RequirementCategory.CERTIFICATION;
        } else if (lower.contains("communication") || lower.contains("leadership") || lower.contains("teamwork") || lower.contains("strategy")) {
            category = RequirementCategory.SKILL_SOFT;
        }

        // Generate natural variations as synonyms (e.g. "AutoCAD" -> "autocad", "cad")
        synonyms.add(lower);
        if (cleanName.contains(" ")) {
            synonyms.add(cleanName.replaceAll("\\s+", ""));
        }

        return new Requirement(cleanName, category, importance, isMandatory, synonyms);
    }

    private static void parseCommaSeparated(String text, boolean isMandatory, List<Requirement> targetList) {
        String[] parts = text.split("[,;]");
        for (String p : parts) {
            String clean = p.trim().replaceAll("^[-•*]\\s*", "");
            if (!clean.isEmpty() && clean.length() > 1 && !clean.equalsIgnoreCase("and")) {
                targetList.add(createRequirement(clean, isMandatory));
            }
        }
    }

    private static String extractField(String text, String prefix, String defaultValue) {
        for (String line : text.split("\n")) {
            if (line.toUpperCase().startsWith(prefix.toUpperCase())) {
                String val = line.substring(prefix.length()).trim();
                if (!val.isEmpty()) return val;
            }
        }
        return defaultValue;
    }
}
