let adminToken = '';
let cachedLawyers = [];

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
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
  const cls = status === 'active' ? 'active' : status === 'blocked' ? 'blocked' : 'inactive';
  return `<span class="pill ${cls}">${escapeHtml(status)}</span>`;
}

function licenseSummary(lic) {
  if (!lic) return '-';
  const expires = lic.expires_at || 'غير محدد';
  return [
    `<div><strong>${escapeHtml(lic.license_key || '-')}</strong></div>`,
    `<div class="muted">status: ${escapeHtml(lic.status || '-')}</div>`,
    `<div class="muted">max_devices: ${escapeHtml(lic.max_devices || '-')}</div>`,
    `<div class="muted">expires_at: ${escapeHtml(expires)}</div>`,
  ].join('');
}

function devicesSummary(devices) {
  if (!devices || devices.length === 0) return '-';
  return devices
    .map((d) => {
      const title = d.device_name || d.device_id;
      return `<div>
        ${escapeHtml(title)}
        <div class="muted">${escapeHtml(d.platform || '')} | ${escapeHtml(d.app_version || '')}</div>
        <div class="muted">last_check: ${escapeHtml(d.last_check_at || '-')}</div>
      </div>`;
    })
    .join('');
}

function rowActions(lawyer) {
  const statusButtons = `
    <button onclick="setStatus(${lawyer.id}, 'active')">تفعيل</button>
    <button class="warn" onclick="setStatus(${lawyer.id}, 'blocked')">حظر</button>
    <button class="secondary" onclick="setStatus(${lawyer.id}, 'inactive')">تعليق</button>
  `;

  const licenseButtons = lawyer.license
    ? `<button class="secondary" onclick="editLicense(${lawyer.id})">تعديل الترخيص</button>
       <button class="secondary" onclick="resetDevice(${lawyer.license.id})">Reset device</button>`
    : '';

  return `
    <div class="row">
      ${statusButtons}
      <button class="secondary" onclick="editLawyer(${lawyer.id})">تعديل البيانات</button>
      <button class="secondary" onclick="resetPassword(${lawyer.id})">Reset password</button>
      ${licenseButtons}
      <button class="danger" onclick="deleteLawyer(${lawyer.id})">حذف</button>
    </div>
  `;
}

async function loadLawyers() {
  const body = document.getElementById('lawyersBody');
  body.innerHTML = '<tr><td colspan="9">جاري التحميل...</td></tr>';
  try {
    const data = await api('/api/admin/lawyers');
    cachedLawyers = data.lawyers || [];
    if (cachedLawyers.length === 0) {
      body.innerHTML = '<tr><td colspan="9">لا يوجد عملاء بعد.</td></tr>';
      return;
    }

    body.innerHTML = cachedLawyers
      .map((l) => {
        const lic = l.license || null;
        const devices = lic?.devices || [];
        const lastCheck = devices[0]?.last_check_at || '-';
        return `
          <tr>
            <td>${escapeHtml(l.id)}</td>
            <td>
              ${escapeHtml(l.name)}
              <div class="muted">${escapeHtml(l.phone || '')}</div>
            </td>
            <td>${escapeHtml(l.username)}</td>
            <td>${statusBadge(l.status)}</td>
            <td>${escapeHtml(l.notes || '-')}</td>
            <td>${licenseSummary(lic)}</td>
            <td>${devicesSummary(devices)}</td>
            <td>${escapeHtml(lastCheck)}</td>
            <td>${rowActions(l)}</td>
          </tr>
        `;
      })
      .join('');
  } catch (err) {
    body.innerHTML = `<tr><td colspan="9">خطأ: ${escapeHtml(err.message)}</td></tr>`;
  }
}

