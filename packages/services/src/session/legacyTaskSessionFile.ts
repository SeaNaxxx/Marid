import type { QodexSessionFile, QodexTaskMeta } from "@qodex/shared";
import { qodexSessionFileSchema, qodexTaskMetaSchema, qodexTaskModeSchema } from "@qodex/shared";

export type LegacyTaskSessionFile = Omit<QodexSessionFile, "meta"> & {
  meta: Omit<QodexTaskMeta, "mode"> & { mode?: QodexTaskMeta["mode"] };
};

const legacyTaskSessionFileSchema = qodexSessionFileSchema.extend({
  // Claude 原生迁移会按清洗路径删除 meta.mode。
  // legacy snapshot 读取/写入仍要校验其它必需字段，但不能再强制把被过滤字段补回文件。
  meta: qodexTaskMetaSchema.extend({
    mode: qodexTaskModeSchema.optional(),
  }),
});

export function parseLegacyTaskSessionFile(input: unknown): LegacyTaskSessionFile {
  return legacyTaskSessionFileSchema.parse(input);
}

export function safeParseLegacyTaskSessionFile(input: unknown) {
  return legacyTaskSessionFileSchema.safeParse(input);
}
