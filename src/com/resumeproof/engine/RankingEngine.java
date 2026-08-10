package com.resumeproof.engine;

import com.resumeproof.model.EvaluationResult;
import com.resumeproof.model.Evidence;
import com.resumeproof.model.EvidenceStrength;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RankingEngine {

    public static void rankCandidates(List<EvaluationResult> results) {
        Collections.sort(results, new Comparator<EvaluationResult>() {
            @Override
            public int compare(EvaluationResult r1, EvaluationResult r2) {
                // Shortlisted candidates rank higher than rejected candidates
                if (r1.isShortlisted() && !r2.isShortlisted()) return -1;
                if (!r1.isShortlisted() && r2.isShortlisted()) return 1;

                // Higher score ranks first
                int scoreComp = Double.compare(r2.getQualificationScore(), r1.getQualificationScore());
                if (scoreComp != 0) return scoreComp;

                // Tie-breaker: Count strong evidence
                int s1 = countStrongEvidence(r1);
                int s2 = countStrongEvidence(r2);
                return Integer.compare(s2, s1);
            }
        });

        // Generate detailed explanation for each candidate ranking
        for (EvaluationResult res : results) {
            res.setExplanationText(generateExplanation(res));
        }
    }

    private static int countStrongEvidence(EvaluationResult result) {
        int count = 0;
        for (Evidence ev : result.getEvidenceList()) {
            if (ev.getStrength() == EvidenceStrength.STRONG) {
                count++;
            }
        }
        return count;
    }

    private static String generateExplanation(EvaluationResult result) {
        StringBuilder sb = new StringBuilder();
        if (!result.isShortlisted()) {
            sb.append("Not shortlisted under ").append(result.getMode().getTitle()).append(". ");
            sb.append("Reason: ").append(result.getRejectionReason()).append(".");
            return sb.toString();
        }

        sb.append("Candidate achieved a ").append((int) result.getQualificationScore()).append("% qualification score under ")
          .append(result.getMode().getTitle()).append(".\n");

        int strongCount = countStrongEvidence(result);
        sb.append("- Demonstrates strong evidence for ").append(strongCount).append(" requirement(s).\n");

        if (!result.getStrongAreas().isEmpty()) {
            sb.append("- Key Strengths: ").append(String.join(", ", result.getStrongAreas())).append(".\n");
        }

        if (!result.getMissingRequirements().isEmpty()) {
            sb.append("- Missing Requirements: ").append(String.join(", ", result.getMissingRequirements())).append(".\n");
        }

        if (!result.getVerificationFlags().isEmpty()) {
            sb.append("- Clarification Flags: ").append(String.join(", ", result.getVerificationFlags())).append(".\n");
        }

        return sb.toString();
    }
}
