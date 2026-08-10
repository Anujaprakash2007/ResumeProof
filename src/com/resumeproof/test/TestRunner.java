package com.resumeproof.test;

import com.resumeproof.engine.*;
import com.resumeproof.model.*;
import com.resumeproof.parser.JDRequirementExtractor;
import com.resumeproof.parser.ResumeTextParser;

import java.util.*;

public class TestRunner {

    public static void runAllTests() {
        System.out.println("=================================================");
        System.out.println("  RESUMEPROOF AUTOMATED TEST SUITE (TESTS 1-10)  ");
        System.out.println("=================================================");

        int passed = 0;
        int total = 10;

        passed += test1_StrongPython() ? 1 : 0;
        passed += test2_WeakPython() ? 1 : 0;
        passed += test3_NegationPython() ? 1 : 0;
        passed += test4_StrongMLProject() ? 1 : 0;
        passed += test5_FamiliarML() ? 1 : 0;
        passed += test6_StrictFilterRejection() ? 1 : 0;
        passed += test7_BalancedPreferredMissing() ? 1 : 0;
        passed += test8_BestMatchInclusion() ? 1 : 0;
        passed += test9_EvidenceStrengthRanking() ? 1 : 0;
        passed += test10_ContextRuleMatching() ? 1 : 0;

        System.out.println("\n-------------------------------------------------");
        System.out.printf("  TEST RESULTS: %d / %d PASSED (%.1f%%)\n", passed, total, (passed * 100.0 / total));
        System.out.println("=================================================\n");
    }

    private static boolean test1_StrongPython() {
        EvidenceStrength str = ContextEngine.classifyContext("Experienced in Python programming.", "WORK_EXPERIENCE", "python");
        boolean ok = (str == EvidenceStrength.STRONG || str == EvidenceStrength.PARTIAL);
        printResult("Test 1: 'Experienced in Python.' -> Strong/Partial Evidence", ok, str.getLabel());
        return ok;
    }

    private static boolean test2_WeakPython() {
        EvidenceStrength str = ContextEngine.classifyContext("Interested in learning Python.", "SUMMARY", "python");
        boolean ok = (str == EvidenceStrength.UNCLEAR || str == EvidenceStrength.PARTIAL);
        printResult("Test 2: 'Interested in learning Python.' -> Unclear/Partial Evidence", ok, str.getLabel());
        return ok;
    }

    private static boolean test3_NegationPython() {
        EvidenceStrength str = ContextEngine.classifyContext("No experience with Python development.", "SUMMARY", "python");
        boolean ok = (str == EvidenceStrength.NEGATIVE);
        printResult("Test 3: 'No experience with Python.' -> Negative / Not Demonstrated", ok, str.getLabel());
        return ok;
    }

    private static boolean test4_StrongMLProject() {
        EvidenceStrength str = ContextEngine.classifyContext("Developed a machine learning project using Python.", "PROJECTS", "python");
        boolean ok = (str == EvidenceStrength.STRONG);
        printResult("Test 4: 'Developed ML project in Python' -> Strong Evidence", ok, str.getLabel());
        return ok;
    }

    private static boolean test5_FamiliarML() {
        EvidenceStrength str = ContextEngine.classifyContext("Familiar with machine learning concepts.", "SUMMARY", "machine learning");
        boolean ok = (str == EvidenceStrength.UNCLEAR);
        printResult("Test 5: 'Familiar with machine learning.' -> Unclear / Needs Verification", ok, str.getLabel());
        return ok;
    }

    private static boolean test6_StrictFilterRejection() {
        CandidateProfile c = ResumeTextParser.parseResume("rahul", getRahulResume());
        JobDescription jd = JDRequirementExtractor.parseJD("job1", getSampleJD());

        List<Evidence> evList = EvidenceEngine.extractEvidence(c, jd);
        EvaluationResult res = ModeEvaluator.evaluateCandidate(c, jd, EvaluationMode.STRICT, evList);

        boolean ok = (!res.isShortlisted() && res.getRejectionReason().contains("AI/ML project"));
        printResult("Test 6: Missing mandatory AI/ML project in STRICT mode -> Rejected", ok, "Shortlisted=" + res.isShortlisted() + ", Reason=" + res.getRejectionReason());
        return ok;
    }

    private static boolean test7_BalancedPreferredMissing() {
        CandidateProfile c = ResumeTextParser.parseResume("anuja", getAnujaResume());
        JobDescription jd = JDRequirementExtractor.parseJD("job1", getSampleJD());

        List<Evidence> evList = EvidenceEngine.extractEvidence(c, jd);
        EvaluationResult res = ModeEvaluator.evaluateCandidate(c, jd, EvaluationMode.BALANCED, evList);

        boolean ok = (res.isShortlisted() && res.getQualificationScore() > 70);
        printResult("Test 7: Missing preferred requirements in BALANCED mode -> Shortlisted with score", ok, "Score=" + res.getQualificationScore());
        return ok;
    }

