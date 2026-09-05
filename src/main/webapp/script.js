const RECORDINGS_URL = "/insightline/api/v1/loadAllRecording";
const TRANSCRIBE_ALL_URL = "/insightline/api/v1/transcribeAll";
const LOAD_TRANSCRIPTS_URL = "/insightline/api/v1/loadAllTranscripts";
const FILTER_RECORDINGS_URL = "/insightline/api/v1/filterRecordings";
const FILTER_TRANSCRIPTS_URL = "/insightline/api/v1/filterTranscripts";

const REC_PAGE_SIZE = 6;
const TRANSCRIPT_PAGE_SIZE = 3;
const THEME_STORAGE_KEY = "insightline-theme";

let allRecordings = [];
let recPage = 1;
let recordingsRequestId = 0;
let recordingsLoading = false;

let allTranscripts = [];
let transcriptPage = 1;
let transcriptsRequestId = 0;
let transcriptsLoading = false;
let transcriptionRunning = false;

const ICONS = {
    check: '<svg viewBox="0 0 24 24" fill="none"><path d="M20 6 9 17l-5-5" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    clock: '<svg viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.8"/><path d="M12 7v5l3.5 2" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    alert: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 9v4m0 4h.01M10.3 3.9 2.7 17a2 2 0 0 0 1.7 3h15.2a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    chevronLeft: '<svg viewBox="0 0 24 24" fill="none"><path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    chevronRight: '<svg viewBox="0 0 24 24" fill="none"><path d="M9 18l6-6-6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    chevronDown: '<svg viewBox="0 0 24 24" fill="none"><path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    mic: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 15a3 3 0 0 0 3-3V6a3 3 0 0 0-6 0v6a3 3 0 0 0 3 3Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M19 11a7 7 0 0 1-14 0M12 18v3" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    calendar: '<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="5" width="18" height="16" rx="2" stroke="currentColor" stroke-width="1.6"/><path d="M8 3v4M16 3v4M3 10h18" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/></svg>',
    refresh: '<svg viewBox="0 0 24 24" fill="none"><path d="M4 4v5h5M20 20v-5h-5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/><path d="M5.5 15a8 8 0 0 0 13.9 2.5M18.5 9A8 8 0 0 0 4.6 6.5" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    doc: '<svg viewBox="0 0 24 24" fill="none"><path d="M7 3h7l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><path d="M14 3v5h5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>',
    sparkle: '<svg viewBox="0 0 24 24" fill="none"><path d="M12 3l1.6 4.9L18.5 9.5 13.6 11.1 12 16l-1.6-4.9L5.5 9.5l4.9-1.6L12 3Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/></svg>',
    list: '<svg viewBox="0 0 24 24" fill="none"><path d="M9 6h11M9 12h11M9 18h11M4 6h.01M4 12h.01M4 18h.01" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>',
    grid: '<svg viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/><rect x="14" y="3" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/><rect x="3" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/><rect x="14" y="14" width="7" height="7" rx="1.5" stroke="currentColor" stroke-width="1.6"/></svg>'
};

function initTheme() {
    const saved = localStorage.getItem(THEME_STORAGE_KEY);
    const prefersDark = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
    document.documentElement.setAttribute("data-theme", saved || (prefersDark ? "dark" : "light"));
}

function toggleTheme() {
    const current = document.documentElement.getAttribute("data-theme") === "dark" ? "dark" : "light";
    const next = current === "dark" ? "light" : "dark";
    document.documentElement.setAttribute("data-theme", next);
    localStorage.setItem(THEME_STORAGE_KEY, next);
}

