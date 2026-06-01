let adminToken = '';
let adminUsername = '';
let cachedLawyers = [];
let filteredLawyers = [];

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function showMsg(id, type, text) {
  const el = document.getElementById(id);
  if (!el) return;
  if (!text) {
    el.className = 'msg';
    el.textContent = '';
    return;
  }
  el.className = type === 'err' ? 'msg err' : 'msg ok';
  el.textContent = text;
}

async function api(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (adminToken) headers.Authorization = `Bearer ${adminToken}`;

  const res = await fetch(path, { ...options, headers });
  const data = await res.json().catch(() => ({}));

  if (!res.ok) {
    throw new Error(data.message || data.error || `HTTP ${res.status}`);
  }

  return data;
}

function statusBadge(status) {
  const cls = status === 'active' ? 'st-active' : status === 'blocked' ? 'st-blocked' : 'st-inactive';
  return `<span class="pill ${cls}">${escapeHtml(status)}</span>`;
}

function licenseSummary(license) {
  if (!license) return '<span class="muted">لا يوجد</span>';
  const expires = license.expires_at || 'غير محدد';
  return [
    `<div><strong>${escapeHtml(license.license_key || '-')}</strong></div>`,
    `<div class="muted">status: ${escapeHtml(license.status || '-')}</div>`,
    `<div class="muted">max_devices: ${escapeHtml(license.max_devices || '-')}</div>`,
    `<div class="muted">expires_at: ${escapeHtml(expires)}</div>`,
  ].join('');
}

function devicesSummary(devices) {
  if (!devices || devices.length === 0) return '<span class="muted">لا يوجد</span>';
  return devices
    .map((device) => {
      const title = device.device_name || device.device_id;
      return [
        `<div>${escapeHtml(title)}</div>`,
        `<div class="muted">${escapeHtml(device.platform || '-')} | ${escapeHtml(device.app_version || '-')}</div>`,
      ].join('');
    })
    .join('<hr style="border:none;border-top:1px solid #eef2f8;margin:8px 0;" />');
}

function getLastCheck(devices) {
  if (!devices || devices.length === 0) return '-';
  return devices[0]?.last_check_at || '-';
}

function rowActions(lawyer) {
  const hasLicense = Boolean(lawyer.license);
  return `
    <div class="row-actions">
      <button type="button" class="btn-soft" onclick="setStatus(${lawyer.id}, 'active')">تفعيل</button>
      <button type="button" class="btn-warning" onclick="setStatus(${lawyer.id}, 'inactive')">تعليق</button>
      <button type="button" class="btn-danger" onclick="setStatus(${lawyer.id}, 'blocked')">حظر</button>
      <button type="button" class="btn-secondary" onclick="openLawyerModal(${lawyer.id})">تعديل بيانات</button>
      <button type="button" class="btn-secondary" onclick="resetPassword(${lawyer.id})">تغيير الباسورد</button>
      ${hasLicense ? `<button type="button" class="btn-secondary" onclick="openLicenseModal(${lawyer.id})">تعديل الترخيص</button>` : ''}
      ${hasLicense ? `<button type="button" class="btn-secondary" onclick="resetDevice(${lawyer.license.id})">فك الأجهزة</button>` : ''}
      <button type="button" class="btn-danger" onclick="deleteLawyer(${lawyer.id})">حذف</button>
    </div>
  `;
}

function updateStats(list) {
  const total = list.length;
  const active = list.filter((x) => x.status === 'active').length;
  const blocked = list.filter((x) => x.status === 'blocked').length;
  const devices = list.reduce((acc, x) => acc + (x.license?.devices?.length || 0), 0);

  document.getElementById('statTotal').textContent = String(total);
  document.getElementById('statActive').textContent = String(active);
  document.getElementById('statBlocked').textContent = String(blocked);
  document.getElementById('statDevices').textContent = String(devices);
}

function renderLawyersTable(list) {
  const body = document.getElementById('lawyersBody');
  if (!list || list.length === 0) {
    body.innerHTML = '<tr><td colspan="8" class="muted">لا يوجد نتائج مطابقة.</td></tr>';
    updateStats([]);
    return;
  }

  body.innerHTML = list
    .map((lawyer) => {
      const license = lawyer.license || null;
      const devices = license?.devices || [];
      return `
        <tr>
          <td>${escapeHtml(lawyer.id)}</td>
          <td>
            <strong>${escapeHtml(lawyer.name || '-')}</strong>
            <div class="muted">${escapeHtml(lawyer.phone || '-')}</div>
            <div class="muted">${escapeHtml(lawyer.notes || '-')}</div>
          </td>
          <td>${escapeHtml(lawyer.username || '-')}</td>
          <td>${statusBadge(lawyer.status)}</td>
          <td>${licenseSummary(license)}</td>
          <td>${devicesSummary(devices)}</td>
          <td>${escapeHtml(getLastCheck(devices))}</td>
          <td>${rowActions(lawyer)}</td>
        </tr>
      `;
    })
    .join('');

  updateStats(list);
}