async function setStatus(id, status) {
  const ok = confirm(`تغيير الحالة إلى ${status}؟`);
  if (!ok) return;
  try {
    await api(`/api/admin/lawyers/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) });
    await loadLawyers();
  } catch (err) {
    alert(err.message);
  }
}

async function editLawyer(id) {
  const lawyer = cachedLawyers.find((x) => x.id === id);
  if (!lawyer) return;

  const name = prompt('اسم العميل/المحامي', lawyer.name || '');
  if (name === null) return;
  const username = prompt('اسم المستخدم', lawyer.username || '');
  if (username === null) return;
  const phone = prompt('رقم الهاتف', lawyer.phone || '');
  if (phone === null) return;
  const notes = prompt('اسم المكتب / ملاحظات', lawyer.notes || '');
  if (notes === null) return;

  try {
    await api(`/api/admin/lawyers/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ name: name.trim(), username: username.trim(), phone: phone.trim(), notes: notes.trim() }),
    });
    await loadLawyers();
  } catch (err) {
    alert(err.message);
  }
}

async function editLicense(lawyerId) {
  const lawyer = cachedLawyers.find((x) => x.id === lawyerId);
  if (!lawyer || !lawyer.license) return;

  const maxDevicesRaw = prompt('max_devices', String(lawyer.license.max_devices || 1));
  if (maxDevicesRaw === null) return;
  const status = prompt('status (active|blocked|inactive)', lawyer.license.status || 'active');
  if (status === null) return;
  const expiresAt = prompt('expires_at (YYYY-MM-DD أو اتركه فارغ)', lawyer.license.expires_at || '');
  if (expiresAt === null) return;

  try {
    await api(`/api/admin/licenses/${lawyer.license.id}`, {
      method: 'PATCH',
      body: JSON.stringify({
        max_devices: Number(maxDevicesRaw || '1'),
        status: status.trim(),
        expires_at: expiresAt.trim(),
      }),
    });
    await loadLawyers();
  } catch (err) {
    alert(err.message);
  }
}

async function resetPassword(id) {
  const value = prompt('أدخل كلمة المرور الجديدة (8 أحرف على الأقل)');
  if (!value) return;
  try {
    await api(`/api/admin/lawyers/${id}/reset-password`, { method: 'POST', body: JSON.stringify({ new_password: value }) });
    alert('تم التحديث');
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
  const ok = confirm('سيتم حذف العميل/المحامي وكل تراخيصه وأجهزته. متابعة؟');
  if (!ok) return;
  try {
    await api(`/api/admin/lawyers/${id}`, { method: 'DELETE' });
    await loadLawyers();
  } catch (err) {
    alert(err.message);
  }
}

window.setStatus = setStatus;
window.editLawyer = editLawyer;
window.editLicense = editLicense;
window.resetPassword = resetPassword;
window.resetDevice = resetDevice;
window.deleteLawyer = deleteLawyer;

document.getElementById('loginBtn').addEventListener('click', async () => {
  const username = document.getElementById('adminUser').value.trim();
  const password = document.getElementById('adminPass').value;
  const msg = document.getElementById('loginMsg');
  msg.textContent = '...';
  try {
    const data = await api('/api/admin/login', { method: 'POST', body: JSON.stringify({ username, password }) });
    adminToken = data.token;
    document.getElementById('loginCard').style.display = 'none';
    document.getElementById('panel').style.display = 'block';
    await loadLawyers();
  } catch (err) {
    msg.textContent = err.message;
  }
});

document.getElementById('refreshBtn').addEventListener('click', loadLawyers);

document.getElementById('addLawyerBtn').addEventListener('click', async () => {
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
  const msg = document.getElementById('addMsg');
  msg.textContent = '...';
  try {
    const data = await api('/api/admin/lawyers', { method: 'POST', body: JSON.stringify(payload) });
    msg.textContent = `تمت الإضافة. license_key=${data.license_key}`;

    ['name', 'phone', 'username', 'password', 'expiresAt', 'notes'].forEach((id) => {
      const el = document.getElementById(id);
      if (el) el.value = '';
    });
    document.getElementById('maxDevices').value = '1';
    document.getElementById('status').value = 'active';
    document.getElementById('licenseStatus').value = 'active';

    await loadLawyers();
  } catch (err) {
    msg.textContent = err.message;
  }
});
