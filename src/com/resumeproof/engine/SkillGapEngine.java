package com.resumeproof.engine;

import com.resumeproof.model.EvaluationResult;
import com.resumeproof.model.Evidence;
import com.resumeproof.model.EvidenceStrength;

import java.util.ArrayList;
import java.util.List;

public class SkillGapEngine {

    public static void populateSkillGaps(EvaluationResult result) {
        List<String> strong = new ArrayList<>();
        List<String> partial = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        List<String> verify = new ArrayList<>();

        for (Evidence ev : result.getEvidenceList()) {
            EvidenceStrength str = ev.getStrength();
            String reqName = ev.getRequirementName();

            if (str == EvidenceStrength.STRONG) {
                strong.add(reqName);
            } else if (str == EvidenceStrength.PARTIAL) {
                partial.add(reqName);
            } else if (str == EvidenceStrength.UNCLEAR) {
                verify.add(reqName + " (Vague/Passive context)");
            } else if (str == EvidenceStrength.NEGATIVE) {
                missing.add(reqName + " (Not Demonstrated)");
            } else {
                missing.add(reqName);
            }
        }

        result.setStrongAreas(strong);
        result.setPartialAreas(partial);
        result.setMissingRequirements(missing);
        result.setVerificationFlags(verify);
    }
}
