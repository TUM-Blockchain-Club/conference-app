// Run with `make test-functions` (node --test). Uses only Node built-ins: the
// module under test needs nothing but fetch and Web Crypto.
import { strict as assert } from "node:assert";
import { test } from "node:test";
import {
  type DeviceToken,
  buildMessage,
  classifyFailure,
  fanOut,
  getAccessToken,
  parseServiceAccount,
  sendFcm,
  signServiceAccountJwt,
} from "./fcm.ts";

async function testServiceAccount() {
  const keys = await crypto.subtle.generateKey(
    { name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" },
    true,
    ["sign", "verify"],
  );
  const der = new Uint8Array(await crypto.subtle.exportKey("pkcs8", keys.privateKey));
  const pem = `-----BEGIN PRIVATE KEY-----\n${Buffer.from(der).toString("base64")}\n-----END PRIVATE KEY-----\n`;
  return { account: { client_email: "push@test.iam.gserviceaccount.com", private_key: pem }, publicKey: keys.publicKey };
}

const fromBase64url = (s: string) => Buffer.from(s.replace(/-/g, "+").replace(/_/g, "/"), "base64");

test("the service-account JWT carries the FCM scope and verifies against the public key", async () => {
  const { account, publicKey } = await testServiceAccount();
  const jwt = await signServiceAccountJwt(account, 1_000);
  const [header, claims, signature] = jwt.split(".");

  assert.deepEqual(JSON.parse(fromBase64url(header).toString()), { alg: "RS256", typ: "JWT" });
  const body = JSON.parse(fromBase64url(claims).toString());
  assert.equal(body.iss, account.client_email);
  assert.equal(body.scope, "https://www.googleapis.com/auth/firebase.messaging");
  assert.equal(body.aud, "https://oauth2.googleapis.com/token");
  assert.equal(body.exp - body.iat, 3600);

  const valid = await crypto.subtle.verify(
    "RSASSA-PKCS1-v1_5",
    publicKey,
    fromBase64url(signature),
    new TextEncoder().encode(`${header}.${claims}`),
  );
  assert.ok(valid);
});

test("getAccessToken posts a jwt-bearer grant and returns the access token", async () => {
  const { account } = await testServiceAccount();
  let seen: URLSearchParams | undefined;
  const fakeFetch = (async (_url: string, init: RequestInit) => {
    seen = init.body as URLSearchParams;
    return new Response(JSON.stringify({ access_token: "ya29.test" }), { status: 200 });
  }) as typeof fetch;

  assert.equal(await getAccessToken(account, fakeFetch, 1_000), "ya29.test");
  assert.equal(seen?.get("grant_type"), "urn:ietf:params:oauth:grant-type:jwt-bearer");
  assert.equal(seen?.get("assertion")?.split(".").length, 3);
});

test("getAccessToken surfaces a rejected exchange", async () => {
  const { account } = await testServiceAccount();
  const fakeFetch = (async () => new Response("bad key", { status: 401 })) as typeof fetch;
  await assert.rejects(getAccessToken(account, fakeFetch), /401/);
});

test("the message is a notification with the id in data and the Android channel set", () => {
  const message = buildMessage({ id: "a1", title: "T", body: "B", category: "emergency" }, "tok").message;
  assert.equal(message.token, "tok");
  assert.deepEqual(message.notification, { title: "T", body: "B" });
  assert.deepEqual(message.data, { announcement_id: "a1", category: "emergency" });
  assert.equal(message.android.notification.channel_id, "announcements");
  assert.equal(message.android.priority, "HIGH");
});

test("FCM errors are triaged into dead, invalid and transient", () => {
  const unregistered = {
    error: { status: "NOT_FOUND", details: [{ "@type": "type.googleapis.com/google.firebase.fcm.v1.FcmError", errorCode: "UNREGISTERED" }] },
  };
  const invalid = { error: { status: "INVALID_ARGUMENT", details: [{ errorCode: "INVALID_ARGUMENT" }] } };
  assert.equal(classifyFailure(404, unregistered), "dead");
  assert.equal(classifyFailure(400, invalid), "invalid");
  assert.equal(classifyFailure(429, { error: { status: "RESOURCE_EXHAUSTED" } }), "error");
  assert.equal(classifyFailure(503, null), "error");
});

test("sendFcm reports a network failure as transient rather than throwing", async () => {
  const failing = (async () => {
    throw new TypeError("fetch failed");
  }) as typeof fetch;
  assert.equal(await sendFcm("p", "t", { id: "a", title: "", body: "", category: "general" }, "tok", failing), "error");
});

function tokens(n: number, prefix = "h"): DeviceToken[] {
  return Array.from({ length: n }, (_, i) => ({ token: `t${i}`, token_hash: `${prefix}${String(i).padStart(5, "0")}` }));
}

function pager(all: DeviceToken[]) {
  const pages: (string | null)[] = [];
  return {
    pages,
    async nextPage(after: string | null, size: number) {
      pages.push(after);
      return all.filter((t) => after === null || t.token_hash > after).slice(0, size);
    },
  };
}

test("fan-out pages 500 at a time by hash and sends to every token once", async () => {
  const all = tokens(1_234);
  const { pages, nextPage } = pager(all);
  const sentTo: string[] = [];

  const result = await fanOut({
    nextPage,
    send: async (t) => {
      sentTo.push(t.token);
      return null;
    },
    prune: async () => assert.fail("nothing to prune"),
  });

  assert.deepEqual(result, { sent: 1_234, failed: 0, pruned: 0 });
  assert.equal(new Set(sentTo).size, 1_234);
  assert.equal(pages.length, 3);
  assert.equal(pages[0], null);
  assert.equal(pages[1], all[499].token_hash);
});

test("fan-out prunes unregistered tokens, and invalid ones only when the batch had a success", async () => {
  const all = tokens(4);
  const pruned: string[] = [];
  const outcomes = new Map([
    ["t0", null],
    ["t1", "dead"],
    ["t2", "invalid"],
    ["t3", "error"],
  ] as const);

  const result = await fanOut({
    nextPage: pager(all).nextPage,
    send: async (t) => outcomes.get(t.token as "t0") ?? null,
    prune: async (hashes) => {
      pruned.push(...hashes);
    },
  });

  assert.deepEqual(result, { sent: 1, failed: 3, pruned: 2 });
  assert.deepEqual(pruned, [all[1].token_hash, all[2].token_hash]);
});

test("a batch where every send is INVALID_ARGUMENT is treated as a payload bug and prunes nothing", async () => {
  const result = await fanOut({
    nextPage: pager(tokens(3)).nextPage,
    send: async () => "invalid",
    prune: async () => assert.fail("must not prune on an all-invalid batch"),
  });
  assert.deepEqual(result, { sent: 0, failed: 3, pruned: 0 });
});

test("no target devices is a clean no-op", async () => {
  const result = await fanOut({
    nextPage: async () => [],
    send: async () => assert.fail("nobody to send to"),
    prune: async () => assert.fail("nothing to prune"),
  });
  assert.deepEqual(result, { sent: 0, failed: 0, pruned: 0 });
});

test("a service account whose \\n escapes were expanded by an env loader still parses", () => {
  const original = { client_email: "a@b.iam.gserviceaccount.com", private_key: "-----BEGIN PRIVATE KEY-----\nAAA\n-----END PRIVATE KEY-----\n" };
  const intact = JSON.stringify(original);
  const expanded = intact.replace(/\\n/g, "\n");
  assert.notEqual(intact, expanded);

  assert.deepEqual(parseServiceAccount(intact), original);
  assert.deepEqual(parseServiceAccount(expanded), original);
  assert.throws(() => parseServiceAccount("{}"), /client_email/);
});
