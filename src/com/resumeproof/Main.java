package com.resumeproof;

import com.resumeproof.model.CandidateProfile;
import com.resumeproof.model.JobDescription;
import com.resumeproof.server.HttpServerHost;
import com.resumeproof.storage.DataStorageManager;
import com.resumeproof.test.TestRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("Initializing ResumeProof Single Source Engine...");
        System.out.println("=================================================\n");

        // 1. Execute Automated Core Test Suite (Tests 1-10)
        TestRunner.runAllTests();

        // 2. Initialize Data Storage & Load Sample Datasets
        String projectRoot = System.getProperty("user.dir");
        String dataDir = projectRoot + File.separator + "data";
        String webDir = projectRoot + File.separator + "web";

        DataStorageManager storageManager = new DataStorageManager(dataDir);
        List<JobDescription> availableJobs = storageManager.loadAllJobs();
        List<CandidateProfile> candidatePool = storageManager.loadAllCandidates();

        JobDescription activeJd = availableJobs.isEmpty() ? null : availableJobs.get(0);

        System.out.printf("[CORE ENGINE] Loaded %d Domain Job Templates\n", availableJobs.size());
        if (activeJd != null) {
            System.out.printf("[CORE ENGINE] Active hiring role: '%s' (%s)\n", activeJd.getTitle(), activeJd.getDomain());
        }
        System.out.printf("[CORE ENGINE] Candidate Pool Size: %d candidates\n\n", candidatePool.size());

        // 3. Launch Embedded Java SE 17 HTTP Server
        int port = 8080;
        try {
            HttpServerHost serverHost = new HttpServerHost(port, webDir);
            serverHost.start(candidatePool, activeJd, availableJobs, storageManager);
        } catch (IOException e) {
            System.err.println("Failed to start embedded HTTP server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
