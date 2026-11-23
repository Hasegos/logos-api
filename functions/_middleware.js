/**
 * 스냅샷 키를 가진 요청만 스냅샷을 내려 주고, 나머지는 같은 경로의 운영 사이트로 보낸다.
 *
 * @param context Pages Functions 실행 정보 (request, env, next)
 * @returns 스냅샷 응답 또는 운영 사이트로 가는 302 응답
 */
export async function onRequest({ request, env, next }) {
  if (request.headers.get("X-Snapshot-Key") === env.SNAPSHOT_KEY) return next();
  const { pathname } = new URL(request.url);
  return Response.redirect("https://logos-api.com" + pathname, 302);
}
