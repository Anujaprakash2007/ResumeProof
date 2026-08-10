package com.resumeproof.engine;

import com.resumeproof.model.EvidenceStrength;

import java.util.Arrays;
import java.util.List;

public class ContextEngine {

    private static final List<String> NEGATION_TRIGGERS = Arrays.asList(
            "no experience", "never used", "not familiar", "lack of experience",
            "without experience", "have not worked", "no exposure", "didn't use", "don't know"
    );

    private static final List<String> STRONG_ACTION_VERBS = Arrays.asList(
            "developed", "built", "engineered", "implemented", "created",
            "designed", "deployed", "architected", "spearheaded", "trained",
            "analyzed", "processed", "achieved", "evaluated", "worked on"
    );

    private static final List<String> COURSEWORK_TRIGGERS = Arrays.asList(
            "coursework", "academic course", "studied", "learned in class",
            "curriculum", "training", "online course", "tutorial"
    );

    private static final List<String> WEAK_TRIGGERS = Arrays.asList(
            "interested in", "familiar with", "learning", "exposure to",
            "basic knowledge", "knowledge of", "aspiring"
    );

    public static boolean isNegated(String sentence, String keyword) {
        String lowerSentence = sentence.toLowerCase();
        for (String trigger : NEGATION_TRIGGERS) {
            if (lowerSentence.contains(trigger)) {
                return true;
            }
        }
        return false;
    }

    public static EvidenceStrength classifyContext(String sentence, String sectionName, String keyword) {
        if (sentence == null || sentence.trim().isEmpty()) {
            return EvidenceStrength.NOT_FOUND;
        }

        String lowerSentence = sentence.toLowerCase();

        // 1. Check for Negation first
        if (isNegated(lowerSentence, keyword)) {
            return EvidenceStrength.NEGATIVE;
        }

        // 2. Check for Strong Action Verbs in Projects or Work Experience
        boolean hasStrongVerb = false;
        for (String verb : STRONG_ACTION_VERBS) {
            if (lowerSentence.contains(verb)) {
                hasStrongVerb = true;
                break;
            }
        }

        if (hasStrongVerb && ("PROJECTS".equals(sectionName) || "WORK_EXPERIENCE".equals(sectionName) || lowerSentence.contains("project") || lowerSentence.contains("system"))) {
            return EvidenceStrength.STRONG;
        }

        // 3. Check Coursework context
        for (String cw : COURSEWORK_TRIGGERS) {
            if (lowerSentence.contains(cw)) {
                return EvidenceStrength.PARTIAL;
            }
        }

        // 4. Check Weak context
        for (String weak : WEAK_TRIGGERS) {
            if (lowerSentence.contains(weak)) {
                return EvidenceStrength.UNCLEAR;
            }
        }

        // Default: If keyword appears in Skills section or general text with strong verb
        if (hasStrongVerb) {
            return EvidenceStrength.STRONG;
        } else if ("SKILLS".equals(sectionName)) {
            return EvidenceStrength.PARTIAL;
        }

        return EvidenceStrength.UNCLEAR;
    }
}
