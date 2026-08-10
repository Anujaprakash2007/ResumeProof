package com.resumeproof.engine;

import com.resumeproof.model.CandidateProfile;
import com.resumeproof.model.Claim;
import com.resumeproof.model.EvidenceStrength;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DOMAIN-INDEPENDENT Claim Verification Engine.
 * Evaluates accomplishment claims against a 5-point verification checklist:
 * 1. Action Verb Present
 * 2. Technology / Tool / Skill Stack Specified
 * 3. Methodology / Model / Process Detail
 * 4. Quantified Metric / Results
 * 5. Production / Deployment / Execution Context
 */
public class ClaimVerificationEngine {

    private static final List<String> ACTION_VERBS = Arrays.asList(
            "developed", "built", "engineered", "designed", "created", "spearheaded",
            "implemented", "architected", "executed", "formulated", "led", "delivered",
            "analyzed", "modeled", "evaluated", "fabricated", "optmized", "drove"
    );

    private static final List<String> METHODOLOGY_KEYWORDS = Arrays.asList(
            "model", "algorithm", "methodology", "process", "strategy", "analysis",
            "design", "framework", "simulation", "architecture", "calculation",
            "technique", "valuation", "dcf", "fea", "pv", "seo", "cnn", "sql",
            "cad", "excel", "survey", "audit", "protocol"
    );

    private static final List<String> DEPLOYMENT_KEYWORDS = Arrays.asList(
            "deployed", "production", "launched", "implemented", "installed",
            "delivered", "published", "commissioned", "operationalized", "executed",
            "validated", "live", "rest api", "onnx", "plant", "site", "portal"
    );

    public static List<Claim> verifyClaims(CandidateProfile candidate) {
        List<Claim> claims = new ArrayList<>();
        String rawText = candidate.getRawResumeText();

        String[] lines = rawText.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() < 25) continue;

            String lower = trimmed.toLowerCase();
            boolean hasAction = false;
            for (String verb : ACTION_VERBS) {
                if (lower.contains(verb)) {
                    hasAction = true;
                    break;
                }
            }

            if (hasAction || lower.contains("achieved") || lower.contains("system") || lower.contains("campaign") || lower.contains("project")) {
                Claim claim = analyzeClaim(trimmed, candidate);
                if (claim != null) {
                    claims.add(claim);
                }
            }
        }

        if (claims.isEmpty()) {
            List<String> defaultChecklist = Arrays.asList(
                    "Project Context: Present",
                    "Tool / Competency Stack: Specified",
                    "Methodology / Process: Limited supporting evidence",
                    "Quantified Results: Insufficient supporting evidence"
            );
            claims.add(new Claim(
                    "Core domain contributions mentioned in profile",
                    EvidenceStrength.PARTIAL,
                    60.0,
                    defaultChecklist,
                    "Clarification recommended during technical interview."
            ));
        }

        return claims;
    }

    private static Claim analyzeClaim(String sentence, CandidateProfile candidate) {
        String lower = sentence.toLowerCase();
        List<String> checklist = new ArrayList<>();
        double score = 0.0;

        // Check 1: Action Verb
        boolean hasVerb = false;
        for (String verb : ACTION_VERBS) {
            if (lower.contains(verb)) {
                hasVerb = true;
                break;
            }
        }
        if (hasVerb) {
            score += 20.0;
            checklist.add("Action Verb: Present");
        } else {
            checklist.add("Action Verb: Missing / Passive");
        }

        // Check 2: Tool / Skill Stack Specified (Dynamic check against candidate skills + general domain terms)
        boolean hasTool = false;
        for (String skill : candidate.getExtractedSkills()) {
            if (!skill.isEmpty() && lower.contains(skill.toLowerCase())) {
                hasTool = true;
                break;
            }
        }
        if (!hasTool) {
            // Fallback check for common tool/tech indicators
            hasTool = lower.contains("using") || lower.contains("with") || lower.contains("in") || lower.contains("via");
        }
        if (hasTool) {
            score += 20.0;
            checklist.add("Domain Tool / Skill Stack: Specified");
        } else {
            checklist.add("Domain Tool / Skill Stack: Missing");
        }

        // Check 3: Methodology / Model / Process Detail
        boolean hasMethod = false;
        for (String method : METHODOLOGY_KEYWORDS) {
            if (lower.contains(method)) {
                hasMethod = true;
                break;
            }
        }
        if (hasMethod) {
            score += 20.0;
            checklist.add("Methodology / Process Detail: Present");
        } else {
            checklist.add("Methodology / Process Detail: Limited supporting evidence");
        }

        // Check 4: Quantified Metric
        boolean hasMetric = lower.matches(".*\\d+%.*") || lower.matches(".*\\d+\\s*(mw|k|m|hours|users|gb|mAP|ctr|accuracy|growth|yield|efficiency|reduction).*");
        if (!hasMetric) {
            hasMetric = lower.contains("percent") || lower.contains("accuracy") || lower.contains("increased") || lower.contains("reduced") || lower.contains("growth");
        }
        if (hasMetric) {
            score += 20.0;
            checklist.add("Quantified Metric / Results: Present");
        } else {
            checklist.add("Quantified Metric / Results: Insufficient supporting evidence");
        }

        // Check 5: Production / Deployment / Execution Context
        boolean hasDeploy = false;
        for (String dep : DEPLOYMENT_KEYWORDS) {
            if (lower.contains(dep)) {
                hasDeploy = true;
                break;
            }
        }
        if (hasDeploy) {
            score += 20.0;
            checklist.add("Execution / Production Context: Present");
        } else {
            checklist.add("Execution / Production Context: Details not specified");
        }

        EvidenceStrength strength;
        String action;
        if (score >= 80.0) {
            strength = EvidenceStrength.STRONG;
            action = "Strong supporting evidence provided.";
        } else if (score >= 50.0) {
            strength = EvidenceStrength.PARTIAL;
            action = "Clarification recommended regarding specific implementation details.";
        } else {
            strength = EvidenceStrength.UNCLEAR;
            action = "Verification recommended during interview.";
        }

        return new Claim(sentence, strength, score, checklist, action);
    }
}
