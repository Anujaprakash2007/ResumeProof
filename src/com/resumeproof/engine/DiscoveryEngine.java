package com.resumeproof.engine;

import com.resumeproof.model.CandidateProfile;

import java.util.ArrayList;
import java.util.List;

public class DiscoveryEngine {

    public static List<CandidateProfile> searchCandidates(List<CandidateProfile> candidates, String query) {
        List<CandidateProfile> matches = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return candidates;
        }

        String[] terms = query.toLowerCase().split("[,+&]|\\band\\b");
        for (CandidateProfile c : candidates) {
            String fullText = c.getRawResumeText().toLowerCase();
            boolean allMatched = true;

            for (String term : terms) {
                String cleanTerm = term.trim();
                if (!cleanTerm.isEmpty() && !fullText.contains(cleanTerm)) {
                    allMatched = false;
                    break;
                }
            }

            if (allMatched) {
                matches.add(c);
            }
        }

        return matches;
    }
}
