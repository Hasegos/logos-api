export async function onRequest({ request, env, next }) {
  if (request.headers.get("X-Snapshot-Key") === env.SNAPSHOT_KEY) return next();
  const { pathname } = new URL(request.url);
  return Response.redirect("https://logos-api.com" + pathname, 302);
}
