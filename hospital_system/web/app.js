/**
 * PulseCare Hospital Management System - Front-end Application Controller
 */

// Global State
const state = {
    stats: { totalPatients: 0, totalDoctors: 0, totalAppointments: 0, todayAppointments: 0 },
    patients: [],
    doctors: [],
    appointments: [],
    activeTab: 'overview'
};

// DOM Ready
document.addEventListener('DOMContentLoaded', () => {
    initClock();
    initNavigation();
    initModals();
    initForms();
    initFilters();
    setDefaultDate();
    loadAllData();
});

// ---------- Clock & Header ----------
function initClock() {
    const el = document.getElementById('header-datetime');
    function update() {
        const now = new Date();
        const options = { weekday: 'short', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' };
        if (el) el.textContent = `PulseCare Medical Center • Live Monitoring • ${now.toLocaleDateString(undefined, options)}`;
    }
    update();
    setInterval(update, 30000);
}

// ---------- Navigation & Tabs ----------
function initNavigation() {
    const navButtons = document.querySelectorAll('.nav-item');
    navButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetTab = btn.getAttribute('data-tab');
            switchTab(targetTab);
        });
    });

    // Quick action buttons in header & cards
    const quickBook = document.getElementById('btn-quick-book');
    if (quickBook) quickBook.addEventListener('click', () => switchTab('schedule'));

    const quickNewPatient = document.getElementById('btn-quick-new-patient');
    if (quickNewPatient) quickNewPatient.addEventListener('click', () => openModal('modal-patient'));

    const viewAllAppointments = document.getElementById('btn-view-all-appointments');
    if (viewAllAppointments) viewAllAppointments.addEventListener('click', () => switchTab('appointments'));

    const tabBookApp = document.getElementById('btn-tab-book-appointment');
    if (tabBookApp) tabBookApp.addEventListener('click', () => switchTab('schedule'));

    const quickAddDoc = document.getElementById('btn-quick-add-doctor');
    if (quickAddDoc) quickAddDoc.addEventListener('click', () => openModal('modal-doctor'));
}

function switchTab(tabName) {
    state.activeTab = tabName;

    // Update nav items
    document.querySelectorAll('.nav-item').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-tab') === tabName);
    });

    // Update tab panes
    document.querySelectorAll('.tab-pane').forEach(pane => {
        pane.classList.toggle('active', pane.id === `tab-${tabName}`);
    });

    // Update title
    const titles = {
        overview: 'Hospital Executive Dashboard',
        appointments: 'Appointments Management',
        patients: 'Patient Directory & Records',
        doctors: 'Medical Specialists & Staff',
        schedule: 'Schedule Clinical Appointment'
    };
    const titleEl = document.getElementById('page-title');
    if (titleEl) titleEl.textContent = titles[tabName] || 'Hospital Management System';
}

// ---------- Data Fetching & Sync ----------
async function loadAllData() {
    try {
        await Promise.all([
            fetchStats(),
            fetchPatients(),
            fetchDoctors(),
            fetchAppointments()
        ]);
        renderAll();
    } catch (err) {
        showToast('Error syncing hospital data: ' + err.message, 'error');
    }
}

async function fetchStats() {
    const res = await fetch('/api/stats');
    if (res.ok) {
        state.stats = await res.json();
    }
}

async function fetchPatients(search = '') {
    const url = search ? `/api/patients?search=${encodeURIComponent(search)}` : '/api/patients';
    const res = await fetch(url);
    if (res.ok) {
        state.patients = await res.json();
    }
}

async function fetchDoctors() {
    const res = await fetch('/api/doctors');
    if (res.ok) {
        state.doctors = await res.json();
    }
}

async function fetchAppointments() {
    const res = await fetch('/api/appointments');
    if (res.ok) {
        state.appointments = await res.json();
    }
}

// ---------- Render Pipeline ----------
function renderAll() {
    renderStats();
    renderOverviewAppointments();
    renderOverviewDoctors();
    renderAppointmentsTable();
    renderPatientsTable();
    renderDoctorsGrid();
    populateBookingDropdowns();
}

