// send-announcement: fans a newly published announcement out to devices via
// FCM. Called by the announcements_send_push trigger through pg_net (see
// supabase/migrations/20260916000300_announcement_push.sql).
//
// Credential-optional. FCM_PROJECT_ID and FCM_SERVICE_ACCOUNT_JSON come from
// `supabase secrets set`, never from the repo. Without them this is a logged
// no-op returning 200, which is what lets `supabase functions serve` run with
// no keys at all.
//
// Safe to expose with verify_jwt = false (config.toml): the request carries
// only an announcement id, and the function acts on server-side state alone.
// It atomically *claims* the row (pushed_at is null -> now()) before sending,
// so a repeated or forged call cannot send anything twice, and it never sends
// a draft.

import { createClient } from "npm:@supabase/supabase-js@2";
import {
  type Announcement,
  DEFAULT_FCM_ENDPOINT,
  type DeviceToken,
  fanOut,
  getAccessToken,
  parseServiceAccount,
  sendFcm,
} from "./fcm.ts";

/** Announcements published longer ago than this are claimed but not sent. */
const STALE_AFTER_MS = 60 * 60 * 1000;

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json" } });
}

Deno.serve(async (req) => {
  // First, before anything else: no credentials means push is off.
  const projectId = Deno.env.get("FCM_PROJECT_ID");
  const serviceAccountJson = Deno.env.get("FCM_SERVICE_ACCOUNT_JSON");
  if (!projectId || !serviceAccountJson) {
    console.log("push disabled: FCM credentials absent");
    return json({ sent: 0, disabled: true });
  }

  if (req.method !== "POST") return json({ error: "method not allowed" }, 405);
  let announcementId: unknown;
  try {
    ({ announcement_id: announcementId } = await req.json());
  } catch {
    return json({ error: "body must be JSON" }, 400);
  }
  if (typeof announcementId !== "string" || !UUID.test(announcementId)) {
    return json({ error: "announcement_id must be a uuid" }, 400);
  }

  // Injected by the Edge Runtime; never set by hand.
  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, {
    auth: { persistSession: false },
  });

  // The claim. Only one caller can move pushed_at off null, so at most one
  // send per publication; clearing pushed_at by hand re-arms it.
  const { data: claimed, error: claimError } = await supabase
    .from("announcements")
    .update({ pushed_at: new Date().toISOString() })
    .eq("id", announcementId)
    .eq("is_published", true)
    .is("pushed_at", null)
    .select("id, title, body, category, published_at")
    .maybeSingle();
  if (claimError) return json({ error: claimError.message }, 500);
  if (!claimed) return json({ sent: 0, skipped: "not published, or already pushed" });

  // Switching push on mid-conference would otherwise push the whole backlog.
  if (Date.now() - new Date(claimed.published_at).getTime() > STALE_AFTER_MS) {
    console.log(`not pushing stale announcement ${claimed.id}`);
    return json({ sent: 0, skipped: "stale" });
  }

  let accessToken: string;
  try {
    accessToken = await getAccessToken(parseServiceAccount(serviceAccountJson));
  } catch (err) {
    // Nothing was sent: release the claim so a retry (or re-publish) can.
    await supabase.from("announcements").update({ pushed_at: null }).eq("id", claimed.id);
    console.error(`FCM auth failed: ${err}`);
    return json({ error: "FCM auth failed" }, 502);
  }

  const announcement = claimed as Announcement;
  // Unset in production. Lets `supabase functions serve` be pointed at a local
  // fake FCM to exercise the fan-out queries without Google in the loop.
  const fcmEndpoint = Deno.env.get("FCM_ENDPOINT") ?? DEFAULT_FCM_ENDPOINT;
  const result = await fanOut({
    async nextPage(afterHash, pageSize) {
      let query = supabase.from("device_tokens").select("token, token_hash").order("token_hash").limit(pageSize);
      if (afterHash) query = query.gt("token_hash", afterHash);
      // Emergency ignores preferences: it goes to every registered device.
      if (announcement.category !== "emergency") query = query.contains("categories", [announcement.category]);
      const { data, error } = await query;
      if (error) throw new Error(error.message);
      return (data ?? []) as DeviceToken[];
    },
    send: (device) => sendFcm(projectId, accessToken, announcement, device.token, fetch, fcmEndpoint),
    async prune(hashes) {
      const { error } = await supabase.from("device_tokens").delete().in("token_hash", hashes);
      if (error) console.error(`pruning failed: ${error.message}`);
    },
  });

  console.log(`announcement ${announcement.id}: ${JSON.stringify(result)}`);
  return json(result);
});
