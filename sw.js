// تمرينتي — Service Worker
// لما تعمل أي تحديث في الكود: زوّد رقم CACHE_VERSION عشان المستخدمين ياخدوا التحديث أوتوماتيك
const CACHE_VERSION = 'v11';
const CACHE = 'tamreeni-' + CACHE_VERSION;

const APP_SHELL = [
  './app.html',
  './manifest.json',
  './icon-192.png',
  './icon-512.png',
  './tefa-logo-small.png',
  './tefa-logo.png'
];

self.addEventListener('install', e => {
  self.skipWaiting();
  e.waitUntil(
    caches.open(CACHE).then(c => c.addAll(APP_SHELL)).catch(() => {})
  );
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;

  // Network-first: يجيب أحدث نسخة أول ما يبقى فيه نت، ويرجع للكاش لو النت واقع
  e.respondWith(
    fetch(e.request).then(res => {
      if (res && res.ok) {
        const clone = res.clone();
        caches.open(CACHE).then(c => c.put(e.request, clone));
      }
      return res;
    }).catch(() => caches.match(e.request))
  );
});

let restTimer = null;

self.addEventListener('message', e => {
  if (!e.data) return;

  if (e.data.type === 'CHECK_UPDATE') {
    self.registration.update();
  }

  if (e.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }

  if (e.data.type === 'START_REST') {
    if (restTimer) { clearTimeout(restTimer); restTimer = null; }
    const ms = e.data.seconds * 1000;
    const icon = e.data.icon || './icon-192.png';
    restTimer = setTimeout(() => {
      restTimer = null;
      self.clients.matchAll().then(clients => {
        clients.forEach(c => c.postMessage({ type: 'REST_DONE' }));
      });
      self.registration.showNotification('تمرينتي 💪', {
        body: 'انتهى وقت الراحة — ابدأ السيت الجديد!',
        icon,
        badge: icon,
        vibrate: [300, 100, 300, 100, 300],
        tag: 'rest-done',
        renotify: true,
        requireInteraction: true,
        silent: false,
        actions: [{ action: 'ok', title: '✓ جاهز' }]
      });
    }, ms);
  }

  if (e.data.type === 'CANCEL_REST') {
    if (restTimer) { clearTimeout(restTimer); restTimer = null; }
  }

  if (e.data.type === 'CACHE_PAGE') {
    caches.open(CACHE).then(c => c.add(e.data.url));
  }
});

self.addEventListener('notificationclick', e => {
  e.notification.close();
  e.waitUntil(self.clients.matchAll({ type: 'window' }).then(clients => {
    if (clients.length) clients[0].focus();
    else self.clients.openWindow('./app.html');
  }));
});