function initTabs() {
    const tabRecordingsBtn = document.getElementById("tabRecordingsBtn");
    const tabTranscriptsBtn = document.getElementById("tabTranscriptsBtn");
    const panelRecordings = document.getElementById("panelRecordings");
    const panelTranscripts = document.getElementById("panelTranscripts");
    const tabs = [tabRecordingsBtn, tabTranscriptsBtn];

    function activate(tab) {
        const isRecordings = tab === "recordings";
        tabRecordingsBtn.classList.toggle("is-active", isRecordings);
        tabTranscriptsBtn.classList.toggle("is-active", !isRecordings);
        tabRecordingsBtn.setAttribute("aria-selected", String(isRecordings));
        tabTranscriptsBtn.setAttribute("aria-selected", String(!isRecordings));
        tabRecordingsBtn.tabIndex = isRecordings ? 0 : -1;
        tabTranscriptsBtn.tabIndex = isRecordings ? -1 : 0;
        panelRecordings.hidden = !isRecordings;
        panelTranscripts.hidden = isRecordings;
    }

    tabs.forEach((tab, index) => {
        tab.addEventListener("click", () => activate(index === 0 ? "recordings" : "transcripts"));
        tab.addEventListener("keydown", event => {
            let nextIndex;
            if (event.key === "ArrowRight") nextIndex = (index + 1) % tabs.length;
            else if (event.key === "ArrowLeft") nextIndex = (index - 1 + tabs.length) % tabs.length;
            else if (event.key === "Home") nextIndex = 0;
            else if (event.key === "End") nextIndex = tabs.length - 1;
            else return;

            event.preventDefault();
            tabs[nextIndex].focus();
            activate(nextIndex === 0 ? "recordings" : "transcripts");
        });
    });

    activate("recordings");
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text == null ? "" : String(text);
    return div.innerHTML;
}

function formatDate(unixSeconds) {
    if (!unixSeconds && unixSeconds !== 0) return "—";
    const date = new Date(Number(unixSeconds) * 1000);
    if (Number.isNaN(date.getTime())) return String(unixSeconds);
    return date.toLocaleString(undefined, {
        month: "short",
        day: "numeric",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    });
}

function dateInputToEpoch(value, endOfDay) {
    if (!value) return null;
    const date = new Date(value + (endOfDay ? "T23:59:59" : "T00:00:00"));
    if (Number.isNaN(date.getTime())) return null;
    return Math.floor(date.getTime() / 1000);
}

async function requestJson(url, options) {
    let response;
    try {
        response = await fetch(url, options);
    } catch {
        throw new Error("Network request failed. Please check your connection.");
    }

    let bodyText;
    try {
        bodyText = await response.text();
    } catch {
        throw new Error("Unable to read the server response.");
    }

    let body = null;
    if (bodyText) {
        try {
            body = JSON.parse(bodyText);
        } catch {
            if (response.ok) throw new Error("The server returned an invalid response.");
        }
    }

    if (!response.ok) {
        const message = body && typeof body.error === "string" && body.error.trim()
            ? body.error.trim()
            : `Request failed (${response.status}).`;
        throw new Error(message);
    }

    if (body === null) throw new Error("The server returned an empty response.");
    return {body, response};
}

function setStatus(statusEl, message, isError = false) {
    statusEl.classList.toggle("is-error", isError);
    statusEl.innerHTML = `${isError ? ICONS.alert : ICONS.check}<span>${escapeHtml(message)}</span>`;
}

function setLoadingStatus(statusEl, message) {
    statusEl.classList.remove("is-error");
    statusEl.innerHTML = `${ICONS.refresh}<span>${escapeHtml(message)}</span>`;
}

function errorMessage(error) {
    return error instanceof Error && error.message ? error.message : "An unexpected error occurred.";
}

function setDisabled(ids, disabled) {
    ids.forEach(id => {
        document.getElementById(id).disabled = disabled;
    });
}

function setRecordingsLoading(loading) {
    recordingsLoading = loading;
    setDisabled(["loadBtn", "recFrom", "recTo", "recFilterApplyBtn", "recFilterClearBtn"], loading);
    document.getElementById("recordingsArea").setAttribute("aria-busy", String(loading));
}

function updateTranscriptControls() {
    const blocked = transcriptsLoading || transcriptionRunning;
    setDisabled([
        "transcribeBtn",
        "loadTranscriptsBtn",
        "transFrom",
        "transTo",
        "transCategory",
        "transFilterApplyBtn",
        "transFilterClearBtn"
    ], blocked);
    document.getElementById("transcriptsArea").setAttribute("aria-busy", String(blocked));
}

function statusPill(status) {
    const value = (status || "").toUpperCase();
    if (value === "CACHED" || value === "COMPLETED" || value === "DONE") {
        return `<span class="pill-tag tag-teal">${ICONS.check}${escapeHtml(status)}</span>`;
    }
    if (value === "PENDING" || value === "QUEUED" || value === "PROCESSING") {
        return `<span class="pill-tag tag-amber">${ICONS.clock}${escapeHtml(status)}</span>`;
    }
    if (value === "FAILED" || value === "ERROR") {
        return `<span class="pill-tag tag-red">${ICONS.alert}${escapeHtml(status)}</span>`;
    }
    return `<span class="pill-tag tag-gray">${escapeHtml(status || "Unknown")}</span>`;
}

