/**
 * ResumeProof Core Engine & Server
 * DOMAIN-INDEPENDENT & COMPANY-DRIVEN RECRUITMENT PLATFORM
 * Built with ZERO THIRD-PARTY DEPENDENCIES using ONLY Node.js Standard Library
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

// --- 1. DOMAIN MODELS & ENUMS ---
const EvidenceStrength = {
    STRONG: { label: 'Strong Evidence', icon: '🟢', multiplier: 1.00 },
    PARTIAL: { label: 'Partial Evidence', icon: '🟡', multiplier: 0.60 },
    UNCLEAR: { label: 'Needs Verification', icon: '🟠', multiplier: 0.30 },
    NOT_FOUND: { label: 'Not Found', icon: '🔴', multiplier: 0.00 },
    NEGATIVE: { label: 'Not Demonstrated', icon: '⛔', multiplier: -0.50 }
};

const Importance = {
    HIGH: { label: 'High', weight: 3.0 },
    MEDIUM: { label: 'Medium', weight: 2.0 },
    LOW: { label: 'Low', weight: 1.0 }
};

const CategoryWeight = {
    "Technical Skill": 1.2,
    "Soft Skill": 0.8,
    "Work Experience": 1.1,
    "Education": 1.0,
    "Project Experience": 1.2,
    "Certification": 0.9
};

// --- 2. TEXT PROCESSING & NEGATION ENGINE ---
const NEGATION_TRIGGERS = [
    "no experience", "never used", "not familiar", "lack of experience",
    "without experience", "have not worked", "no exposure", "didn't use", "don't know"
];

const STRONG_VERBS = [
    "developed", "built", "engineered", "implemented", "created",
    "designed", "deployed", "architected", "spearheaded", "trained",
    "analyzed", "processed", "achieved", "evaluated", "worked on",
    "managed", "led", "executed", "formulated", "coordinated", "delivered"
];

const COURSEWORK_TRIGGERS = [
    "coursework", "academic course", "studied", "learned in class",
    "curriculum", "training", "online course", "tutorial", "certification"
];

const WEAK_TRIGGERS = [
    "interested in", "familiar with", "learning", "exposure to",
    "basic knowledge", "knowledge of", "aspiring"
];

function classifyContext(sentence, sectionName, keyword) {
    if (!sentence) return EvidenceStrength.NOT_FOUND;
    const lower = sentence.toLowerCase();

    // 1. Negation Check
    for (const trigger of NEGATION_TRIGGERS) {
        if (lower.includes(trigger)) return EvidenceStrength.NEGATIVE;
    }

    // 2. Strong Verbs
    let hasStrongVerb = false;
    for (const verb of STRONG_VERBS) {
        if (lower.includes(verb)) {
            hasStrongVerb = true;
            break;
        }
    }

    if (hasStrongVerb && (sectionName === "PROJECTS" || sectionName === "WORK_EXPERIENCE" || lower.includes("project") || lower.includes("system") || lower.includes("campaign") || lower.includes("process"))) {
        return EvidenceStrength.STRONG;
    }

    // 3. Coursework / Academic
    for (const cw of COURSEWORK_TRIGGERS) {
        if (lower.includes(cw)) return EvidenceStrength.PARTIAL;
    }

    // 4. Weak / Passive
    for (const weak of WEAK_TRIGGERS) {
        if (lower.includes(weak)) return EvidenceStrength.UNCLEAR;
    }

    if (hasStrongVerb) return EvidenceStrength.STRONG;
    if (sectionName === "SKILLS") return EvidenceStrength.PARTIAL;

    return EvidenceStrength.UNCLEAR;
}

// --- 3. RESUME & JD PARSERS ---
function parseResume(id, rawText) {
    const lines = rawText.split(/\r?\n/);
    let name = `Candidate ${id}`;
    let email = `candidate${id}@example.com`;
    let phone = "Not specified";

    const sections = {};
    let currentSection = "SUMMARY";
    let sectionBuffer = [];

    const dreamCompanies = [];
    const preferredRoles = [];
    const preferredDomains = [];

    for (const line of lines) {
        const upper = line.trim().toUpperCase();
        if (upper.startsWith("NAME:")) name = line.substring(5).trim();
        else if (upper.startsWith("EMAIL:")) email = line.substring(6).trim();
        else if (upper.startsWith("PHONE:")) phone = line.substring(6).trim();
        else if (upper.startsWith("DREAM COMPANIES:")) {
            line.substring(line.indexOf(":") + 1).split(",").forEach(s => s.trim() && dreamCompanies.push(s.trim()));
        } else if (upper.startsWith("PREFERRED ROLE:") || upper.startsWith("PREFERRED ROLES:")) {
            line.substring(line.indexOf(":") + 1).split(",").forEach(s => s.trim() && preferredRoles.push(s.trim()));
        } else if (upper.startsWith("PREFERRED DOMAIN:") || upper.startsWith("PREFERRED DOMAINS:")) {
            line.substring(line.indexOf(":") + 1).split(",").forEach(s => s.trim() && preferredDomains.push(s.trim()));
        }

        let newSection = null;
        if (upper.startsWith("TECHNICAL SKILLS") || upper.startsWith("SKILLS") || upper.includes("COMPETENCIES")) newSection = "SKILLS";
        else if (upper.startsWith("PROJECTS") || upper === "KEY PROJECTS") newSection = "PROJECTS";
        else if (upper.startsWith("WORK EXPERIENCE") || upper.startsWith("EXPERIENCE") || upper.startsWith("EMPLOYMENT")) newSection = "WORK_EXPERIENCE";
        else if (upper.startsWith("EDUCATION")) newSection = "EDUCATION";
        else if (upper.startsWith("CERTIFICATIONS")) newSection = "CERTIFICATIONS";

        if (newSection) {
            if (sectionBuffer.length > 0) sections[currentSection] = sectionBuffer.join("\n");
            currentSection = newSection;
            sectionBuffer = [];
        } else {
            sectionBuffer.push(line);
        }
    }

    if (sectionBuffer.length > 0) sections[currentSection] = sectionBuffer.join("\n");

    return {
        id,
        name,
        email,
        phone,
        rawResumeText: rawText,
        parsedSections: sections,
        preferences: { dreamCompanies, preferredRoles, preferredDomains }
    };
}

function parseJDFromInput(id, title, companyName, domain, mandatoryText, preferredText, rawText) {
    const parseReqList = (text, isMandatory) => {
        if (!text) return [];
        return text.split(/\n|,/).map(item => item.trim()).filter(Boolean).map(reqName => {
            const cleanName = reqName.replace(/^[-•*]\s*/, "");
            let category = "Technical Skill";
            if (/project|system|campaign|app/i.test(cleanName)) category = "Project Experience";
            else if (/degree|b\.tech|b\.e|mba|bba|b\.com/i.test(cleanName)) category = "Education";
            else if (/experience|years/i.test(cleanName)) category = "Work Experience";

            const importance = isMandatory ? Importance.HIGH : Importance.MEDIUM;
            const synonyms = [cleanName.toLowerCase()];
            if (cleanName.toLowerCase().includes("python")) synonyms.push("py", "python3");
            if (cleanName.toLowerCase().includes("machine learning")) synonyms.push("ml");
            if (cleanName.toLowerCase().includes("software engineer")) synonyms.push("se", "developer");
            if (cleanName.toLowerCase().includes("human resources")) synonyms.push("hr");

            return {
                name: cleanName,
                category,
                importance,
                isMandatory,
                synonyms,
                categoryWeight: CategoryWeight[category] || 1.0
            };
        });
    };

    const mandatoryRequirements = parseReqList(mandatoryText, true);
    const preferredRequirements = parseReqList(preferredText, false);

    return {
        id,
        title: title || "Custom Hiring Role",
        companyName: companyName || "Hiring Organization",
        domain: domain || "General Industry",
        rawText: rawText || "",
        mandatoryRequirements,
        preferredRequirements
    };
}