function renderStats() {
    document.getElementById('stat-total-patients').textContent = state.stats.totalPatients;
    document.getElementById('stat-total-doctors').textContent = state.stats.totalDoctors;
    document.getElementById('stat-total-appointments').textContent = state.stats.totalAppointments;
    document.getElementById('stat-today-appointments').textContent = state.stats.todayAppointments;

    document.getElementById('nav-patient-count').textContent = state.patients.length;
    document.getElementById('nav-doctor-count').textContent = state.doctors.length;
    document.getElementById('nav-appointment-count').textContent = state.appointments.length;
}

function renderOverviewAppointments() {
    const tbody = document.getElementById('overview-appointments-body');
    if (!tbody) return;

    if (state.appointments.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="empty-state">No appointments booked yet.</td></tr>`;
        return;
    }

    const todayStr = new Date().toISOString().split('T')[0];
    const previewList = state.appointments.slice(0, 5);

    tbody.innerHTML = previewList.map(a => {
        const isToday = a.date === todayStr;
        const statusBadge = isToday
            ? `<span class="status-pill today">● Today</span>`
            : `<span class="status-pill confirmed">● Confirmed</span>`;

        return `
            <tr>
                <td><strong>${escapeHtml(a.date)}</strong> at <span class="muted-text">${escapeHtml(a.time)}</span></td>
                <td><strong>${escapeHtml(a.patientName)}</strong></td>
                <td>${escapeHtml(a.doctorName)} <br><small class="muted-text">${escapeHtml(a.doctorSpecialization)}</small></td>
                <td>${statusBadge}</td>
                <td>
                    <button class="btn btn-danger-outline btn-sm" onclick="cancelAppointment(${a.id})">Cancel</button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderOverviewDoctors() {
    const container = document.getElementById('overview-doctors-list');
    if (!container) return;

    if (state.doctors.length === 0) {
        container.innerHTML = `<div class="empty-state">No doctors registered yet.</div>`;
        return;
    }

    const previewList = state.doctors.slice(0, 4);
    container.innerHTML = previewList.map(d => {
        const initials = d.name.replace('Dr. ', '').split(' ').map(n => n[0]).join('').substring(0, 2);
        return `
            <div class="doctor-quick-item">
                <div class="avatar-badge">${initials}</div>
                <div class="doctor-meta">
                    <div class="doctor-meta-name">${escapeHtml(d.name)}</div>
                    <div class="doctor-meta-spec">${escapeHtml(d.specialization)}</div>
                </div>
            </div>
        `;
    }).join('');
}

function renderAppointmentsTable(filterTerm = '') {
    const tbody = document.getElementById('appointments-table-body');
    if (!tbody) return;

    let list = state.appointments;
    if (filterTerm) {
        const term = filterTerm.toLowerCase();
        list = list.filter(a => 
            a.patientName.toLowerCase().includes(term) ||
            a.doctorName.toLowerCase().includes(term) ||
            a.doctorSpecialization.toLowerCase().includes(term) ||
            a.date.includes(term)
        );
    }

    if (list.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="empty-state">No matching appointments found.</td></tr>`;
        return;
    }

    const todayStr = new Date().toISOString().split('T')[0];

    tbody.innerHTML = list.map(a => {
        const isToday = a.date === todayStr;
        const statusBadge = isToday
            ? `<span class="status-pill today">● Today</span>`
            : `<span class="status-pill confirmed">● Scheduled</span>`;

        return `
            <tr>
                <td><strong class="muted-text">#${a.id}</strong></td>
                <td><strong>${escapeHtml(a.date)}</strong></td>
                <td>${escapeHtml(a.time)}</td>
                <td><strong>${escapeHtml(a.patientName)}</strong></td>
                <td>${escapeHtml(a.doctorName)}</td>
                <td><span class="doctor-spec-tag">${escapeHtml(a.doctorSpecialization)}</span></td>
                <td>${statusBadge}</td>
                <td>
                    <button class="btn btn-danger-outline btn-sm" onclick="cancelAppointment(${a.id})">Cancel</button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderPatientsTable() {
    const tbody = document.getElementById('patients-table-body');
    if (!tbody) return;

    if (state.patients.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" class="empty-state">No patients in records.</td></tr>`;
        return;
    }

    tbody.innerHTML = state.patients.map(p => `
        <tr>
            <td><strong class="muted-text">#${p.id}</strong></td>
            <td><strong>${escapeHtml(p.name)}</strong></td>
            <td>${p.age} yrs</td>
            <td>${escapeHtml(p.gender)}</td>
            <td>${escapeHtml(p.phone)}</td>
            <td>${escapeHtml(p.disease)}</td>
            <td>
                <button class="btn btn-secondary btn-sm" onclick="prefillBookingPatient(${p.id})">Book Visit</button>
            </td>
        </tr>
    `).join('');
}

function renderDoctorsGrid() {
    const grid = document.getElementById('doctors-card-grid');
    if (!grid) return;

    if (state.doctors.length === 0) {
        grid.innerHTML = `<div class="empty-state">No doctors registered yet.</div>`;
        return;
    }

    grid.innerHTML = state.doctors.map(d => {
        const initials = d.name.replace('Dr. ', '').split(' ').map(n => n[0]).join('').substring(0, 2);
        return `
            <div class="doctor-card">
                <div class="doctor-card-top">
                    <div class="avatar-badge">${initials}</div>
                    <div>
                        <div style="font-weight: 700; font-size: 1rem;">${escapeHtml(d.name)}</div>
                        <div class="muted-text" style="font-size: 0.8rem;">ID: #${d.id}</div>
                    </div>
                </div>
                <div class="doctor-spec-tag">${escapeHtml(d.specialization)}</div>
                <div class="doctor-contact">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg>
                    <span>${escapeHtml(d.phone)}</span>
                </div>
            </div>
        `;
    }).join('');
}

function populateBookingDropdowns() {
    const patientSel = document.getElementById('select-patient');
    const doctorSel = document.getElementById('select-doctor');

    if (patientSel) {
        const currentVal = patientSel.value;
        patientSel.innerHTML = `<option value="">-- Choose Registered Patient --</option>` +
            state.patients.map(p => `<option value="${p.id}">${escapeHtml(p.name)} (#${p.id})</option>`).join('');
        if (currentVal) patientSel.value = currentVal;
    }

    if (doctorSel) {
        const currentVal = doctorSel.value;
        doctorSel.innerHTML = `<option value="">-- Choose Specialist Doctor --</option>` +
            state.doctors.map(d => `<option value="${d.id}">${escapeHtml(d.name)} - ${escapeHtml(d.specialization)}</option>`).join('');
        if (currentVal) doctorSel.value = currentVal;
    }
}

// ---------- Modals & Forms ----------
function initModals() {
    const btnClosePatient = document.getElementById('btn-close-patient-modal');
    const btnCancelPatient = document.getElementById('btn-cancel-patient');
    const btnOpenPatient = document.getElementById('btn-open-patient-modal');

    if (btnOpenPatient) btnOpenPatient.addEventListener('click', () => openModal('modal-patient'));
    if (btnClosePatient) btnClosePatient.addEventListener('click', () => closeModal('modal-patient'));
    if (btnCancelPatient) btnCancelPatient.addEventListener('click', () => closeModal('modal-patient'));

    const btnCloseDoctor = document.getElementById('btn-close-doctor-modal');
    const btnCancelDoctor = document.getElementById('btn-cancel-doctor');
    const btnOpenDoctor = document.getElementById('btn-open-doctor-modal');

    if (btnOpenDoctor) btnOpenDoctor.addEventListener('click', () => openModal('modal-doctor'));
    if (btnCloseDoctor) btnCloseDoctor.addEventListener('click', () => closeModal('modal-doctor'));
    if (btnCancelDoctor) btnCancelDoctor.addEventListener('click', () => closeModal('modal-doctor'));

    // Backdrop click close
    document.querySelectorAll('.modal-overlay').forEach(overlay => {
        overlay.addEventListener('click', (e) => {
            if (e.target === overlay) overlay.classList.remove('open');
        });
    });
}

function openModal(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.add('open');
}

function closeModal(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.remove('open');
}

function initForms() {
    // Register Patient
    const formPatient = document.getElementById('form-register-patient');
    if (formPatient) {
        formPatient.addEventListener('submit', async (e) => {
            e.preventDefault();
            const name = document.getElementById('patient-name').value.trim();
            const age = parseInt(document.getElementById('patient-age').value);
            const gender = document.getElementById('patient-gender').value;
            const phone = document.getElementById('patient-phone').value.trim();
            const disease = document.getElementById('patient-disease').value.trim();

            try {
                const res = await fetch('/api/patients', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, age, gender, phone, disease })
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.error || 'Failed to register patient');

                showToast(`Patient ${name} registered successfully!`, 'success');
                formPatient.reset();
                closeModal('modal-patient');
                await loadAllData();
            } catch (err) {
                showToast(err.message, 'error');
            }
        });
    }

    // Add Doctor
    const formDoctor = document.getElementById('form-add-doctor');
    if (formDoctor) {
        formDoctor.addEventListener('submit', async (e) => {
            e.preventDefault();
            const name = document.getElementById('doctor-name').value.trim();
            const specialization = document.getElementById('doctor-specialization').value.trim();
            const phone = document.getElementById('doctor-phone').value.trim();

            try {
                const res = await fetch('/api/doctors', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ name, specialization, phone })
                });
                const data = await res.json();
                if (!res.ok) throw new Error(data.error || 'Failed to add doctor');

                showToast(`${name} added to medical staff!`, 'success');
                formDoctor.reset();
                closeModal('modal-doctor');
                await loadAllData();
            } catch (err) {
                showToast(err.message, 'error');
            }
        });
    }

    // Schedule Appointment
    const formSchedule = document.getElementById('form-schedule-appointment');
    if (formSchedule) {
        formSchedule.addEventListener('submit', async (e) => {
            e.preventDefault();
            const feedbackBox = document.getElementById('schedule-feedback');
            feedbackBox.style.display = 'none';

            const patientId = parseInt(document.getElementById('select-patient').value);
            const doctorId = parseInt(document.getElementById('select-doctor').value);
            const date = document.getElementById('input-appointment-date').value;
            const time = document.getElementById('input-appointment-time').value;

            try {
                const res = await fetch('/api/appointments', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ patientId, doctorId, date, time })
                });
                const data = await res.json();
                if (!res.ok) {
                    feedbackBox.className = 'form-notification-box error';
                    feedbackBox.textContent = data.error || 'Booking conflict or error.';
                    feedbackBox.style.display = 'block';
                    return;
                }

                showToast('Appointment successfully scheduled!', 'success');
                feedbackBox.className = 'form-notification-box success';
                feedbackBox.textContent = 'Appointment confirmed and stored in database.';
                feedbackBox.style.display = 'block';

                await loadAllData();
                setTimeout(() => {
                    feedbackBox.style.display = 'none';
                    switchTab('appointments');
                }, 1200);
            } catch (err) {
                feedbackBox.className = 'form-notification-box error';
                feedbackBox.textContent = 'Network or server error: ' + err.message;
                feedbackBox.style.display = 'block';
            }
        });
    }
}

