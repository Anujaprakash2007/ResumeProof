package com.resumeproof.storage;

import com.resumeproof.model.CandidateProfile;
import com.resumeproof.model.JobDescription;
import com.resumeproof.parser.JDRequirementExtractor;
import com.resumeproof.parser.ResumeTextParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * File-based persistence manager built with ZERO third-party libraries using ONLY standard Java File I/O.
 * Persists recruiter-created jobs and submitted candidate profiles locally in the data directory.
 */
public class DataStorageManager {

    private final String dataDir;

    public DataStorageManager(String dataDir) {
        this.dataDir = dataDir;
        ensureDirectoryExists(dataDir + File.separator + "job_descriptions");
        ensureDirectoryExists(dataDir + File.separator + "resumes");
    }

    private void ensureDirectoryExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public List<JobDescription> loadAllJobs() {
        List<JobDescription> jobs = new ArrayList<>();
        File jdFolder = new File(dataDir + File.separator + "job_descriptions");
        if (jdFolder.exists() && jdFolder.isDirectory()) {
            File[] files = jdFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
            if (files != null) {
                int id = 1;
                for (File file : files) {
                    try {
                        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                        JobDescription jd = JDRequirementExtractor.parseJD("jd_" + (id++), text);
                        jobs.add(jd);
                    } catch (IOException e) {
                        System.err.println("Error reading job file " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        return jobs;
    }

    public List<CandidateProfile> loadAllCandidates() {
        List<CandidateProfile> candidates = new ArrayList<>();
        File resumeFolder = new File(dataDir + File.separator + "resumes");
        if (resumeFolder.exists() && resumeFolder.isDirectory()) {
            File[] files = resumeFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
            if (files != null) {
                int id = 1;
                for (File file : files) {
                    try {
                        String text = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                        CandidateProfile cand = ResumeTextParser.parseResume(String.valueOf(id++), text);
                        candidates.add(cand);
                    } catch (IOException e) {
                        System.err.println("Error reading resume file " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        return candidates;
    }

    public void saveCustomJob(JobDescription jd) {
        String filename = "job_custom_" + System.currentTimeMillis() + ".txt";
        File file = new File(dataDir + File.separator + "job_descriptions" + File.separator + filename);

        StringBuilder sb = new StringBuilder();
        sb.append("POSITION: ").append(jd.getTitle()).append("\n");
        sb.append("COMPANY: ").append(jd.getCompanyName()).append("\n");
        sb.append("DOMAIN: ").append(jd.getDomain()).append("\n\n");

        sb.append("MANDATORY REQUIREMENTS:\n");
        for (var req : jd.getMandatoryRequirements()) {
            sb.append("- ").append(req.getName()).append("\n");
        }

        sb.append("\nPREFERRED REQUIREMENTS:\n");
        for (var req : jd.getPreferredRequirements()) {
            sb.append("- ").append(req.getName()).append("\n");
        }

        try {
            Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("[STORAGE] Saved custom job to " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving job file: " + e.getMessage());
        }
    }

    public void saveCandidateProfile(CandidateProfile candidate) {
        String filename = "resume_" + candidate.getName().toLowerCase().replaceAll("[^a-z0-9]", "_") + "_" + System.currentTimeMillis() + ".txt";
        File file = new File(dataDir + File.separator + "resumes" + File.separator + filename);

        StringBuilder sb = new StringBuilder();
        sb.append("NAME: ").append(candidate.getName()).append("\n");
        sb.append("EMAIL: ").append(candidate.getEmail()).append("\n");
        sb.append("PHONE: ").append(candidate.getPhone()).append("\n");

        if (candidate.getPreferences() != null) {
            if (!candidate.getPreferences().getPreferredRoles().isEmpty()) {
                sb.append("PREFERRED ROLE: ").append(String.join(", ", candidate.getPreferences().getPreferredRoles())).append("\n");
            }
            if (!candidate.getPreferences().getPreferredDomains().isEmpty()) {
                sb.append("PREFERRED DOMAIN: ").append(String.join(", ", candidate.getPreferences().getPreferredDomains())).append("\n");
            }
            if (!candidate.getPreferences().getDreamCompanies().isEmpty()) {
                sb.append("DREAM COMPANIES: ").append(String.join(", ", candidate.getPreferences().getDreamCompanies())).append("\n");
            }
        }

        sb.append("\nSUMMARY:\n").append(candidate.getRawResumeText()).append("\n");

        try {
            Files.write(file.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("[STORAGE] Saved candidate profile to " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving candidate file: " + e.getMessage());
        }
    }
}