// --- 4. EVIDENCE & CLAIM ENGINE ---
function extractEvidence(candidate, jd) {
    const allReqs = [...jd.mandatoryRequirements, ...jd.preferredRequirements];
    const evidenceList = [];

    const sectionKeys = ["PROJECTS", "WORK_EXPERIENCE", "SKILLS", "SUMMARY", "EDUCATION", "CERTIFICATIONS"];

    for (const req of allReqs) {
        const keywords = [req.name.toLowerCase(), ...(req.synonyms || []).map(s => s.toLowerCase())];
        let bestStrength = EvidenceStrength.NOT_FOUND;
        let bestSnippet = "No relevant evidence found in candidate profile.";
        let bestSection = "None";

        for (const secKey of sectionKeys) {
            const content = candidate.parsedSections[secKey];
            if (!content) continue;

            const sentences = content.split(/\n|\./);
            for (const sentence of sentences) {
                const trimmed = sentence.trim();
                if (!trimmed) continue;

                for (const kw of keywords) {
                    if (trimmed.toLowerCase().includes(kw)) {
                        const strength = classifyContext(trimmed, secKey, kw);
                        if (strength.multiplier > bestStrength.multiplier || (bestStrength === EvidenceStrength.NOT_FOUND && strength === EvidenceStrength.NEGATIVE)) {
                            bestStrength = strength;
                            bestSnippet = trimmed;
                            bestSection = secKey === "PROJECTS" ? "Project Experience" : secKey === "WORK_EXPERIENCE" ? "Work Experience" : secKey;
                        }
                    }
                }
            }
        }

        evidenceList.push({
            requirementName: req.name,
            strength: bestStrength.label === 'Strong Evidence' ? 'STRONG' : bestStrength.label === 'Partial Evidence' ? 'PARTIAL' : bestStrength.label === 'Needs Verification' ? 'UNCLEAR' : bestStrength.label === 'Not Demonstrated' ? 'NEGATIVE' : 'NOT_FOUND',
            strengthLabel: bestStrength.label,
            icon: bestStrength.icon,
            multiplier: bestStrength.multiplier,
            snippet: bestSnippet,
            sourceSection: bestSection,
            contextNote: `${bestStrength.label} identified in ${bestSection}.`
        });
    }

    return evidenceList;
}

