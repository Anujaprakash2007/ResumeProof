package com.resumeproof.engine;

import com.resumeproof.model.*;

import java.util.ArrayList;
import java.util.List;

public class ModeEvaluator {

    public static EvaluationResult evaluateCandidate(CandidateProfile candidate, JobDescription jd, EvaluationMode mode, List<Evidence> evidenceList) {
        EvaluationResult result = new EvaluationResult(candidate.getId(), candidate.getName(), mode);
        result.setEvidenceList(evidenceList);

        List<Requirement> mandatory = jd.getMandatoryRequirements();
        List<Requirement> preferred = jd.getPreferredRequirements();

        switch (mode) {
            case STRICT:
                evaluateStrict(result, mandatory, preferred, evidenceList);
                break;
            case BALANCED:
                evaluateBalanced(result, mandatory, preferred, evidenceList);
                break;
            case BEST_MATCH:
                evaluateBestMatch(result, mandatory, preferred, evidenceList);
                break;
        }

        return result;
    }

    private static void evaluateStrict(EvaluationResult result, List<Requirement> mandatory, List<Requirement> preferred, List<Evidence> evidenceList) {
        List<String> failedMandatory = new ArrayList<>();

        for (Requirement req : mandatory) {
            Evidence ev = findEvidence(req.getName(), evidenceList);
            if (ev == null || ev.getStrength() == EvidenceStrength.NOT_FOUND || ev.getStrength() == EvidenceStrength.NEGATIVE || ev.getStrength() == EvidenceStrength.UNCLEAR) {
                failedMandatory.add(req.getName());
            }
        }

        if (!failedMandatory.isEmpty()) {
            result.setShortlisted(false);
            result.setQualificationScore(0.0);
            result.setRejectionReason("Missing mandatory requirement: " + String.join(", ", failedMandatory));
        } else {
            result.setShortlisted(true);
            result.setRejectionReason("");
            double score = calculateWeightedScore(evidenceList, mandatory, preferred);
            result.setQualificationScore(score);
        }
    }

    private static void evaluateBalanced(EvaluationResult result, List<Requirement> mandatory, List<Requirement> preferred, List<Evidence> evidenceList) {
        double mandWeightedSum = 0.0;
        double mandTotalWeight = 0.0;
        List<String> failedMandatory = new ArrayList<>();

        for (Requirement req : mandatory) {
            Evidence ev = findEvidence(req.getName(), evidenceList);
            double w = req.getImportance().getWeight();
            mandTotalWeight += w;

            if (ev != null) {
                mandWeightedSum += w * Math.max(0, ev.getStrength().getMultiplier());
                if (ev.getStrength() == EvidenceStrength.NOT_FOUND || ev.getStrength() == EvidenceStrength.NEGATIVE) {
                    failedMandatory.add(req.getName());
                }
            }
        }

        double mandRatio = mandTotalWeight > 0 ? mandWeightedSum / mandTotalWeight : 1.0;

        double prefWeightedSum = 0.0;
        double prefTotalWeight = 0.0;
        for (Requirement req : preferred) {
            Evidence ev = findEvidence(req.getName(), evidenceList);
            double w = req.getImportance().getWeight();
            prefTotalWeight += w;
            if (ev != null) {
                prefWeightedSum += w * Math.max(0, ev.getStrength().getMultiplier());
            }
        }

        double prefRatio = prefTotalWeight > 0 ? prefWeightedSum / prefTotalWeight : 1.0;

        double finalScore = Math.round((0.65 * mandRatio + 0.35 * prefRatio) * 100.0);

        if (!failedMandatory.isEmpty()) {
            result.setShortlisted(false);
            result.setRejectionReason("Did not satisfy mandatory requirements: " + String.join(", ", failedMandatory));
        } else {
            result.setShortlisted(true);
            result.setRejectionReason("");
        }

        result.setQualificationScore(finalScore);
    }

    private static void evaluateBestMatch(EvaluationResult result, List<Requirement> mandatory, List<Requirement> preferred, List<Evidence> evidenceList) {
        // No hard elimination in Best Match Mode
        result.setShortlisted(true);
        result.setRejectionReason("");

        List<Requirement> all = new ArrayList<>(mandatory);
        all.addAll(preferred);

        double totalWeightedScore = 0.0;
        double totalMaxWeight = 0.0;

        for (Requirement req : all) {
            Evidence ev = findEvidence(req.getName(), evidenceList);
            double impWeight = req.getImportance().getWeight();
            double catWeight = req.getCategory().getCategoryWeight();
            double combinedWeight = impWeight * catWeight;

            totalMaxWeight += combinedWeight;
            if (ev != null) {
                totalWeightedScore += combinedWeight * Math.max(0, ev.getStrength().getMultiplier());
            }
        }

        double score = totalMaxWeight > 0 ? Math.round((totalWeightedScore / totalMaxWeight) * 100.0) : 0.0;
        result.setQualificationScore(score);
    }

    private static double calculateWeightedScore(List<Evidence> evidenceList, List<Requirement> mandatory, List<Requirement> preferred) {
        List<Requirement> all = new ArrayList<>(mandatory);
        all.addAll(preferred);

        double earned = 0.0;
        double possible = 0.0;

        for (Requirement req : all) {
            Evidence ev = findEvidence(req.getName(), evidenceList);
            double w = req.getImportance().getWeight();
            possible += w;
            if (ev != null) {
                earned += w * Math.max(0, ev.getStrength().getMultiplier());
            }
        }

        return possible > 0 ? Math.round((earned / possible) * 100.0) : 0.0;
    }

    private static Evidence findEvidence(String reqName, List<Evidence> evidenceList) {
        for (Evidence ev : evidenceList) {
            if (ev.getRequirementName().equalsIgnoreCase(reqName)) {
                return ev;
            }
        }
        return null;
    }
}
