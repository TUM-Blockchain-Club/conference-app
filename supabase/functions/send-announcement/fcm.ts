// FCM HTTP v1 for send-announcement: OAuth, message shape, error triage and
// fan-out. Deliberately free of Deno and Supabase APIs — only fetch and Web
// Crypto — so `node --test` can exercise it without either (see fcm.test.ts
// and `make test-functions`).

export type Category = "general" | "schedule" | "keynote" | "emergency";

export interface Announcement {
  id: string;
  title: string;
  body: string;
  category: Category;
}

export interface ServiceAccount {
  client_email: string;
  private_key: string;
  token_uri?: string;
}

export interface DeviceToken {
  token: string;
  token_hash: string;
}

type Fetch = typeof fetch;

/** Production FCM. Overridable only so a local fake can stand in — see index.ts. */
export const DEFAULT_FCM_ENDPOINT = "https://fcm.googleapis.com";

const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token";

/** Also hard-coded in AsmaraApplication.kt and AndroidManifest.xml — keep in step. */
export const ANDROID_CHANNEL_ID = "announcements";

function base64url(bytes: Uint8Array | string): string {
  const data = typeof bytes === "string" ? new TextEncoder().encode(bytes) : bytes;
  let binary = "";
  for (const b of data) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToDer(pem: string): Uint8Array {
  const b64 = pem
    .replace(/-----BEGIN [^-]+-----/, "")
    .replace(/-----END [^-]+-----/, "")
    .replace(/\s+/g, "");
  const binary = atob(b64);
  const out = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) out[i] = binary.charCodeAt(i);
  return out;
}

/**
 * Parses FCM_SERVICE_ACCOUNT_JSON. Tolerates the one way it reliably arrives
 * damaged: an env-file loader that expands the `\n` escapes inside
 * `private_key` into real newlines, which JSON forbids inside a string.
 */
export function parseServiceAccount(raw: string): ServiceAccount {
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    parsed = JSON.parse(raw.trim().replace(/\r?\n/g, "\\n"));
  }
  const account = parsed as Partial<ServiceAccount>;
  if (typeof account?.client_email !== "string" || typeof account?.private_key !== "string") {
    throw new Error("service account JSON needs client_email and private_key");
  }
  return account as ServiceAccount;
}

/** A signed RS256 JWT asserting the service account, for the OAuth exchange. */
export async function signServiceAccountJwt(account: ServiceAccount, nowSeconds: number): Promise<string> {
  const header = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = base64url(
    JSON.stringify({
      iss: account.client_email,
      scope: FCM_SCOPE,
      aud: account.token_uri ?? DEFAULT_TOKEN_URI,
      iat: nowSeconds,
      exp: nowSeconds + 3600,
    }),
  );
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToDer(account.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(`${header}.${claims}`),
  );
  return `${header}.${claims}.${base64url(new Uint8Array(signature))}`;
}

/** Exchanges the service account for a one-hour OAuth access token. */
export async function getAccessToken(
  account: ServiceAccount,
  fetchFn: Fetch = fetch,
  nowSeconds = Math.floor(Date.now() / 1000),
): Promise<string> {
  const assertion = await signServiceAccountJwt(account, nowSeconds);
  const response = await fetchFn(account.token_uri ?? DEFAULT_TOKEN_URI, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!response.ok) {
    throw new Error(`OAuth token exchange failed with ${response.status}: ${await response.text()}`);
  }
  const { access_token: accessToken } = await response.json();
  if (typeof accessToken !== "string") throw new Error("OAuth response had no access_token");
  return accessToken;
}

/**
 * One FCM v1 message. A notification message (so the OS draws it with the
 * app in the background) plus a data payload carrying the announcement id,
 * which the app uses only to know it should refresh — the feed is always read
 * from the server, never from the push.
 */