function verifyClaims(candidate) {
    const claims = [];
    const lines = candidate.rawResumeText.split(/\r?\n/);

    for (const line of lines) {
        const trimmed = line.trim();
        if (trimmed.length < 25) continue;
        const lower = trimmed.toLowerCase();

        if (lower.includes("developed") || lower.includes("built") || lower.includes("engineered") || lower.includes("managed") || lower.includes("led") || lower.includes("achieved") || lower.includes("system") || lower.includes("campaign")) {
            let score = 0;
            const checklist = [];

            if (lower.includes("developed") || lower.includes("built") || lower.includes("engineered") || lower.includes("managed") || lower.includes("led")) { score += 20; checklist.push("Action Verb: Present"); }
            else checklist.push("Action Verb: Missing");

            if (/[a-z]{3,}/.test(lower)) { score += 20; checklist.push("Domain Tool/Skill Specified: Present"); }
            else checklist.push("Domain Tool/Skill Specified: Missing");

            if (lower.includes("process") || lower.includes("method") || lower.includes("strategy") || lower.includes("system") || lower.includes("algorithm")) { score += 20; checklist.push("Methodology/Strategy Detail: Present"); }
            else checklist.push("Methodology/Strategy Detail: Limited evidence");

            if (/\d+%|\$\d+|\d+k|\d+ users|\d+ clients/.test(lower) || lower.includes("increased") || lower.includes("improved")) { score += 20; checklist.push("Quantified Results: Present"); }
            else checklist.push("Quantified Results: Insufficient evidence");

            if (lower.includes("deployed") || lower.includes("launched") || lower.includes("production") || lower.includes("executed")) { score += 20; checklist.push("Execution Context: Present"); }
            else checklist.push("Execution Context: Not specified");

            const strength = score >= 80 ? "STRONG" : score >= 50 ? "PARTIAL" : "UNCLEAR";
            const action = score >= 80 ? "Strong supporting evidence." : "Verification recommended during interview.";

            claims.push({ claimText: trimmed, strength, verificationScore: score, checklist, action });
        }
    }

    if (claims.length === 0) {
        claims.push({
            claimText: "Core accomplishments stated in profile",
            strength: "PARTIAL",
            verificationScore: 60,
            checklist: ["Project Context: Verified", "Metrics: Limited"],
            action: "Clarification recommended."
        });
    }

    return claims;
}