function transcribedPill(isTranscripted) {
    const transcribed = isTranscripted === true || isTranscripted === "true";
    return transcribed
        ? `<span class="pill-tag tag-teal">${ICONS.check}Transcribed</span>`
        : `<span class="pill-tag tag-amber">${ICONS.clock}Not yet</span>`;
}

function categoryPill(category) {
    const value = String(category || "UNKNOWN").trim().toUpperCase();
    const labels = {
        TECHNICAL: "Technical",
        HELP: "Help",
        ACCOUNT_BILLING: "Account & Billing",
        ORDER_SERVICE: "Order & Service",
        FEEDBACK_REQUEST: "Feedback & Request",
        UNKNOWN: "Unknown"
    };
    return `<span class="pill-tag tag-gray">${escapeHtml(labels[value] || value)}</span>`;
}

function renderPagination(container, totalItems, currentPage, pageSize, onPageChange) {
    container.innerHTML = "";
    if (totalItems === 0) return;

    const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
    const prevBtn = document.createElement("button");
    prevBtn.type = "button";
    prevBtn.className = "page-btn";
    prevBtn.innerHTML = ICONS.chevronLeft;
    prevBtn.setAttribute("aria-label", "Previous page");
    prevBtn.disabled = currentPage <= 1;
    prevBtn.addEventListener("click", () => onPageChange(currentPage - 1));
    container.appendChild(prevBtn);

    const maxButtons = 5;
    let start = Math.max(1, currentPage - Math.floor(maxButtons / 2));
    let end = Math.min(totalPages, start + maxButtons - 1);
    start = Math.max(1, end - maxButtons + 1);

    for (let page = start; page <= end; page++) {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "page-btn" + (page === currentPage ? " is-active" : "");
        btn.textContent = page;
        btn.setAttribute("aria-label", `Page ${page}`);
        if (page === currentPage) btn.setAttribute("aria-current", "page");
        btn.addEventListener("click", () => onPageChange(page));
        container.appendChild(btn);
    }

    const nextBtn = document.createElement("button");
    nextBtn.type = "button";
    nextBtn.className = "page-btn";
    nextBtn.innerHTML = ICONS.chevronRight;
    nextBtn.setAttribute("aria-label", "Next page");
    nextBtn.disabled = currentPage >= totalPages;
    nextBtn.addEventListener("click", () => onPageChange(currentPage + 1));
    container.appendChild(nextBtn);

    const info = document.createElement("span");
    info.className = "page-info";
    info.textContent = `${totalItems} total`;
    container.appendChild(info);
}

async function loadAllRecordings() {
    if (recordingsLoading) return;

    const requestId = ++recordingsRequestId;
    const statusEl = document.getElementById("status");
    setRecordingsLoading(true);
    setLoadingStatus(statusEl, "Loading recordings…");

    try {
        const {body} = await requestJson(RECORDINGS_URL, {method: "GET"});
        if (requestId !== recordingsRequestId) return;

        allRecordings = Array.isArray(body) ? body : [];
        recPage = 1;
        setStatus(statusEl, `Loaded ${allRecordings.length} recording(s).`);
        renderRecordingsPage();
    } catch (error) {
        if (requestId === recordingsRequestId) setStatus(statusEl, errorMessage(error), true);
    } finally {
        if (requestId === recordingsRequestId) setRecordingsLoading(false);
    }
}

async function filterRecordings() {
    if (recordingsLoading) return;

    const statusEl = document.getElementById("status");
    const from = dateInputToEpoch(document.getElementById("recFrom").value, false);
    const to = dateInputToEpoch(document.getElementById("recTo").value, true);

    if (from !== null && to !== null && from > to) {
        setStatus(statusEl, '"From" date must be before "To" date.', true);
        return;
    }

    const params = new URLSearchParams();
    if (from !== null) params.set("from", from);
    if (to !== null) params.set("to", to);

    const requestId = ++recordingsRequestId;
    setRecordingsLoading(true);
    setLoadingStatus(statusEl, "Filtering recordings…");

    try {
        const {body} = await requestJson(`${FILTER_RECORDINGS_URL}?${params}`, {method: "GET"});
        if (requestId !== recordingsRequestId) return;

        allRecordings = Array.isArray(body) ? body : [];
        recPage = 1;
        setStatus(statusEl, `Found ${allRecordings.length} recording(s) in range.`);
        renderRecordingsPage();
    } catch (error) {
        if (requestId === recordingsRequestId) setStatus(statusEl, errorMessage(error), true);
    } finally {
        if (requestId === recordingsRequestId) setRecordingsLoading(false);
    }
}