export function buildMessage(announcement: Announcement, token: string) {
  return {
    message: {
      token,
      notification: { title: announcement.title, body: announcement.body },
      data: { announcement_id: announcement.id, category: announcement.category },
      android: {
        // User-visible notifications must be high priority to be delivered
        // promptly under Doze.
        priority: "HIGH",
        notification: { channel_id: ANDROID_CHANNEL_ID },
      },
      apns: {
        headers: { "apns-priority": "10" },
        payload: { aps: { sound: "default" } },
      },
    },
  };
}

/**
 * - `dead`: the token will never work again; delete its row.
 * - `invalid`: INVALID_ARGUMENT. Usually a bad token, but also what FCM says
 *   about a malformed *message* — the caller only prunes these when the same
 *   batch also had successes, so a payload bug cannot wipe every token.
 * - `error`: anything else (quota, 5xx, auth). Leave the row alone.
 */
export type SendFailure = "dead" | "invalid" | "error";

export function classifyFailure(status: number, body: unknown): SendFailure {
  const error = (body as { error?: { status?: string; details?: { errorCode?: string }[] } })?.error;
  const codes = new Set<string>([error?.status ?? "", ...(error?.details ?? []).map((d) => d.errorCode ?? "")]);
  if (codes.has("UNREGISTERED") || status === 404) return "dead";
  if (codes.has("INVALID_ARGUMENT")) return "invalid";
  return "error";
}

export interface FanOutResult {
  sent: number;
  failed: number;
  pruned: number;
}

export interface FanOutDeps {
  /** Next page of tokens strictly after `afterHash` in hash order; fewer than `pageSize` means last page. */
  nextPage(afterHash: string | null, pageSize: number): Promise<DeviceToken[]>;
  /** Sends one message; resolves to null on success or the failure class. */
  send(token: DeviceToken): Promise<SendFailure | null>;
  prune(tokenHashes: string[]): Promise<void>;
}

/**
 * Pages through the target devices 500 at a time — keyset pagination on the
 * hash, so pruning a page cannot shift the next one — and sends each page with
 * bounded concurrency.
 */
export async function fanOut(
  deps: FanOutDeps,
  { pageSize = 500, concurrency = 50 }: { pageSize?: number; concurrency?: number } = {},
): Promise<FanOutResult> {
  const result: FanOutResult = { sent: 0, failed: 0, pruned: 0 };
  let after: string | null = null;

  for (;;) {
    const page = await deps.nextPage(after, pageSize);
    if (page.length === 0) break;

    const outcomes = await mapWithConcurrency(page, concurrency, deps.send);
    const successes = outcomes.filter((o) => o === null).length;
    result.sent += successes;
    result.failed += outcomes.length - successes;

    const toPrune = page
      .filter((_, i) => outcomes[i] === "dead" || (outcomes[i] === "invalid" && successes > 0))
      .map((t) => t.token_hash);
    if (toPrune.length > 0) {
      await deps.prune(toPrune);
      result.pruned += toPrune.length;
    }

    after = page[page.length - 1].token_hash;
    if (page.length < pageSize) break;
  }
  return result;
}

async function mapWithConcurrency<T, R>(items: T[], limit: number, fn: (item: T) => Promise<R>): Promise<R[]> {
  const results = new Array<R>(items.length);
  let next = 0;
  const workers = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (next < items.length) {
      const index = next++;
      results[index] = await fn(items[index]);
    }
  });
  await Promise.all(workers);
  return results;
}

/** POSTs one message to FCM v1 and triages the answer. */
export async function sendFcm(
  projectId: string,
  accessToken: string,
  announcement: Announcement,
  token: string,
  fetchFn: Fetch = fetch,
  endpoint: string = DEFAULT_FCM_ENDPOINT,
): Promise<SendFailure | null> {
  let response: Response;
  try {
    response = await fetchFn(`${endpoint}/v1/projects/${projectId}/messages:send`, {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${accessToken}` },
      body: JSON.stringify(buildMessage(announcement, token)),
    });
  } catch {
    return "error";
  }
  if (response.ok) return null;
  let body: unknown = null;
  try {
    body = await response.json();
  } catch {
    // Non-JSON error page; status alone decides.
  }
  return classifyFailure(response.status, body);
}