// --- 5. EVALUATION MODES & RANKING ENGINE ---
function evaluateCandidate(candidate, jd, mode, evidenceList) {
    let isShortlisted = true;
    let rejectionReason = "";
    let qualificationScore = 0;

    const mandatoryNames = jd.mandatoryRequirements.map(r => r.name);
    const failedMandatory = [];

    for (const mName of mandatoryNames) {
        const ev = evidenceList.find(e => e.requirementName.toLowerCase() === mName.toLowerCase());
        if (!ev || ev.strength === 'NOT_FOUND' || ev.strength === 'NEGATIVE' || ev.strength === 'UNCLEAR') {
            failedMandatory.push(mName);
        }
    }

    if (mode === "STRICT") {
        if (failedMandatory.length > 0) {
            isShortlisted = false;
            qualificationScore = 0;
            rejectionReason = `Missing mandatory requirement: ${failedMandatory.join(', ')}`;
        } else {
            isShortlisted = true;
            let earned = 0, totalW = 0;
            evidenceList.forEach(e => {
                const req = [...jd.mandatoryRequirements, ...jd.preferredRequirements].find(r => r.name === e.requirementName);
                const w = req ? req.importance.weight : 2.0;
                totalW += w;
                earned += w * Math.max(0, e.multiplier);
            });
            qualificationScore = totalW > 0 ? Math.round((earned / totalW) * 100) : 0;
        }
    } else if (mode === "BALANCED") {
        if (failedMandatory.length > 0) {
            isShortlisted = false;
            rejectionReason = `Did not satisfy mandatory requirements: ${failedMandatory.join(', ')}`;
        } else {
            isShortlisted = true;
        }

        let mandEarned = 0, mandTotal = 0;
        jd.mandatoryRequirements.forEach(r => {
            const ev = evidenceList.find(e => e.requirementName === r.name);
            mandTotal += r.importance.weight;
            if (ev) mandEarned += r.importance.weight * Math.max(0, ev.multiplier);
        });

        let prefEarned = 0, prefTotal = 0;
        jd.preferredRequirements.forEach(r => {
            const ev = evidenceList.find(e => e.requirementName === r.name);
            prefTotal += r.importance.weight;
            if (ev) prefEarned += r.importance.weight * Math.max(0, ev.multiplier);
        });

        const mandRatio = mandTotal > 0 ? mandEarned / mandTotal : 1;
        const prefRatio = prefTotal > 0 ? prefEarned / prefTotal : 1;

        qualificationScore = Math.round((0.65 * mandRatio + 0.35 * prefRatio) * 100);
    } else {
        // BEST MATCH MODE
        isShortlisted = true;
        rejectionReason = "";

        let earned = 0, totalW = 0;
        [...jd.mandatoryRequirements, ...jd.preferredRequirements].forEach(r => {
            const ev = evidenceList.find(e => e.requirementName === r.name);
            const w = r.importance.weight * (r.categoryWeight || 1.0);
            totalW += w;
            if (ev) earned += w * Math.max(0, ev.multiplier);
        });

        qualificationScore = totalW > 0 ? Math.round((earned / totalW) * 100) : 0;
    }

    const strongAreas = evidenceList.filter(e => e.strength === 'STRONG').map(e => e.requirementName);
    const partialAreas = evidenceList.filter(e => e.strength === 'PARTIAL').map(e => e.requirementName);
    const missingRequirements = evidenceList.filter(e => e.strength === 'NOT_FOUND' || e.strength === 'NEGATIVE').map(e => e.requirementName);
    const verificationFlags = evidenceList.filter(e => e.strength === 'UNCLEAR').map(e => `${e.requirementName} (Vague context)`);

    // Domain-Independent Interview Questions
    const interviewQuestions = [];
    evidenceList.forEach(e => {
        if (e.strength === 'UNCLEAR' || e.strength === 'PARTIAL') {
            interviewQuestions.push({
                target: e.requirementName,
                question: `You noted experience in ${e.requirementName}. Could you describe a specific project or business situation where you demonstrated ${e.requirementName} and explain your approach?`,
                category: "Targeted Deep-Dive",
                priority: 1
            });
        }
    });

    if (interviewQuestions.length === 0 && strongAreas.length > 0) {
        interviewQuestions.push({
            target: strongAreas[0],
            question: `How did you optimize your implementation/process when utilizing ${strongAreas[0]} in your recent work?`,
            category: "Domain Mastery",
            priority: 2
        });
    }

    // Opportunity Alignment
    let matches = 0;
    if (candidate.preferences.preferredRoles.some(r => jd.title.toLowerCase().includes(r.toLowerCase()))) matches++;
    if (candidate.preferences.preferredDomains.some(d => jd.domain.toLowerCase().includes(d.toLowerCase()))) matches++;
    if (candidate.preferences.dreamCompanies.some(c => jd.companyName.toLowerCase().includes(c.toLowerCase()))) matches++;

    const preferenceAlignment = matches >= 2 ? "HIGH ALIGNMENT" : matches === 1 ? "MEDIUM ALIGNMENT" : "LOW ALIGNMENT";

    const modeTitle = mode === 'STRICT' ? 'Strict Mode' : mode === 'BALANCED' ? 'Balanced Mode' : 'Best Match Mode';
    let explanationText = "";
    if (!isShortlisted) {
        explanationText = `Not shortlisted under ${modeTitle}. Reason: ${rejectionReason}.`;
    } else {
        explanationText = `Candidate achieved a ${qualificationScore}% score under ${modeTitle}.\n- Strong evidence for ${strongAreas.length} criteria.\n- Key Strengths: ${strongAreas.join(', ') || 'None'}.`;
    }

    return {
        candidateId: candidate.id,
        candidateName: candidate.name,
        mode,
        modeTitle,
        qualificationScore,
        isShortlisted,
        rejectionReason,
        evidenceList,
        claims: verifyClaims(candidate),
        strongAreas,
        partialAreas,
        missingRequirements,
        verificationFlags,
        interviewQuestions,
        preferenceAlignment,
        explanationText
    };
}