function clearRecordingsFilter() {
    if (recordingsLoading) return;
    document.getElementById("recFrom").value = "";
    document.getElementById("recTo").value = "";
    loadAllRecordings();
}

function renderRecordingsPage() {
    const area = document.getElementById("recordingsArea");
    const paginationEl = document.getElementById("recPagination");

    if (allRecordings.length === 0) {
        area.innerHTML = `
          <div class="empty-state">
            <div class="empty-icon">${ICONS.mic}</div>
            <h3>No recordings found</h3>
            <p>Once calls are captured they'll show up here, ready to transcribe.</p>
          </div>`;
        paginationEl.innerHTML = "";
        return;
    }

    const totalPages = Math.max(1, Math.ceil(allRecordings.length / REC_PAGE_SIZE));
    if (recPage > totalPages) recPage = totalPages;

    const startIdx = (recPage - 1) * REC_PAGE_SIZE;
    const pageItems = allRecordings.slice(startIdx, startIdx + REC_PAGE_SIZE);
    const rows = pageItems.map(rec => `
        <div class="rec-row">
          <div class="rec-icon">${ICONS.mic}</div>
          <div class="rec-main">
            <div class="rec-name">${escapeHtml(rec.recordingName)}</div>
            <div class="rec-meta">
              <span class="rec-meta-item">#${escapeHtml(rec.id)}</span>
              <span class="rec-meta-item">${ICONS.calendar}${formatDate(rec.createdAt)}</span>
              <span class="rec-meta-item">${ICONS.refresh}${formatDate(rec.updatedAt)}</span>
            </div>
          </div>
          <div class="rec-tags">
            ${statusPill(rec.status)}
            ${transcribedPill(rec.isTranscripted)}
          </div>
        </div>
    `).join("");

    area.innerHTML = `<div class="rec-list">${rows}</div>`;
    renderPagination(paginationEl, allRecordings.length, recPage, REC_PAGE_SIZE, page => {
        recPage = page;
        renderRecordingsPage();
    });
}

async function transcribeAll() {
    if (transcriptionRunning || transcriptsLoading) return;

    const statusEl = document.getElementById("transcribeStatus");
    const loader = document.getElementById("waveformLoader");
    transcriptionRunning = true;
    loader.hidden = false;
    setLoadingStatus(statusEl, "Transcribing pending recordings…");
    updateTranscriptControls();

    try {
        const {body, response} = await requestJson(TRANSCRIBE_ALL_URL, {method: "POST"});
        const count = Array.isArray(body) ? body.length : 0;
        const failedValue = Number.parseInt(response.headers.get("X-Insightline-Failed") || "0", 10);
        const failedCount = Number.isFinite(failedValue) && failedValue > 0 ? failedValue : 0;
        const message = failedCount > 0
            ? `Transcribed ${count} recording(s); ${failedCount} could not be transcribed.`
            : `Transcribed ${count} recording(s).`;
        await loadAllTranscripts(message);
    } catch (error) {
        setStatus(statusEl, errorMessage(error), true);
    } finally {
        transcriptionRunning = false;
        loader.hidden = true;
        updateTranscriptControls();
    }
}

async function loadAllTranscripts(successMessage) {
    if (transcriptsLoading) return;

    const requestId = ++transcriptsRequestId;
    const statusEl = document.getElementById("transcribeStatus");
    transcriptsLoading = true;
    updateTranscriptControls();
    setLoadingStatus(statusEl, "Loading transcripts…");

    try {
        const {body} = await requestJson(LOAD_TRANSCRIPTS_URL, {method: "GET"});
        if (requestId !== transcriptsRequestId) return;

        allTranscripts = Array.isArray(body) ? body : [];
        transcriptPage = 1;
        setStatus(statusEl, successMessage || `Loaded ${allTranscripts.length} transcript(s).`);
        renderTranscriptsPage();
    } catch (error) {
        if (requestId === transcriptsRequestId) setStatus(statusEl, errorMessage(error), true);
    } finally {
        if (requestId === transcriptsRequestId) {
            transcriptsLoading = false;
            updateTranscriptControls();
        }
    }
}