    private static boolean test8_BestMatchInclusion() {
        CandidateProfile c = ResumeTextParser.parseResume("rahul", getRahulResume());
        JobDescription jd = JDRequirementExtractor.parseJD("job1", getSampleJD());

        List<Evidence> evList = EvidenceEngine.extractEvidence(c, jd);
        EvaluationResult res = ModeEvaluator.evaluateCandidate(c, jd, EvaluationMode.BEST_MATCH, evList);

        boolean ok = (res.isShortlisted());
        printResult("Test 8: BEST MATCH mode preserves candidate in ranked list", ok, "Shortlisted=" + res.isShortlisted() + ", Score=" + res.getQualificationScore());
        return ok;
    }

    private static boolean test9_EvidenceStrengthRanking() {
        CandidateProfile c1 = ResumeTextParser.parseResume("sanjana", getSanjanaResume());
        CandidateProfile c2 = ResumeTextParser.parseResume("priya", getPriyaResume());
        JobDescription jd = JDRequirementExtractor.parseJD("job1", getSampleJD());

        List<Evidence> ev1 = EvidenceEngine.extractEvidence(c1, jd);
        List<Evidence> ev2 = EvidenceEngine.extractEvidence(c2, jd);

        EvaluationResult r1 = ModeEvaluator.evaluateCandidate(c1, jd, EvaluationMode.BEST_MATCH, ev1);
        EvaluationResult r2 = ModeEvaluator.evaluateCandidate(c2, jd, EvaluationMode.BEST_MATCH, ev2);

        List<EvaluationResult> list = Arrays.asList(r1, r2);
        RankingEngine.rankCandidates(list);

        boolean ok = (list.get(0).getCandidateName().equals("Sanjana R"));
        printResult("Test 9: Stronger evidence candidate ranks higher than weak evidence candidate", ok, "Rank 1: " + list.get(0).getCandidateName());
        return ok;
    }

    private static boolean test10_ContextRuleMatching() {
        Requirement req = new Requirement("AI/ML project", RequirementCategory.PROJECT, Importance.HIGH, true, Arrays.asList("ai project", "prediction system"));
        CandidateProfile c = ResumeTextParser.parseResume("anuja", getAnujaResume());

        Evidence ev = EvidenceEngine.findEvidenceForRequirement(c, req);
        boolean ok = (ev.getStrength() == EvidenceStrength.STRONG && ev.getMatchedSnippet().contains("Disease Prediction System"));
        printResult("Test 10: Rule-based domain synonym matching for project requirement", ok, "Matched: " + ev.getMatchedSnippet());
        return ok;
    }

    private static void printResult(String testName, boolean passed, String detail) {
        System.out.printf("[%s] %s | Detail: %s\n", passed ? "PASS" : "FAIL", testName, detail);
    }

    private static String getSampleJD() {
        return "POSITION: AI/ML Intern\n\nMANDATORY REQUIREMENTS:\n- Python\n- Machine Learning\n- At least 1 AI/ML project\n\nPREFERRED REQUIREMENTS:\n- SQL\n- Deep Learning\n- Computer Vision\n- Git";
    }

    private static String getAnujaResume() {
        return "NAME: Anuja P\nTECHNICAL SKILLS:\n- Languages: Python, SQL\n- Concepts: Machine Learning\nPROJECTS:\n- Disease Prediction System using Machine Learning\n  Developed a disease prediction system using machine learning algorithms in Python.";
    }

    private static String getRahulResume() {
        return "NAME: Rahul K\nTECHNICAL SKILLS:\n- Languages: Python, Java, SQL\n- Concepts: Machine Learning\nPROJECTS:\n- E-Commerce Web Portal\n  Built a web portal using HTML, CSS, MySQL.";
    }

    private static String getPriyaResume() {
        return "NAME: Priya S\nSUMMARY:\nInterested in learning Python. Familiar with machine learning concepts from tutorials.";
    }

    private static String getSanjanaResume() {
        return "NAME: Sanjana R\nTECHNICAL SKILLS:\n- Languages: Python, PyTorch, OpenCV\nPROJECTS:\n- Real-Time Traffic Sign Detection using Computer Vision & Deep Learning\n  Engineered an object detection system using OpenCV and PyTorch.";
    }

    public static void main(String[] args) {
        runAllTests();
    }
}