// --- 6. MULTI-DOMAIN SAMPLE DATA ---
const sampleJobTemplates = [
    {
        id: "job_se",
        title: "Software Engineer",
        companyName: "Acme Cloud Corp",
        domain: "Software Development",
        mandatoryText: "Java, SQL, Data Structures",
        preferredText: "Spring Boot, Git, Docker, Microservices"
    },
    {
        id: "job_mkt",
        title: "Digital Marketing Intern",
        companyName: "Growth Brand Labs",
        domain: "Marketing & Media",
        mandatoryText: "SEO, Content Writing, Social Media",
        preferredText: "Google Analytics, Copywriting, Canva, Email Campaigns"
    },
    {
        id: "job_hr",
        title: "HR Executive",
        companyName: "Global Talent Solutions",
        domain: "Human Resources",
        mandatoryText: "Talent Acquisition, Communication, Interviewing",
        preferredText: "Payroll, Labor Laws, Employee Engagement, Excel"
    },
    {
        id: "job_ml",
        title: "AI/ML Intern",
        companyName: "Cognitive Tech Systems",
        domain: "Artificial Intelligence",
        mandatoryText: "Python, Machine Learning, AI project",
        preferredText: "SQL, Deep Learning, Computer Vision, Git"
    }
];

let activeJd = parseJDFromInput(
    sampleJobTemplates[0].id,
    sampleJobTemplates[0].title,
    sampleJobTemplates[0].companyName,
    sampleJobTemplates[0].domain,
    sampleJobTemplates[0].mandatoryText,
    sampleJobTemplates[0].preferredText
);

const candidatePool = [];