async function filterTranscripts() {
    if (transcriptionRunning || transcriptsLoading) return;

    const statusEl = document.getElementById("transcribeStatus");
    const from = dateInputToEpoch(document.getElementById("transFrom").value, false);
    const to = dateInputToEpoch(document.getElementById("transTo").value, true);
    const category = document.getElementById("transCategory").value;

    if (from !== null && to !== null && from > to) {
        setStatus(statusEl, '"From" date must be before "To" date.', true);
        return;
    }

    const params = new URLSearchParams();
    if (from !== null) params.set("from", from);
    if (to !== null) params.set("to", to);
    if (category && category !== "ALL") params.set("category", category);

    const requestId = ++transcriptsRequestId;
    transcriptsLoading = true;
    updateTranscriptControls();
    setLoadingStatus(statusEl, "Filtering transcripts…");

    try {
        const {body} = await requestJson(`${FILTER_TRANSCRIPTS_URL}?${params}`, {method: "GET"});
        if (requestId !== transcriptsRequestId) return;

        allTranscripts = Array.isArray(body) ? body : [];
        transcriptPage = 1;
        setStatus(statusEl, `Found ${allTranscripts.length} transcript(s) in range.`);
        renderTranscriptsPage();
    } catch (error) {
        if (requestId === transcriptsRequestId) setStatus(statusEl, errorMessage(error), true);
    } finally {
        if (requestId === transcriptsRequestId) {
            transcriptsLoading = false;
            updateTranscriptControls();
        }
    }
}

function clearTranscriptsFilter() {
    if (transcriptionRunning || transcriptsLoading) return;
    document.getElementById("transFrom").value = "";
    document.getElementById("transTo").value = "";
    document.getElementById("transCategory").value = "ALL";
    loadAllTranscripts();
}

