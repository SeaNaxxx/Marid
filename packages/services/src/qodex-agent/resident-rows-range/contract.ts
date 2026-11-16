import { qodexProtocolMethods, qodexRuntimeCapabilitiesSchema } from "@qodex/shared";
import type { QodexProtocolClient } from "../qodexProtocolClient.js";

const residentRowsChecks = new WeakMap<object, Promise<boolean>>();
/** 旧 CLI 的 rowsRange 会剥掉 residency 并冷恢复；确认执行端认识它才转发只读热会话请求。 */
export function supportsResidentRowsRange(
  client: Pick<QodexProtocolClient, "request">,
): Promise<boolean> {
  const existing = residentRowsChecks.get(client);
  if (existing) return existing;
  const check = client
    .request(qodexProtocolMethods.runtimeCapabilities, {}, qodexRuntimeCapabilitiesSchema)
    .then((result) => result.residentRowsRange === true)
    .catch(() => {
      residentRowsChecks.delete(client);
      return false;
    });
  residentRowsChecks.set(client, check);
  return check;
}
