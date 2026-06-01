# MohamyPhone Admin Server

خادم إدارة العملاء والتراخيص لتطبيق **MohamyPhone**.

## وظيفة الخادم
- إدارة حسابات العملاء (المحامين) من لوحة ويب.
- إنشاء وتحديث وإيقاف/حظر الحسابات.
- إدارة التراخيص وربط الأجهزة.
- تفعيل الترخيص للتطبيق عبر API.

## مهم
- لا يتم حفظ بيانات القضايا/العملاء القانونية الخاصة بالتطبيق في هذا الخادم.
- الخادم يدير فقط حسابات الدخول والترخيص.

## 1) التشغيل المحلي
```bash
cd admin-server
npm install
cp .env.example .env
# عدل القيم داخل .env
npm start
```

ثم افتح:
- [http://localhost:8080](http://localhost:8080)

## 2) متغيرات البيئة
أنشئ ملف `.env` داخل `admin-server`:

```env
PORT=8080
NODE_ENV=production

# مطلوب في الإنتاج: 24 حرف+ على الأقل
JWT_SECRET=CHANGE_THIS_TO_A_LONG_RANDOM_SECRET_32CHARS_MIN

# إنشاء أدمن تلقائي أول مرة فقط إذا لم يوجد أدمن
ADMIN_BOOTSTRAP_USERNAME=admin
ADMIN_BOOTSTRAP_PASSWORD=CHANGE_THIS_NOW_12345

# مسار قاعدة بيانات sqlite
DB_FILE=./data/admin.db

# السماح بالدومينات (مفصولة بفاصلة) مثال:
# CORS_ORIGINS=https://admin.example.com,https://example.com
# أو * لكل الدومينات (غير مفضل إنتاجياً)
CORS_ORIGINS=*

# عند العمل خلف reverse proxy
TRUST_PROXY=true
```

## 3) النشر على أي سيرفر + دومين
يمكنك نشره على VPS وربطه بأي دومين عبر Nginx reverse proxy.

### مثال Nginx
```nginx
server {
  listen 80;
  server_name admin.example.com;

  location / {
    proxy_pass http://127.0.0.1:8080;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
  }
}
```

بعدها فعّل SSL عبر Certbot.

## 4) تشغيل دائم (PM2)
```bash
npm i -g pm2
cd admin-server
pm2 start src/server.js --name mohamy-admin
pm2 save
pm2 startup
```

## 5) API الأساسية
### Admin
- `POST /api/admin/login`
- `GET /api/admin/me`
- `GET /api/admin/lawyers`
- `POST /api/admin/lawyers`
- `PATCH /api/admin/lawyers/:id`
- `PATCH /api/admin/lawyers/:id/status`
- `DELETE /api/admin/lawyers/:id`
- `PATCH /api/admin/licenses/:id`
- `POST /api/admin/lawyers/:id/reset-password`
- `POST /api/admin/licenses/:id/reset-device`

### App License
- `POST /api/license/activate`
- `POST /api/license/check`

## 6) ملاحظات أمان
- غيّر `JWT_SECRET` وبيانات الأدمن قبل الإنتاج.
- لا تستخدم `CORS_ORIGINS=*` في الإنتاج إلا إذا كنت تحتاج ذلك فعلياً.
- احفظ نسخة احتياطية دورية من `admin.db`.
