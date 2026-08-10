package com.resumeproof.server;

import com.resumeproof.model.CandidateProfile;
import com.resumeproof.model.JobDescription;
import com.resumeproof.storage.DataStorageManager;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

public class HttpServerHost {

    private final int port;
    private final String webRoot;
    private HttpServer server;

    public HttpServerHost(int port, String webRoot) {
        this.port = port;
        this.webRoot = webRoot;
    }

    public void start(List<CandidateProfile> candidatePool, JobDescription activeJd, List<JobDescription> availableJobs, DataStorageManager storageManager) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        ApiHandler apiHandler = new ApiHandler(candidatePool, activeJd, availableJobs, storageManager);
        StaticFileHandler staticHandler = new StaticFileHandler(webRoot);

        server.createContext("/api", apiHandler);
        server.createContext("/", staticHandler);

        server.setExecutor(null);
        server.start();

        System.out.println("=================================================");
        System.out.println("  RESUMEPROOF RECRUITMENT INTELLIGENCE SERVER   ");
        System.out.println("=================================================");
        System.out.println("  Server running at: http://localhost:" + port);
        System.out.println("  Web UI directory : " + webRoot);
        System.out.println("  Single Source    : Java SE 17 Engine Core");
        System.out.println("  Constraint       : ZERO EXTERNAL APIS / PACKAGES");
        System.out.println("=================================================\n");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}
