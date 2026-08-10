// ResumeProof Client Application logic — Connected to Java SE 17 Core Engine
document.addEventListener('DOMContentLoaded', () => {
    let currentMode = 'BALANCED';
    let currentResults = [];
    let activeJd = null;
    let candidatePool = [];
    let sampleJobs = [];

    // Initialize UI
    initNavigation();
    initModeSelector();
    initForms();
    initSearch();
    initCompletenessTracker();
    loadSampleJobs();
    loadJobDescription();
    runAnalysis(currentMode);
    loadCandidates();

    // Navigation Tabs
    function initNavigation() {
        const navBtns = document.querySelectorAll('.nav-btn');
        const tabContents = document.querySelectorAll('.tab-content');

        navBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const targetTab = btn.getAttribute('data-tab');
                navBtns.forEach(b => b.classList.remove('active'));
                tabContents.forEach(c => c.classList.remove('active'));

                btn.classList.add('active');
                document.getElementById(targetTab).classList.add('active');
            });
        });

        document.getElementById('btn-close-modal').addEventListener('click', closeModal);
    }

    // Mode Selector Pills
    function initModeSelector() {
        const modePills = document.querySelectorAll('.mode-pill');
        modePills.forEach(pill => {
            pill.addEventListener('click', () => {
                modePills.forEach(p => p.classList.remove('active'));
                pill.classList.add('active');

                currentMode = pill.getAttribute('data-mode');
                updateModeBanner(currentMode);
                runAnalysis(currentMode);
            });
        });
    }

    function updateModeBanner(mode) {
        const titleEl = document.getElementById('mode-title');
        const descEl = document.getElementById('mode-desc');

        if (mode === 'STRICT') {
            titleEl.textContent = '🔒 Strict Mode Active';
            descEl.textContent = 'Mandatory requirements are hard filters. Candidates missing ANY mandatory requirement are immediately rejected.';
        } else if (mode === 'BALANCED') {
            titleEl.textContent = '⚖️ Balanced Mode Active';
            descEl.textContent = 'Mandatory requirements act as primary filters. Preferred requirements and evidence strength drive final ranking.';
        } else {
            titleEl.textContent = '🌟 Best Match Mode Active';
            descEl.textContent = 'No hard elimination. Ranks all candidates holistically based on overall suitability and requirement coverage.';
        }
    }

    // Form Event Handlers
    function initForms() {
        // Recruiter Job Requirement Creator
        const jobForm = document.getElementById('form-create-job');
        jobForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const payload = {
                title: document.getElementById('job-input-title').value.trim(),
                companyName: document.getElementById('job-input-company').value.trim(),
                domain: document.getElementById('job-input-domain').value.trim(),
                mandatoryText: document.getElementById('job-input-mandatory').value.trim(),
                preferredText: document.getElementById('job-input-preferred').value.trim()
            };

            try {
                const res = await fetch('/api/jobs', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                const data = await res.json();
                if (data.success) {
                    loadJobDescription();
                    runAnalysis(currentMode);
                }
            } catch (err) {
                console.error("Error creating job:", err);
            }
        });

        // Candidate Profile Creator
        const candForm = document.getElementById('form-create-candidate');
        candForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const payload = {
                name: document.getElementById('cand-input-name').value.trim(),
                email: document.getElementById('cand-input-email').value.trim(),
                phone: document.getElementById('cand-input-phone').value.trim(),
                dreamCompanies: document.getElementById('cand-input-dream').value.trim(),
                preferredRoles: document.getElementById('cand-input-roles').value.trim(),
                preferredDomains: document.getElementById('cand-input-domains').value.trim(),
                rawResumeText: document.getElementById('cand-input-resume').value.trim()
            };

            try {
                const res = await fetch('/api/candidates', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                });
                const data = await res.json();
                if (data.success) {
                    candForm.reset();
                    updateCompletenessBar();
                    loadCandidates();
                    runAnalysis(currentMode);
                    alert("Profile submitted! Candidate has entered the candidate pool.");
                }
            } catch (err) {
                console.error("Error creating candidate:", err);
            }
        });
    }

    // Candidate Profile Completeness Tracker
    function initCompletenessTracker() {
        const fields = ['cand-input-name', 'cand-input-email', 'cand-input-phone', 'cand-input-dream', 'cand-input-roles', 'cand-input-domains', 'cand-input-resume'];
        fields.forEach(id => {
            const el = document.getElementById(id);
            if (el) {
                el.addEventListener('input', updateCompletenessBar);
            }
        });
    }

    function updateCompletenessBar() {
        let filled = 0;
        const total = 7;

        if (document.getElementById('cand-input-name').value.trim()) filled++;
        if (document.getElementById('cand-input-email').value.trim()) filled++;
        if (document.getElementById('cand-input-phone').value.trim()) filled++;
        if (document.getElementById('cand-input-dream').value.trim()) filled++;
        if (document.getElementById('cand-input-roles').value.trim()) filled++;
        if (document.getElementById('cand-input-domains').value.trim()) filled++;
        
        const resumeText = document.getElementById('cand-input-resume').value.trim();
        if (resumeText.length > 50) filled++;

        const pct = Math.round((filled / total) * 100);
        document.getElementById('completeness-percentage').textContent = `${pct}%`;
        document.getElementById('completeness-fill').style.width = `${pct}%`;
    }

    // Load Sample Jobs Dropdown
    async function loadSampleJobs() {
        try {
            const res = await fetch('/api/sample-jobs');
            sampleJobs = await res.json();
            const select = document.getElementById('sample-job-select');
            select.innerHTML = '<option value="">-- Select Template --</option>' +
                sampleJobs.map(j => `<option value="${j.id}">${j.title} (${j.domain})</option>`).join('');

            select.addEventListener('change', async () => {
                const val = select.value;
                if (!val) return;
                await fetch('/api/jobs', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ templateId: val })
                });
                loadJobDescription();
                runAnalysis(currentMode);
            });
        } catch (err) {
            console.error("Error loading sample jobs:", err);
        }
    }

    // Fetch Job Description
    async function loadJobDescription() {
        try {
            const res = await fetch('/api/jobs');
            activeJd = await res.json();

            document.getElementById('header-job-title').textContent = activeJd.title;
            document.getElementById('header-job-meta').textContent = `${activeJd.companyName} | ${activeJd.domain}`;

            // Populate Form fields for editing
            document.getElementById('job-input-title').value = activeJd.title;
            document.getElementById('job-input-company').value = activeJd.companyName;
            document.getElementById('job-input-domain').value = activeJd.domain;
            document.getElementById('job-input-mandatory').value = activeJd.mandatoryRequirements.map(r => r.name).join(', ');
            document.getElementById('job-input-preferred').value = activeJd.preferredRequirements.map(r => r.name).join(', ');

            const mandContainer = document.getElementById('mandatory-tags');
            mandContainer.innerHTML = activeJd.mandatoryRequirements
                .map(r => `<span class="tag mandatory">Required: ${r.name}</span>`)
                .join('');

            const prefContainer = document.getElementById('preferred-tags');
            prefContainer.innerHTML = activeJd.preferredRequirements
                .map(r => `<span class="tag preferred">Preferred: ${r.name}</span>`)
                .join('');
        } catch (err) {
            console.error("Error loading JD:", err);
        }
    }

    // Run Engine Analysis
    async function runAnalysis(mode) {
        try {
            const res = await fetch(`/api/analyze?mode=${mode}`);
            currentResults = await res.json();
            renderCandidatesTable(currentResults);
        } catch (err) {
            console.error("Error running analysis:", err);
        }
    }

    // Render Candidates Data Table
    function renderCandidatesTable(results) {
        const tbody = document.getElementById('candidates-tbody');
        tbody.innerHTML = '';

        results.forEach((r, idx) => {
            const tr = document.createElement('tr');
            const rankStr = idx + 1;
            const statusBadge = r.isShortlisted
                ? `<span class="badge badge-shortlisted">Shortlisted</span>`
                : `<span class="badge badge-rejected">Not Shortlisted</span>`;

            const scoreColor = r.isShortlisted ? 'var(--accent-green)' : 'var(--text-secondary)';
            const scoreDisplay = r.isShortlisted ? `${r.qualificationScore}%` : '0%';
            const strongCount = r.strongAreas ? r.strongAreas.length : 0;

            tr.innerHTML = `
                <td><strong>#${rankStr}</strong></td>
                <td><strong>${r.candidateName}</strong></td>
                <td>${statusBadge}</td>
                <td><span class="score-badge" style="color: ${scoreColor}">${scoreDisplay}</span></td>
                <td><span class="badge badge-partial">${r.preferenceAlignment}</span></td>
                <td>🟢 ${strongCount} criteria</td>
                <td><button class="btn-primary" style="padding: 6px 12px; font-size: 12px;">Examine Evidence</button></td>
            `;

            tr.addEventListener('click', () => openEvidenceModal(r));
            tbody.appendChild(tr);
        });
    }

    // Open Candidate Evidence Deep-Dive Modal
    function openEvidenceModal(candidateResult) {
        const modal = document.getElementById('evidence-modal');
        modal.classList.add('active');

        document.getElementById('modal-candidate-name').textContent = candidateResult.candidateName;
        document.getElementById('modal-candidate-meta').textContent = `Candidate ID: ${candidateResult.candidateId} | Mode: ${candidateResult.modeTitle}`;

        document.getElementById('modal-score').textContent = candidateResult.isShortlisted ? `${candidateResult.qualificationScore}%` : '0%';
        document.getElementById('modal-status').textContent = candidateResult.isShortlisted ? 'Shortlisted' : 'Not Shortlisted';
        document.getElementById('modal-status').style.color = candidateResult.isShortlisted ? 'var(--accent-green)' : 'var(--accent-red)';

        document.getElementById('modal-alignment').textContent = candidateResult.preferenceAlignment;
        document.getElementById('modal-explanation-text').innerText = candidateResult.explanationText;

        // Evidence Matrix
        const evTbody = document.getElementById('modal-evidence-tbody');
        evTbody.innerHTML = candidateResult.evidenceList.map(ev => {
            let badgeClass = 'badge-unclear';
            if (ev.strength === 'STRONG') badgeClass = 'badge-strong';
            else if (ev.strength === 'PARTIAL') badgeClass = 'badge-partial';
            else if (ev.strength === 'NOT_FOUND') badgeClass = 'badge-notfound';
            else if (ev.strength === 'NEGATIVE') badgeClass = 'badge-negative';

            return `
                <tr>
                    <td><strong>${ev.requirementName}</strong></td>
                    <td><span class="badge ${badgeClass}">${ev.icon} ${ev.strengthLabel}</span></td>
                    <td style="font-family: monospace; font-size: 12px;">"${ev.snippet}"</td>
                    <td><span class="tag">${ev.sourceSection}</span></td>
                </tr>
            `;
        }).join('');

        // Strong & Missing Areas
        document.getElementById('modal-strong-list').innerHTML = candidateResult.strongAreas.length > 0
            ? candidateResult.strongAreas.map(s => `<li>🟢 ${s}</li>`).join('')
            : '<li>None identified</li>';

        document.getElementById('modal-missing-list').innerHTML = candidateResult.missingRequirements.length > 0
            ? candidateResult.missingRequirements.map(m => `<li>🔴 ${m}</li>`).join('')
            : '<li>None missing</li>';

        // Claims
        document.getElementById('modal-claims-container').innerHTML = candidateResult.claims.map(cl => `
            <div class="claim-item">
                <strong>Accomplishment Claim:</strong> "${cl.claimText}"<br/>
                <span class="badge badge-partial" style="margin-top: 4px;">Strength: ${cl.strength} (${cl.verificationScore}%)</span>
                <p style="font-size: 12px; color: var(--text-secondary); margin-top: 6px;">Action: ${cl.action}</p>
                <div style="margin-top: 6px; font-size: 11px; color: var(--text-secondary);">
                    ${cl.checklist.map(c => `<span class="tag" style="margin-right: 4px;">${c}</span>`).join('')}
                </div>
            </div>
        `).join('');

        // Interview Questions
        document.getElementById('modal-questions-container').innerHTML = candidateResult.interviewQuestions.map(q => `
            <div class="question-item">
                <strong>[${q.category}] Target: ${q.target}</strong>
                <p>"${q.question}"</p>
            </div>
        `).join('');
    }

    function closeModal() {
        document.getElementById('evidence-modal').classList.remove('active');
    }

    // Candidate Discovery Search
    function initSearch() {
        const searchInput = document.getElementById('discovery-search-input');
        const searchBtn = document.getElementById('btn-search-discovery');

        const doSearch = async (queryOverride) => {
            const query = (queryOverride !== undefined ? queryOverride : searchInput.value).trim();
            try {
                const res = await fetch(`/api/search?q=${encodeURIComponent(query)}`);
                const pool = await res.json();
                renderDiscoveryGrid(pool);
            } catch (err) {
                console.error("Search error:", err);
            }
        };

        searchBtn.addEventListener('click', () => doSearch());
        searchInput.addEventListener('keyup', (e) => { if (e.key === 'Enter') doSearch(); });

        document.querySelectorAll('.btn-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                const q = chip.getAttribute('data-query');
                searchInput.value = q;
                doSearch(q);
            });
        });
    }

    function renderDiscoveryGrid(pool) {
        const container = document.getElementById('discovery-results');
        if (pool.length === 0) {
            container.innerHTML = '<p style="color: var(--text-secondary);">No candidate profiles matched your criteria.</p>';
            return;
        }

        container.innerHTML = pool.map(c => `
            <div class="discovery-card">
                <h4>${c.name}</h4>
                <p>${c.email} | ${c.phone}</p>
                <div style="margin-bottom: 10px;">
                    <strong style="font-size: 11px; color: var(--accent-blue);">DREAM COMPANIES:</strong>
                    <div style="font-size: 12px;">${c.dreamCompanies.join(', ') || 'Not specified'}</div>
                </div>
                <div style="margin-bottom: 10px;">
                    <strong style="font-size: 11px; color: var(--accent-purple);">PREFERRED ROLES:</strong>
                    <div style="font-size: 12px;">${c.preferredRoles.join(', ') || 'Not specified'}</div>
                </div>
                <div>
                    <strong style="font-size: 11px; color: var(--accent-green);">PREFERRED DOMAINS:</strong>
                    <div style="font-size: 12px;">${c.preferredDomains.join(', ') || 'Not specified'}</div>
                </div>
            </div>
        `).join('');
    }

    // Load Candidate Profiles for Tab 2
    async function loadCandidates() {
        try {
            const res = await fetch('/api/candidates');
            candidatePool = await res.json();

            const picker = document.getElementById('candidate-picker');
            picker.innerHTML = candidatePool.map(c => `<option value="${c.id}">${c.name}</option>`).join('');

            picker.addEventListener('change', () => renderProfileDetails(picker.value));
            if (candidatePool.length > 0) renderProfileDetails(candidatePool[0].id);

            renderDiscoveryGrid(candidatePool);
        } catch (err) {
            console.error("Error loading candidates:", err);
        }
    }

    function renderProfileDetails(candidateId) {
        const candidate = candidatePool.find(c => c.id === candidateId);
        if (!candidate) return;

        const view = document.getElementById('candidate-profile-view');
        view.innerHTML = `
            <div class="sub-card">
                <h4>Contact Details</h4>
                <p><strong>Name:</strong> ${candidate.name}</p>
                <p><strong>Email:</strong> ${candidate.email}</p>
                <p><strong>Phone:</strong> ${candidate.phone}</p>
            </div>
            <div class="sub-card">
                <h4>Career Preferences</h4>
                <p><strong>Dream Companies:</strong> ${candidate.dreamCompanies.join(', ') || 'None'}</p>
                <p><strong>Preferred Roles:</strong> ${candidate.preferredRoles.join(', ') || 'None'}</p>
                <p><strong>Preferred Domains:</strong> ${candidate.preferredDomains.join(', ') || 'None'}</p>
            </div>
        `;
    }
});
