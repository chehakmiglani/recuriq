import React, { useEffect, useState } from 'react';
import { LoaderCircle } from 'lucide-react';

const PROBE_URL = '/api/customer/plans/all';   // public and cheap
const PROBE_TIMEOUT_MS = 6000;
const RETRY_MS = 4000;

/** True when the API itself answered (not a proxy error page or a timeout). */
async function serverIsUp(): Promise<boolean> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), PROBE_TIMEOUT_MS);
  try {
    const res = await fetch(PROBE_URL, { signal: controller.signal });
    return res.ok && (res.headers.get('content-type') ?? '').includes('application/json');
  } catch {
    return false;
  } finally {
    clearTimeout(timer);
  }
}

/**
 * Free hosting puts the API to sleep when idle. While it wakes up, show a notice
 * instead of an empty page, then reload once it responds so the app starts cleanly.
 */
export const ServerWakeNotice: React.FC = () => {
  const [waking, setWaking] = useState(false);

  useEffect(() => {
    let cancelled = false;
    let sawDown = false;
    let timer: ReturnType<typeof setTimeout> | undefined;

    const check = () => {
      serverIsUp().then((up) => {
        if (cancelled) return;
        if (up) {
          if (sawDown) window.location.reload();
          return;
        }
        sawDown = true;
        setWaking(true);
        timer = setTimeout(check, RETRY_MS);
      });
    };
    check();

    return () => {
      cancelled = true;
      if (timer) clearTimeout(timer);
    };
  }, []);

  if (!waking) return null;

  return (
    <div
      role="status"
      style={{
        position: 'fixed', top: 0, left: 0, right: 0, zIndex: 2000,
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px',
        padding: '10px 16px', background: '#0f172a', color: '#ffffff',
        fontFamily: "'Inter', sans-serif", fontSize: '14px',
      }}
    >
      <LoaderCircle size={16} style={{ animation: 'server-wake-spin 1s linear infinite' }} />
      Waking up the server. On free hosting this can take a minute or two; the page will refresh by itself.
      <style>{'@keyframes server-wake-spin { to { transform: rotate(360deg); } }'}</style>
    </div>
  );
};