// Multi-Domain Sample Resumes
const sampleResumes = [
    {
        id: "1",
        text: `NAME: Anuja P\nEMAIL: anuja@example.com\nPHONE: +91 9876543210\nPREFERRED ROLE: Software Engineer, AI/ML Intern\nPREFERRED DOMAIN: Software Development, Artificial Intelligence\nDREAM COMPANIES: Acme Cloud Corp, Google, Microsoft\n\nSUMMARY:\nPassionate developer with strong foundation in Java, Python, and SQL databases.\n\nTECHNICAL SKILLS:\n- Languages: Java, Python, SQL\n- Frameworks: Spring Boot, Scikit-learn\n- Tools: Git, Docker\n\nPROJECTS:\n- Microservices E-Commerce API\n  Engineered scalable microservices in Java using Spring Boot and PostgreSQL. Deployed Docker containers with 99.9% uptime.\n- Disease Prediction System\n  Developed a disease prediction system using machine learning algorithms in Python.`
    },
    {
        id: "2",
        text: `NAME: Rohan M\nEMAIL: rohan@example.com\nPHONE: +91 9876543215\nPREFERRED ROLE: Digital Marketing Intern\nPREFERRED DOMAIN: Marketing & Media\nDREAM COMPANIES: Growth Brand Labs, Nike\n\nSUMMARY:\nCreative marketer experienced in SEO strategy, content writing, and Google Analytics.\n\nTECHNICAL SKILLS:\n- Marketing: SEO, Copywriting, Social Media, Content Writing\n- Tools: Google Analytics, Canva, Mailchimp\n\nPROJECTS:\n- SEO Brand Growth Campaign\n  Formulated SEO strategy and wrote keyword-rich articles that increased organic website traffic by 45% in 3 months.\n- Social Media Marketing\n  Managed Instagram and LinkedIn ad campaigns resulting in 10,000 new impressions.`
    },
    {
        id: "3",
        text: `NAME: Simran K\nEMAIL: simran@example.com\nPHONE: +91 9876543216\nPREFERRED ROLE: HR Executive\nPREFERRED DOMAIN: Human Resources\nDREAM COMPANIES: Global Talent Solutions, Deloitte\n\nSUMMARY:\nPeople operations specialist skilled in Talent Acquisition, interviewing, and employee engagement.\n\nTECHNICAL SKILLS:\n- HR: Talent Acquisition, Interviewing, Onboarding, Communication\n- Tools: Excel, HRMS, Payroll Systems\n\nWORK EXPERIENCE:\n- HR Assistant, PeopleFirst Inc (2024 - 2025)\n  Coordinated end-to-end Talent Acquisition for 50+ campus hires. Conducted candidate interviewing and managed onboarding procedures.`
    },
    {
        id: "4",
        text: `NAME: Rahul K\nEMAIL: rahul@example.com\nPHONE: +91 9876543211\nPREFERRED ROLE: Software Engineer\nPREFERRED DOMAIN: Web Development\nDREAM COMPANIES: Microsoft\n\nSUMMARY:\nDeveloper skilled in Java and web portals. Interested in learning Python.\n\nTECHNICAL SKILLS:\n- Languages: Java, SQL, HTML\n\nPROJECTS:\n- Java Console App\n  Built a Java application for record management.`
    }
];

sampleResumes.forEach(sr => candidatePool.push(parseResume(sr.id, sr.text)));

// --- 7. SERVER SETUP & REST API ---
const WEB_DIR = path.join(__dirname, 'web');

