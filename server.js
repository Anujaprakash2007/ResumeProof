/**
 * ResumeProof Minimal Local Server & API Proxy
 * 
 * ARCHITECTURE DIRECTIVE:
 * Java SE 17 is the SINGLE SOURCE OF TRUTH for all recruitment logic, matching,
 * negation detection, scoring, ranking, modes, claim verification, interview intelligence,
 * and preference alignment.
 * 
 * This file (server.js) maintains ZERO duplicate business logic. It solely serves
 * static frontend web assets from 'web/' and proxies API requests to the authoritative
 * Java SE 17 HTTP engine on port 8080.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 8080;
const JAVA_PORT = process.env.JAVA_PORT || 8080;
const WEB_DIR = path.join(__dirname, 'web');

const MIME_TYPES = {
    '.html': 'text/html; charset=UTF-8',
    '.css': 'text/css; charset=UTF-8',
    '.js': 'text/javascript; charset=UTF-8',
    '.json': 'application/json; charset=UTF-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon'
};

const server = http.createServer((req, res) => {
    const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    const pathname = parsedUrl.pathname;

    // Set CORS Headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    // Proxy API Requests to Java SE 17 Core Engine if port differs, or delegate
    if (pathname.startsWith('/api/')) {
        proxyToJavaEngine(req, res);
        return;
    }

    // Serve Static Frontend Assets
    let filePath = path.join(WEB_DIR, pathname === '/' ? 'index.html' : pathname);

    fs.stat(filePath, (err, stats) => {
        if (err || !stats.isFile()) {
            filePath = path.join(WEB_DIR, 'index.html');
        }

        const ext = path.extname(filePath).toLowerCase();
        const contentType = MIME_TYPES[ext] || 'text/plain';

        fs.readFile(filePath, (readErr, content) => {
            if (readErr) {
                res.writeHead(500, { 'Content-Type': 'text/plain' });
                res.end('500 Internal Server Error');
            } else {
                res.writeHead(200, { 'Content-Type': contentType });
                res.end(content);
            }
        });
    });
});

function proxyToJavaEngine(req, res) {
    const options = {
        hostname: '127.0.0.1',
        port: JAVA_PORT,
        path: req.url,
        method: req.method,
        headers: req.headers
    };

    const proxyReq = http.request(options, (proxyRes) => {
        res.writeHead(proxyRes.statusCode, proxyRes.headers);
        proxyRes.pipe(res, { end: true });
    });

    proxyReq.on('error', (err) => {
        console.error('[PROXY ERROR] Unable to reach Java SE 17 Core Engine:', err.message);
        res.writeHead(502, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            error: 'Java SE 17 Core Engine is offline. Please compile and start Java core engine via build.bat / run.bat.',
            detail: err.message
        }));
    });

    req.pipe(proxyReq, { end: true });
}

server.listen(PORT, () => {
    console.log('=================================================');
    console.log('  RESUMEPROOF MINIMAL FRONTEND SERVER (NODE.JS)  ');
    console.log('=================================================');
    console.log(`  Frontend Web Server : http://localhost:${PORT}`);
    console.log('  Single Source Engine: Java SE 17 Core Engine');
    console.log('  Constraint          : ZERO DUPLICATED BUSINESS LOGIC');
    console.log('=================================================\n');
});
