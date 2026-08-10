package com.resumeproof.server;

import com.resumeproof.engine.*;
import com.resumeproof.model.*;
import com.resumeproof.parser.JDRequirementExtractor;
import com.resumeproof.parser.ResumeTextParser;
import com.resumeproof.storage.DataStorageManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ApiHandler implements HttpHandler {

    private List<CandidateProfile> candidatePool;
    private JobDescription activeJd;
    private List<JobDescription> availableJobs;
    private final DataStorageManager storageManager;

    public ApiHandler(List<CandidateProfile> candidatePool, JobDescription activeJd, List<JobDescription> availableJobs, DataStorageManager storageManager) {
        this.candidatePool = candidatePool;
        this.activeJd = activeJd;
        this.availableJobs = availableJobs;
        this.storageManager = storageManager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();
        String method = exchange.getRequestMethod();

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        String responseJson = "{}";
        int statusCode = 200;

        try {
            if (path.endsWith("/api/jobs") && "GET".equalsIgnoreCase(method)) {
                responseJson = buildJdJson(activeJd);
            } else if (path.endsWith("/api/jobs") && "POST".equalsIgnoreCase(method)) {
                String reqBody = readRequestBody(exchange);
                handleCreateOrSwitchJob(reqBody);
                responseJson = "{\"success\": true, \"activeJob\":" + buildJdJson(activeJd) + "}";
            } else if (path.endsWith("/api/sample-jobs") && "GET".equalsIgnoreCase(method)) {
                responseJson = buildSampleJobsJson(availableJobs);
            } else if (path.endsWith("/api/candidates") && "GET".equalsIgnoreCase(method)) {
                responseJson = buildCandidatePoolJson(candidatePool);
            } else if (path.endsWith("/api/candidates") && "POST".equalsIgnoreCase(method)) {
                String reqBody = readRequestBody(exchange);
                CandidateProfile newCand = handleCreateCandidate(reqBody);
                responseJson = "{\"success\": true, \"candidateId\": \"" + escapeJson(newCand.getId()) + "\"}";
            } else if (path.endsWith("/api/search") && "GET".equalsIgnoreCase(method)) {
                String q = extractQueryParam(query, "q");
                List<CandidateProfile> results = DiscoveryEngine.searchCandidates(candidatePool, q);
                responseJson = buildCandidatePoolJson(results);
            } else if (path.endsWith("/api/analyze")) {
                String modeStr = extractQueryParam(query, "mode");
                if (modeStr.isEmpty()) modeStr = "BALANCED";

                EvaluationMode mode = EvaluationMode.BALANCED;
                try { mode = EvaluationMode.valueOf(modeStr.toUpperCase()); } catch (Exception ignored) {}

                List<EvaluationResult> results = new ArrayList<>();
                for (CandidateProfile candidate : candidatePool) {
                    List<Evidence> evList = EvidenceEngine.extractEvidence(candidate, activeJd);
                    List<Claim> claimList = ClaimVerificationEngine.verifyClaims(candidate);

                    EvaluationResult result = ModeEvaluator.evaluateCandidate(candidate, activeJd, mode, evList);
                    result.setClaimList(claimList);

                    SkillGapEngine.populateSkillGaps(result);
                    List<InterviewQuestion> questions = InterviewIntelligenceEngine.generateQuestions(result, activeJd);
                    result.setInterviewQuestions(questions);

                    String alignment = PreferenceEngine.calculateAlignment(candidate, activeJd);
                    result.setPreferenceAlignment(alignment);

                    results.add(result);
                }

                RankingEngine.rankCandidates(results);
                responseJson = buildResultsJson(results);
            } else {
                responseJson = "{\"error\": \"Invalid API Endpoint\"}";
                statusCode = 404;
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseJson = "{\"error\": \"" + escapeJson(e.getMessage()) + "\"}";
            statusCode = 500;
        }

        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void handleCreateOrSwitchJob(String reqBody) {
        String templateId = extractJsonValue(reqBody, "templateId");
        if (!templateId.isEmpty()) {
            for (JobDescription jd : availableJobs) {
                if (jd.getId().equalsIgnoreCase(templateId)) {
                    this.activeJd = jd;
                    return;
                }
            }
        }

        String title = extractJsonValue(reqBody, "title");
        String company = extractJsonValue(reqBody, "companyName");
        String domain = extractJsonValue(reqBody, "domain");
        String mandatoryText = extractJsonValue(reqBody, "mandatoryText");
        String preferredText = extractJsonValue(reqBody, "preferredText");

        if (!title.isEmpty()) {
            StringBuilder raw = new StringBuilder();
            raw.append("POSITION: ").append(title).append("\n");
            raw.append("COMPANY: ").append(company.isEmpty() ? "Hiring Client" : company).append("\n");
            raw.append("DOMAIN: ").append(domain.isEmpty() ? "General Industry" : domain).append("\n\n");
            raw.append("MANDATORY REQUIREMENTS:\n").append(mandatoryText).append("\n\n");
            raw.append("PREFERRED REQUIREMENTS:\n").append(preferredText).append("\n");

            JobDescription newJd = JDRequirementExtractor.parseJD("custom_" + System.currentTimeMillis(), raw.toString());
            this.activeJd = newJd;
            this.availableJobs.add(newJd);

            if (storageManager != null) {
                storageManager.saveCustomJob(newJd);
            }
        }
    }

    private CandidateProfile handleCreateCandidate(String reqBody) {
        String name = extractJsonValue(reqBody, "name");
        String email = extractJsonValue(reqBody, "email");
        String phone = extractJsonValue(reqBody, "phone");
        String dream = extractJsonValue(reqBody, "dreamCompanies");
        String roles = extractJsonValue(reqBody, "preferredRoles");
        String domains = extractJsonValue(reqBody, "preferredDomains");
        String resumeText = extractJsonValue(reqBody, "rawResumeText");

        StringBuilder raw = new StringBuilder();
        raw.append("NAME: ").append(name.isEmpty() ? "Candidate Profile" : name).append("\n");
        raw.append("EMAIL: ").append(email).append("\n");
        raw.append("PHONE: ").append(phone).append("\n");
        raw.append("DREAM COMPANIES: ").append(dream).append("\n");
        raw.append("PREFERRED ROLE: ").append(roles).append("\n");
        raw.append("PREFERRED DOMAIN: ").append(domains).append("\n\n");
        raw.append("SUMMARY:\n").append(resumeText).append("\n");

        String newId = String.valueOf(candidatePool.size() + 1);
        CandidateProfile profile = ResumeTextParser.parseResume(newId, raw.toString());
        candidatePool.add(profile);

        if (storageManager != null) {
            storageManager.saveCandidateProfile(profile);
        }

        return profile;
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    private String extractJsonValue(String json, String key) {
        if (json == null || json.isEmpty()) return "";
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1).replace("\\n", "\n").replace("\\\"", "\"");
        }
        return "";
    }

    private String extractQueryParam(String query, String param) {
        if (query == null || query.isEmpty()) return "";
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].equalsIgnoreCase(param)) {
                return kv[1];
            }
        }
        return "";
    }

    private String buildSampleJobsJson(List<JobDescription> jobs) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < jobs.size(); i++) {
            JobDescription j = jobs.get(i);
            sb.append("{");
            sb.append("\"id\":\"").append(escapeJson(j.getId())).append("\",");
            sb.append("\"title\":\"").append(escapeJson(j.getTitle())).append("\",");
            sb.append("\"companyName\":\"").append(escapeJson(j.getCompanyName())).append("\",");
            sb.append("\"domain\":\"").append(escapeJson(j.getDomain())).append("\"");
            sb.append("}").append(i < jobs.size() - 1 ? "," : "");
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildJdJson(JobDescription jd) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(jd.getId())).append("\",");
        sb.append("\"title\":\"").append(escapeJson(jd.getTitle())).append("\",");
        sb.append("\"companyName\":\"").append(escapeJson(jd.getCompanyName())).append("\",");
        sb.append("\"domain\":\"").append(escapeJson(jd.getDomain())).append("\",");
        sb.append("\"mandatoryRequirements\":[");
        for (int i = 0; i < jd.getMandatoryRequirements().size(); i++) {
            Requirement req = jd.getMandatoryRequirements().get(i);
            sb.append("{");
            sb.append("\"name\":\"").append(escapeJson(req.getName())).append("\",");
            sb.append("\"category\":\"").append(escapeJson(req.getCategory().getLabel())).append("\",");
            sb.append("\"importance\":\"").append(escapeJson(req.getImportance().getLabel())).append("\"");
            sb.append("}").append(i < jd.getMandatoryRequirements().size() - 1 ? "," : "");
        }
        sb.append("],\"preferredRequirements\":[");
        for (int i = 0; i < jd.getPreferredRequirements().size(); i++) {
            Requirement req = jd.getPreferredRequirements().get(i);
            sb.append("{");
            sb.append("\"name\":\"").append(escapeJson(req.getName())).append("\",");
            sb.append("\"category\":\"").append(escapeJson(req.getCategory().getLabel())).append("\",");
            sb.append("\"importance\":\"").append(escapeJson(req.getImportance().getLabel())).append("\"");
            sb.append("}").append(i < jd.getPreferredRequirements().size() - 1 ? "," : "");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String buildCandidatePoolJson(List<CandidateProfile> pool) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < pool.size(); i++) {
            CandidateProfile c = pool.get(i);
            sb.append("{");
            sb.append("\"id\":\"").append(escapeJson(c.getId())).append("\",");
            sb.append("\"name\":\"").append(escapeJson(c.getName())).append("\",");
            sb.append("\"email\":\"").append(escapeJson(c.getEmail())).append("\",");
            sb.append("\"phone\":\"").append(escapeJson(c.getPhone())).append("\",");
            sb.append("\"dreamCompanies\":[").append(listToJsonArray(c.getPreferences().getDreamCompanies())).append("],");
            sb.append("\"preferredRoles\":[").append(listToJsonArray(c.getPreferences().getPreferredRoles())).append("],");
            sb.append("\"preferredDomains\":[").append(listToJsonArray(c.getPreferences().getPreferredDomains())).append("]");
            sb.append("}").append(i < pool.size() - 1 ? "," : "");
        }
        sb.append("]");
        return sb.toString();
    }

    private String buildResultsJson(List<EvaluationResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < results.size(); i++) {
            EvaluationResult r = results.get(i);
            sb.append("{");
            sb.append("\"candidateId\":\"").append(escapeJson(r.getCandidateId())).append("\",");
            sb.append("\"candidateName\":\"").append(escapeJson(r.getCandidateName())).append("\",");
            sb.append("\"mode\":\"").append(escapeJson(r.getMode().name())).append("\",");
            sb.append("\"modeTitle\":\"").append(escapeJson(r.getMode().getTitle())).append("\",");
            sb.append("\"qualificationScore\":").append(r.getQualificationScore()).append(",");
            sb.append("\"isShortlisted\":").append(r.isShortlisted()).append(",");
            sb.append("\"rejectionReason\":\"").append(escapeJson(r.getRejectionReason())).append("\",");
            sb.append("\"preferenceAlignment\":\"").append(escapeJson(r.getPreferenceAlignment())).append("\",");
            sb.append("\"explanationText\":\"").append(escapeJson(r.getExplanationText())).append("\",");

            // Evidence List
            sb.append("\"evidenceList\":[");
            for (int j = 0; j < r.getEvidenceList().size(); j++) {
                Evidence ev = r.getEvidenceList().get(j);
                sb.append("{");
                sb.append("\"requirementName\":\"").append(escapeJson(ev.getRequirementName())).append("\",");
                sb.append("\"strength\":\"").append(escapeJson(ev.getStrength().name())).append("\",");
                sb.append("\"strengthLabel\":\"").append(escapeJson(ev.getStrength().getLabel())).append("\",");
                sb.append("\"icon\":\"").append(escapeJson(ev.getStrength().getIcon())).append("\",");
                sb.append("\"snippet\":\"").append(escapeJson(ev.getMatchedSnippet())).append("\",");
                sb.append("\"sourceSection\":\"").append(escapeJson(ev.getSourceSection())).append("\",");
                sb.append("\"contextNote\":\"").append(escapeJson(ev.getContextNote())).append("\"");
                sb.append("}").append(j < r.getEvidenceList().size() - 1 ? "," : "");
            }
            sb.append("],");

            // Claims
            sb.append("\"claims\":[");
            for (int j = 0; j < r.getClaimList().size(); j++) {
                Claim cl = r.getClaimList().get(j);
                sb.append("{");
                sb.append("\"claimText\":\"").append(escapeJson(cl.getClaimText())).append("\",");
                sb.append("\"strength\":\"").append(escapeJson(cl.getStrength().getLabel())).append("\",");
                sb.append("\"verificationScore\":").append(cl.getVerificationScore()).append(",");
                sb.append("\"action\":\"").append(escapeJson(cl.getRecommendedAction())).append("\",");
                sb.append("\"checklist\":[").append(listToJsonArray(cl.getChecklist())).append("]");
                sb.append("}").append(j < r.getClaimList().size() - 1 ? "," : "");
            }
            sb.append("],");

            // Skill Gaps
            sb.append("\"strongAreas\":[").append(listToJsonArray(r.getStrongAreas())).append("],");
            sb.append("\"partialAreas\":[").append(listToJsonArray(r.getPartialAreas())).append("],");
            sb.append("\"missingRequirements\":[").append(listToJsonArray(r.getMissingRequirements())).append("],");
            sb.append("\"verificationFlags\":[").append(listToJsonArray(r.getVerificationFlags())).append("],");

            // Interview Questions
            sb.append("\"interviewQuestions\":[");
            for (int j = 0; j < r.getInterviewQuestions().size(); j++) {
                InterviewQuestion q = r.getInterviewQuestions().get(j);
                sb.append("{");
                sb.append("\"target\":\"").append(escapeJson(q.getTargetRequirementOrClaim())).append("\",");
                sb.append("\"question\":\"").append(escapeJson(q.getQuestionText())).append("\",");
                sb.append("\"category\":\"").append(escapeJson(q.getCategory())).append("\",");
                sb.append("\"priority\":").append(q.getPriority());
                sb.append("}").append(j < r.getInterviewQuestions().size() - 1 ? "," : "");
            }
            sb.append("]");

            sb.append("}").append(i < results.size() - 1 ? "," : "");
        }
        sb.append("]");
        return sb.toString();
    }

    private String listToJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
            if (i < list.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