function parseTranscript(content) {
    const lines = String(content || "").split("\n");
    const sections = [];
    let current = null;

    lines.forEach(line => {
        const headerMatch = line.match(/^#\s+(.*)/);
        if (headerMatch) {
            current = {title: headerMatch[1].trim(), lines: []};
            sections.push(current);
        } else if (current) {
            current.lines.push(line);
        } else {
            if (sections.length === 0) sections.push({title: "", lines: []});
            sections[sections.length - 1].lines.push(line);
        }
    });

    return sections.map(section => ({title: section.title, raw: section.lines.join("\n").trim()}));
}

function renderSection(section) {
    const titleLower = section.title.toLowerCase();
    const lines = section.raw.split("\n").map(line => line.trim()).filter(Boolean);
    const bulletLines = lines.filter(line => /^[-–]\s+/.test(line)).map(line => line.replace(/^[-–]\s+/, ""));
    const isBulletSection = bulletLines.length > 0 && bulletLines.length === lines.length;

    let iconSvg = ICONS.list;
    if (titleLower.includes("summary")) iconSvg = ICONS.sparkle;
    else if (titleLower.includes("key")) iconSvg = ICONS.check;
    else if (titleLower.includes("detail")) iconSvg = ICONS.grid;

    const labelHtml = section.title
        ? `<p class="tsection-label">${iconSvg}${escapeHtml(section.title)}</p>`
        : "";

    if (titleLower.includes("detail") && isBulletSection) {
        const chips = bulletLines.map(line => {
            const splitIdx = line.indexOf(":");
            const label = splitIdx > -1 ? line.slice(0, splitIdx).trim() : line;
            const value = splitIdx > -1 ? line.slice(splitIdx + 1).trim() : "";
            return `<div class="detail-chip">
                <div class="d-label">${escapeHtml(label)}</div>
                ${value ? `<div class="d-value">${escapeHtml(value)}</div>` : ""}
            </div>`;
        }).join("");
        return `<div>${labelHtml}<div class="detail-grid">${chips}</div></div>`;
    }

    if (isBulletSection) {
        const items = bulletLines.map(line => `<li>${ICONS.check}<span>${escapeHtml(line)}</span></li>`).join("");
        return `<div>${labelHtml}<ul class="keypoints">${items}</ul></div>`;
    }

    if (lines.length === 0) return "";
    return `<div>${labelHtml}<p class="fallback-text">${escapeHtml(lines.join("\n"))}</p></div>`;
}

function renderTranscriptsPage() {
    const area = document.getElementById("transcriptsArea");
    const paginationEl = document.getElementById("transcriptPagination");

    if (allTranscripts.length === 0) {
        area.innerHTML = `
          <div class="empty-state">
            <div class="empty-icon">${ICONS.doc}</div>
            <h3>No transcripts found</h3>
            <p>Transcribe pending recordings, then load transcripts to see them here.</p>
          </div>`;
        paginationEl.innerHTML = "";
        return;
    }

    const totalPages = Math.max(1, Math.ceil(allTranscripts.length / TRANSCRIPT_PAGE_SIZE));
    if (transcriptPage > totalPages) transcriptPage = totalPages;

    const startIdx = (transcriptPage - 1) * TRANSCRIPT_PAGE_SIZE;
    const pageItems = allTranscripts.slice(startIdx, startIdx + TRANSCRIPT_PAGE_SIZE);
    const cards = pageItems.map((transcript, index) => {
        const sections = parseTranscript(transcript.content);
        const summarySection = sections.find(section => section.title.toLowerCase().includes("summary"));
        const otherSections = sections.filter(section => section !== summarySection);
        const summaryHtml = summarySection
            ? `<div class="summary-block">${escapeHtml(summarySection.raw.replace(/\n+/g, " "))}</div>`
            : `<div class="summary-block">${escapeHtml((sections[0] && sections[0].raw) || "No summary available.")}</div>`;
        const extraHtml = (summarySection ? otherSections : sections.slice(1))
            .map(renderSection).filter(Boolean).join("");
        const cardId = `transcript-card-${startIdx + index}`;
        const detailsId = `${cardId}-details`;
        const recordingName = String(transcript.recordingName || "transcript");

        return `
          <div class="transcript-card" id="${cardId}">
            <div class="transcript-head">
              <div class="doc-icon">${ICONS.doc}</div>
              <div class="doc-name">${escapeHtml(recordingName)}</div>
              ${categoryPill(transcript.category)}
              <span class="pill-tag tag-teal">${ICONS.sparkle}AI generated</span>
            </div>
            <div class="transcript-summary">${summaryHtml}</div>
            ${extraHtml ? `
              <button class="expand-toggle" type="button" data-target="${cardId}" aria-controls="${detailsId}" aria-expanded="false" aria-label="Show details for ${escapeHtml(recordingName)}">
                <span class="expand-label"></span>
                ${ICONS.chevronDown}
              </button>
              <div class="transcript-extra" id="${detailsId}">${extraHtml}</div>
            ` : ""}
          </div>
        `;
    }).join("");

    area.innerHTML = `<div class="transcript-list">${cards}</div>`;
    area.querySelectorAll(".expand-toggle").forEach(btn => {
        btn.addEventListener("click", () => {
            const card = document.getElementById(btn.getAttribute("data-target"));
            if (!card) return;
            const expanded = card.classList.toggle("is-expanded");
            btn.setAttribute("aria-expanded", String(expanded));
            btn.setAttribute("aria-label", `${expanded ? "Hide" : "Show"} ${btn.getAttribute("aria-label").replace(/^(Show|Hide)\s+/, "")}`);
        });
    });

    renderPagination(paginationEl, allTranscripts.length, transcriptPage, TRANSCRIPT_PAGE_SIZE, page => {
        transcriptPage = page;
        renderTranscriptsPage();
    });
}

initTheme();
initTabs();
document.getElementById("themeToggle").addEventListener("click", toggleTheme);
document.getElementById("loadBtn").addEventListener("click", loadAllRecordings);
document.getElementById("transcribeBtn").addEventListener("click", transcribeAll);
document.getElementById("loadTranscriptsBtn").addEventListener("click", () => loadAllTranscripts());
document.getElementById("recFilterApplyBtn").addEventListener("click", filterRecordings);
document.getElementById("recFilterClearBtn").addEventListener("click", clearRecordingsFilter);
document.getElementById("transFilterApplyBtn").addEventListener("click", filterTranscripts);
document.getElementById("transFilterClearBtn").addEventListener("click", clearTranscriptsFilter);