// Cancel Appointment
window.cancelAppointment = async function(id) {
    if (!confirm(`Are you sure you want to cancel appointment #${id}?`)) return;

    try {
        const res = await fetch(`/api/appointments?id=${id}`, { method: 'DELETE' });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Could not cancel appointment');

        showToast(`Appointment #${id} cancelled.`, 'success');
        await loadAllData();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

window.prefillBookingPatient = function(patientId) {
    switchTab('schedule');
    const select = document.getElementById('select-patient');
    if (select) select.value = patientId;
};

// ---------- Filters ----------
function initFilters() {
    const inputAppFilter = document.getElementById('input-appointment-filter');
    if (inputAppFilter) {
        inputAppFilter.addEventListener('input', (e) => {
            renderAppointmentsTable(e.target.value.trim());
        });
    }

    const inputPatientSearch = document.getElementById('input-patient-search');
    if (inputPatientSearch) {
        inputPatientSearch.addEventListener('input', async (e) => {
            const term = e.target.value.trim();
            await fetchPatients(term);
            renderPatientsTable();
        });
    }
}

function setDefaultDate() {
    const dateInput = document.getElementById('input-appointment-date');
    if (dateInput) {
        const d = new Date();
        d.setDate(d.getDate() + 1);
        dateInput.value = d.toISOString().split('T')[0];
    }
}

// ---------- UI Utilities ----------
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;

    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(10px)';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