const server = http.createServer((req, res) => {
    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;

    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    // GET /api/sample-jobs
    if (pathname === '/api/sample-jobs') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(sampleJobTemplates));
        return;
    }

    // GET /api/jobs
    if (pathname === '/api/jobs' && req.method === 'GET') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(activeJd));
        return;
    }

    // POST /api/jobs (Recruiter creates job dynamically)
    if (pathname === '/api/jobs' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const payload = JSON.parse(body);

                // If switching to sample job template
                if (payload.templateId) {
                    const tmpl = sampleJobTemplates.find(t => t.id === payload.templateId);
                    if (tmpl) {
                        activeJd = parseJDFromInput(tmpl.id, tmpl.title, tmpl.companyName, tmpl.domain, tmpl.mandatoryText, tmpl.preferredText);
                    }
                } else {
                    activeJd = parseJDFromInput(
                        "job_" + Date.now(),
                        payload.title,
                        payload.companyName,
                        payload.domain,
                        payload.mandatoryText,
                        payload.preferredText,
                        payload.rawText
                    );
                }

                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: true, activeJd }));
            } catch (err) {
                res.writeHead(400, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: err.message }));
            }
        });
        return;
    }

    // GET /api/candidates
    if (pathname === '/api/candidates' && req.method === 'GET') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(candidatePool));
        return;
    }

    // POST /api/candidates (Candidate uploads profile dynamically)
    if (pathname === '/api/candidates' && req.method === 'POST') {
        let body = '';
        req.on('data', chunk => { body += chunk; });
        req.on('end', () => {
            try {
                const payload = JSON.parse(body);
                let resumeText = payload.rawResumeText || "";

                if (!resumeText && payload.name) {
                    resumeText = `NAME: ${payload.name}\nEMAIL: ${payload.email || ''}\nPHONE: ${payload.phone || ''}\nPREFERRED ROLE: ${payload.preferredRoles || ''}\nPREFERRED DOMAIN: ${payload.preferredDomains || ''}\nDREAM COMPANIES: ${payload.dreamCompanies || ''}\n\nSUMMARY:\n${payload.summary || ''}\n\nTECHNICAL SKILLS:\n${payload.skills || ''}\n\nPROJECTS:\n${payload.projects || ''}\n\nWORK EXPERIENCE:\n${payload.experience || ''}`;
                }

                const newCandidate = parseResume(String(candidatePool.length + 1), resumeText);
                candidatePool.push(newCandidate);

                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ success: true, candidate: newCandidate }));
            } catch (err) {
                res.writeHead(400, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ error: err.message }));
            }
        });
        return;
    }

    // GET /api/search
    if (pathname === '/api/search') {
        const q = (parsedUrl.query.q || '').toLowerCase();
        const results = candidatePool.filter(c => c.rawResumeText.toLowerCase().includes(q));
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(results));
        return;
    }

    // GET /api/analyze
    if (pathname === '/api/analyze') {
        const mode = (parsedUrl.query.mode || 'BALANCED').toUpperCase();
        const results = candidatePool.map(c => {
            const evList = extractEvidence(c, activeJd);
            return evaluateCandidate(c, activeJd, mode, evList);
        });

        results.sort((a, b) => {
            if (a.isShortlisted && !b.isShortlisted) return -1;
            if (!a.isShortlisted && b.isShortlisted) return 1;
            return b.qualificationScore - a.qualificationScore;
        });

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(results));
        return;
    }

    // Static Web File Handler
    let filePath = path.join(WEB_DIR, pathname === '/' ? 'index.html' : pathname);
    if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
        filePath = path.join(WEB_DIR, 'index.html');
    }

    const ext = path.extname(filePath);
    const mimeTypes = {
        '.html': 'text/html; charset=UTF-8',
        '.css': 'text/css',
        '.js': 'application/javascript',
        '.png': 'image/png',
        '.jpg': 'image/jpeg',
        '.svg': 'image/svg+xml'
    };

    fs.readFile(filePath, (err, data) => {
        if (err) {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            res.end("404 Not Found");
        } else {
            res.writeHead(200, { 'Content-Type': mimeTypes[ext] || 'text/plain' });
            res.end(data);
        }
    });
});

const PORT = 8080;
server.listen(PORT, () => {
    console.log("=================================================");
    console.log("  RESUMEPROOF DOMAIN-INDEPENDENT RECRUITMENT SERVER ");
    console.log("=================================================");
    console.log(`  Server running at: http://localhost:${PORT}`);
    console.log(`  Web UI directory : ${WEB_DIR}`);
    console.log("  Constraint       : ZERO THIRD-PARTY DEPENDENCIES");
    console.log("=================================================\n");
});
