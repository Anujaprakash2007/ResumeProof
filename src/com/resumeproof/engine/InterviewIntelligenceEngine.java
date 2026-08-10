package com.resumeproof.engine;

import com.resumeproof.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * DOMAIN-INDEPENDENT Interview Intelligence Engine.
 * Generates targeted, rule-based technical and domain interview questions
 * derived from evidence confidence, accomplishment claim verification,
 * missing requirements, and candidate project history.
 */
public class InterviewIntelligenceEngine {

    public static List<InterviewQuestion> generateQuestions(EvaluationResult result, JobDescription jd) {
        List<InterviewQuestion> questions = new ArrayList<>();

        // Priority 1: Unclear or Partial High-Importance Requirements
        for (Evidence ev : result.getEvidenceList()) {
            String reqName = ev.getRequirementName();
            Requirement req = findRequirement(reqName, jd);

            if (req != null && req.getImportance() == Importance.HIGH) {
                if (ev.getStrength() == EvidenceStrength.UNCLEAR || ev.getStrength() == EvidenceStrength.PARTIAL) {
                    questions.add(new InterviewQuestion(
                            reqName,
                            "You noted experience with '" + reqName + "'. Could you describe a practical project or workflow where you applied '" + reqName + "' and explain your key decisions?",
                            "Domain Expertise Deep-Dive",
                            1
                    ));
                } else if (ev.getStrength() == EvidenceStrength.STRONG) {
                    questions.add(new InterviewQuestion(
                            reqName,
                            "How did you measure, evaluate, and optimize your implementation of '" + reqName + "' in your recent project?",
                            "Mastery & Optimization",
                            3
                    ));
                }
            }
        }

        // Priority 2: Unverified Accomplishment Claims
        for (Claim claim : result.getClaimList()) {
            if (claim.getStrength() == EvidenceStrength.UNCLEAR || claim.getStrength() == EvidenceStrength.PARTIAL) {
                questions.add(new InterviewQuestion(
                        claim.getClaimText(),
                        "Regarding your accomplishment claim: '" + truncate(claim.getClaimText(), 60) + "', can you detail the underlying methodology, tools used, and your specific individual contribution?",
                        "Claim Verification",
                        1
                ));
            }
        }

        // Priority 3: Missing Requirements
        for (String missingReq : result.getMissingRequirements()) {
            String cleanName = missingReq.replaceAll("\\s*\\(.*\\)", "");
            questions.add(new InterviewQuestion(
                    cleanName,
                    "The " + jd.getTitle() + " role emphasizes '" + cleanName + "', which was limited on your resume. What is your current practical exposure to '" + cleanName + "'?",
                    "Skill Gap Assessment",
                    2
            ));
        }

        return questions;
    }

    private static Requirement findRequirement(String name, JobDescription jd) {
        for (Requirement req : jd.getAllRequirements()) {
            if (req.getName().equalsIgnoreCase(name)) {
                return req;
            }
        }
        return null;
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() <= maxLen ? str : str.substring(0, maxLen) + "...";
    }
}
