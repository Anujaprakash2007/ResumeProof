# ResumeProof

### Evidence-Based Candidate Discovery, Verification & Interview Intelligence Platform

> *"Don't just match the resume. Examine the evidence."*

---

## 1. Executive Summary & Problem Statement

Traditional Recruitment Applicant Tracking Systems (ATS) rely heavily on naive keyword frequency matching and black-box AI algorithms. This results in keyword stuffing, inaccurate scoring, unexplainable rejections, and zero proof of true technical competency.

**ResumeProof** solves this problem by introducing **Evidence-Based Candidate Matching**. Instead of outputting an opaque percentage match, ResumeProof inspects candidate resume context to identify:
- What requirement was specified.
- Whether evidence exists.
- Exact text snippet proof and surrounding context.
- Evidence strength (🟢 **STRONG**, 🟡 **PARTIAL**, 🟠 **UNCLEAR**, 🔴 **NOT FOUND**, ⛔ **NEGATIVE**).
- Transparent, explainable candidate ranking across three distinct evaluation modes (**STRICT**, **BALANCED**, and **BEST MATCH**).
- Skill-gap analysis, claim verification, and targeted interview question generation.

---

## 2. Absolute Zero External NLP / AI Dependency Constraint

> [!IMPORTANT]
> **100% Native Implementation**  
> ResumeProof is built using **ONLY built-in standard Java SE 17+ libraries** (`java.util.*`, `java.util.regex.*`, `java.nio.file.*`, `java.net.*`).  
> - **NO external packages** (No spaCy, NLTK, Jackson, Gson, Lucene).  
> - **NO external APIs** (No OpenAI, Gemini, Claude, Hugging Face).  
> - **NO cloud services**.  
> The intelligence comes entirely from custom rule engines, deterministic regex tokenization, context boundary analysis, negation detection, and explainable scoring formulas.

---

## 3. Key Innovation & Features

1. **Requirements $\rightarrow$ Evidence $\rightarrow$ Explanation $\rightarrow$ Decision Support**:
   - Every requirement check produces explicit text snippet evidence, context classification, and confidence scoring.
2. **Three Recruiter Evaluation Modes**:
   - **STRICT MODE**: Mandatory requirements act as hard filters. Missing ANY mandatory requirement results in immediate rejection with explicit reasons.
   - **BALANCED MODE**: Mandatory requirements act as primary filters, while preferred requirements and evidence strength determine final candidate ranking.
   - **BEST MATCH MODE**: Holistic suitability scoring across all criteria without elimination.
3. **Negation Detection**:
   - Scans lookback windows for negation triggers (`"no experience"`, `"never used"`, `"not familiar with"`), accurately categorizing claims as `NEGATIVE / NOT DEMONSTRATED`.
4. **Claim Verification Engine**:
   - Evaluates accomplishment claims against a 5-point sub-evidence checklist (Action Verb, Tech Stack, Algorithm/Model, Quantified Metrics, Deployment Context).
5. **Rule-Based Interview Intelligence**:
   - Generates prioritized, template-based technical interview questions for unverified claims, weak evidence, and high-priority skill gaps.
6. **Multi-Criteria Candidate Discovery**:
   - Search candidate resumes using custom multi-keyword queries with instant expandable evidence drill-down.
7. **Opportunity Preference Alignment**:
   - Calculates candidate career preference alignment (`HIGH`, `MEDIUM`, `LOW`) separately from technical qualification scores.

---

## 4. Architectural System Design

```text
ResumeProof/
│
├── src/
│   └── com/
│       └── resumeproof/
│           ├── model/          # Immutable domain models & Enums
│           ├── parser/         # Resume & JD text parsers, normalizers
│           ├── engine/         # Context, Evidence, Verification, Mode & Interview Engines
│           ├── server/         # Embedded HttpServer Host & JSON API Handlers
│           ├── test/           # Automated Test Suite (Tests 1-10)
│           └── Main.java       # Application entrypoint
│
├── web/                        # Modern Dark-Theme Glassmorphic Web UI
│   ├── index.html
│   ├── css/styles.css
│   └── js/app.js
│
├── data/                       # Plain Text Resumes & Job Descriptions
│   ├── job_descriptions/
│   └── resumes/
│
├── build.bat                   # Compilation script
├── run.bat                     # Server execution script
└── README.md
```

---

## 5. Scoring Formulas & Mode Logic

### Evidence Weighting Matrix
- 🟢 **STRONG EVIDENCE**: $1.00$
- 🟡 **PARTIAL EVIDENCE**: $0.60$
- 🟠 **UNCLEAR / NEEDS VERIFICATION**: $0.30$
- 🔴 **NOT FOUND**: $0.00$
- ⛔ **NEGATIVE / NOT DEMONSTRATED**: $-0.50$ (Penalty)

### Strict Mode Formula

$$\text{Shortlist Status} = \begin{cases} 
\text{NOT SHORTLISTED} & \text{if ANY Mandatory Requirement has } M_{\text{evidence}} < 0.60 \text{ or Status } = \text{NEGATIVE} \\
\text{SHORTLISTED} & \text{if ALL Mandatory Requirements have } M_{\text{evidence}} \ge 0.60 
\end{cases}$$

$$\text{Score}_{\text{Strict}} = \begin{cases}
0.0 & \text{if Shortlist Status } = \text{NOT SHORTLISTED} \\
\frac{\sum \left( W_{\text{importance}} \times M_{\text{evidence}} \right)}{\sum W_{\text{importance}}} \times 100 & \text{if Shortlist Status } = \text{SHORTLISTED}
\end{cases}$$

---

## 6. How to Build & Run

### Prerequisites
- Java Development Kit (JDK 17 or higher) installed and set in system PATH.

### Execution Commands (Windows)

1. **Compile Project**:
   ```cmd
   build.bat
   ```

2. **Run Server**:
   ```cmd
   run.bat
   ```

3. **Access Web Application**:
   Open your browser and navigate to:  
   `http://localhost:8080`

---

## 7. Automated Test Suite (Tests 1–10)

The application automatically executes 10 unit/edge-case tests upon launch:

- **Test 1**: "Experienced in Python." $\rightarrow$ Strong/Partial Evidence
- **Test 2**: "Interested in learning Python." $\rightarrow$ Unclear/Partial Evidence
- **Test 3**: "No experience with Python." $\rightarrow$ Negative / Not Demonstrated
- **Test 4**: "Developed ML project in Python" $\rightarrow$ Strong Evidence
- **Test 5**: "Familiar with machine learning." $\rightarrow$ Unclear / Needs Verification
- **Test 6**: Missing mandatory AI/ML project in STRICT mode $\rightarrow$ Candidate Rejected
- **Test 7**: Missing preferred requirements in BALANCED mode $\rightarrow$ Shortlisted with score
- **Test 8**: BEST MATCH mode preserves candidate in ranked list
- **Test 9**: Stronger evidence candidate ranks higher than weak evidence candidate
- **Test 10**: Rule-based domain synonym matching for project requirement

---

## 8. Verification & Ethical Hiring

ResumeProof functions strictly as a decision-support system. It does not replace human hiring judgment and contains explicit guardrails preventing candidate ranking based on sensitive personal attributes (gender, age, race, religion, health).