function applySearch() {
  const query = document.getElementById('searchInput').value.trim().toLowerCase();
  if (!query) {
    filteredLawyers = [...cachedLawyers];
    renderLawyersTable(filteredLawyers);
    return;
  }

  filteredLawyers = cachedLawyers.filter((lawyer) => {
    const blob = `${lawyer.name || ''} ${lawyer.username || ''} ${lawyer.phone || ''} ${lawyer.notes || ''}`.toLowerCase();
    return blob.includes(query);
  });

  renderLawyersTable(filteredLawyers);
}

async function loadLawyers() {
  const body = document.getElementById('lawyersBody');
  body.innerHTML = '<tr><td colspan="8" class="muted">جاري التحميل...</td></tr>';
  try {
    const data = await api('/api/admin/lawyers');
    cachedLawyers = data.lawyers || [];
    filteredLawyers = [...cachedLawyers];
    applySearch();
  } catch (err) {
    body.innerHTML = `<tr><td colspan="8" class="muted">خطأ: ${escapeHtml(err.message)}</td></tr>`;
  }
}

function findLawyerById(id) {
  return cachedLawyers.find((x) => Number(x.id) === Number(id));
}

async function setStatus(id, status) {
  const ok = confirm(`تأكيد تغيير الحالة إلى ${status}؟`);
  if (!ok) return;
  try {
    await api(`/api/admin/lawyers/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    });
    await loadLawyers();
  } catch (err) {
    alert(err.message);
  }
}

function openLawyerModal(id) {
  const lawyer = findLawyerById(id);
  if (!lawyer) return;

  document.getElementById('editLawyerId').value = String(lawyer.id);
  document.getElementById('editName').value = lawyer.name || '';
  document.getElementById('editPhone').value = lawyer.phone || '';
  document.getElementById('editUsername').value = lawyer.username || '';
  document.getElementById('editNotes').value = lawyer.notes || '';
  document.getElementById('lawyerModal').showModal();
}

async function saveLawyer() {
  const id = Number(document.getElementById('editLawyerId').value || '0');
  const payload = {
    name: document.getElementById('editName').value.trim(),
    phone: document.getElementById('editPhone').value.trim(),
    username: document.getElementById('editUsername').value.trim(),
    notes: document.getElementById('editNotes').value.trim(),
  };

  if (!payload.name || !payload.username) {
    alert('الاسم و username مطلوبان.');
    return;
  }

  try {
    await api(`/api/admin/lawyers/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
    document.getElementById('lawyerModal').close();
    await loadLawyers();
  } catch (err) {
    alert(err.message);
  }
}

function openLicenseModal(lawyerId) {
  const lawyer = findLawyerById(lawyerId);
  if (!lawyer || !lawyer.license) return;

  document.getElementById('editLicenseId').value = String(lawyer.license.id);
  document.getElementById('editLicenseStatus').value = lawyer.license.status || 'active';
  document.getElementById('editLicenseDevices').value = String(lawyer.license.max_devices || 1);
  document.getElementById('editLicenseExpires').value = lawyer.license.expires_at || '';
  document.getElementById('licenseModal').showModal();
}

async function saveLicense() {
  const licenseId = Number(document.getElementById('editLicenseId').value || '0');
  const payload = {
    status: document.getElementById('editLicenseStatus').value,
    max_devices: Number(document.getElementById('editLicenseDevices').value || '1'),
    expires_at: document.getElementById('editLicenseExpires').value.trim(),
  };

  try {
    await api(`/api/admin/licenses/${licenseId}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
    document.getElementById('licenseModal').close();
    await loadLawyers();
  } catch (err) {
    alert(err.message);
  }
}

async function resetPassword(id) {
  const value = prompt('أدخل كلمة المرور الجديدة (8 أحرف على الأقل)');
  if (!value) return;
  try {
    await api(`/api/admin/lawyers/${id}/reset-password`, {
      method: 'POST',
      body: JSON.stringify({ new_password: value }),
    });
    alert('تم تغيير كلمة المرور.');
  } catch (err) {
    alert(err.message);
  }
}

async function resetDevice(licenseId) {
  const ok = confirm('سيتم فك ربط كل الأجهزة من هذا الترخيص. متابعة؟');
  if (!ok) return;

  try {
    await api(`/api/admin/licenses/${licenseId}/reset-device`, { method: 'POST' });
    await loadLawyers();
  } catch (err) {
    alert(err.message);
  }
}

async function deleteLawyer(id) {
  const ok = confirm('سيتم حذف العميل وجميع بيانات ترخيصه وأجهزته. متابعة؟');
  if (!ok) return;

  try {
    await api(`/api/admin/lawyers/${id}`, { method: 'DELETE' });
    await loadLawyers();
  } catch (err) {
    alert(err.message);
  }
}

function showPanel() {
  document.getElementById('loginShell').style.display = 'none';
  document.getElementById('panel').style.display = 'block';
  document.getElementById('adminWho').textContent = `الأدمن: ${adminUsername}`;
}

function showLogin() {
  document.getElementById('panel').style.display = 'none';
  document.getElementById('loginShell').style.display = 'grid';
}

async function login() {
  const username = document.getElementById('adminUser').value.trim();
  const password = document.getElementById('adminPass').value;

  showMsg('loginMsg', '', '');

  if (!username || !password) {
    showMsg('loginMsg', 'err', 'اسم المستخدم وكلمة المرور مطلوبان.');
    return;
  }

  try {
    const data = await api('/api/admin/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });

    adminToken = data.token;
    adminUsername = data.username || username;

    showPanel();
    await loadLawyers();
  } catch (err) {
    showMsg('loginMsg', 'err', err.message);
  }
}

function logout() {
  adminToken = '';
  adminUsername = '';
  cachedLawyers = [];
  filteredLawyers = [];
  document.getElementById('lawyersBody').innerHTML = '';
  showLogin();
}

async function addLawyer() {
  const payload = {
    name: document.getElementById('name').value.trim(),
    phone: document.getElementById('phone').value.trim(),
    username: document.getElementById('username').value.trim(),
    password: document.getElementById('password').value,
    status: document.getElementById('status').value,
    max_devices: Number(document.getElementById('maxDevices').value || '1'),
    expires_at: document.getElementById('expiresAt').value.trim() || null,
    license_status: document.getElementById('licenseStatus').value,
    notes: document.getElementById('notes').value.trim(),
  };

  if (!payload.name || !payload.username || !payload.password) {
    showMsg('addMsg', 'err', 'الاسم و username وكلمة المرور حقول مطلوبة.');
    return;
  }

  try {
    const result = await api('/api/admin/lawyers', {
      method: 'POST',
      body: JSON.stringify(payload),
    });

    showMsg('addMsg', 'ok', `تم إنشاء العميل بنجاح. license_key=${result.license_key}`);

    ['name', 'phone', 'username', 'password', 'expiresAt', 'notes'].forEach((id) => {
      const el = document.getElementById(id);
      if (el) el.value = '';
    });

    document.getElementById('status').value = 'active';
    document.getElementById('licenseStatus').value = 'active';
    document.getElementById('maxDevices').value = '1';

    await loadLawyers();
  } catch (err) {
    showMsg('addMsg', 'err', err.message);
  }
}

window.setStatus = setStatus;
window.openLawyerModal = openLawyerModal;
window.openLicenseModal = openLicenseModal;
window.resetPassword = resetPassword;
window.resetDevice = resetDevice;
window.deleteLawyer = deleteLawyer;

document.getElementById('loginBtn').addEventListener('click', login);
document.getElementById('adminPass').addEventListener('keydown', (event) => {
  if (event.key === 'Enter') login();
});
document.getElementById('refreshBtn').addEventListener('click', loadLawyers);
document.getElementById('logoutBtn').addEventListener('click', logout);
document.getElementById('addLawyerBtn').addEventListener('click', addLawyer);
document.getElementById('searchInput').addEventListener('input', applySearch);
document.getElementById('clearSearchBtn').addEventListener('click', () => {
  document.getElementById('searchInput').value = '';
  applySearch();
});

document.getElementById('saveLawyerBtn').addEventListener('click', saveLawyer);
document.getElementById('cancelLawyerBtn').addEventListener('click', () => document.getElementById('lawyerModal').close());
document.getElementById('saveLicenseBtn').addEventListener('click', saveLicense);
document.getElementById('cancelLicenseBtn').addEventListener('click', () => document.getElementById('licenseModal').close());
