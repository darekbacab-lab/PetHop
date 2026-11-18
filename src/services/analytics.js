// Google Analytics 4: solo se carga con el ID configurado y con el consentimiento de cookies.
// El ID se define con la variable VITE_GA_MEASUREMENT_ID (por ejemplo, G-XXXXXXXXXX).
// Sin ID no se carga nada, así que la página no hace peticiones rotas.
const MEASUREMENT_ID = import.meta.env.VITE_GA_MEASUREMENT_ID;
let loaded = false;

function consentAccepted() {
  try {
    return localStorage.getItem('pethop-cookie-consent') === 'accepted';
  } catch {
    return false;
  }
}

export function initAnalytics() {
  if (loaded || !MEASUREMENT_ID || !consentAccepted()) return;
  window.dataLayer = window.dataLayer || [];
  window.gtag = function (...args) { window.dataLayer.push(args); };
  const script = document.createElement('script');
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${MEASUREMENT_ID}`;
  document.head.appendChild(script);
  window.gtag('js', new Date());
  window.gtag('config', MEASUREMENT_ID, { anonymize_ip: true });
  loaded = true;
}

export function trackPageView(path) {
  if (!loaded || typeof window.gtag !== 'function') return;
  window.gtag('config', MEASUREMENT_ID, { page_path: path });
}
