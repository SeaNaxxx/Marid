import { z } from "zod";

/**
 * Qodex agent 提供方的单一真源。
 *
 * 类型 QodexProvider、运行时 schema qodexProviderSchema 都从这里派生,
 * 避免各处内联 z.enum([...]) 副本随新增/删除 provider 漂移。
 * 本模块只依赖 zod(叶子),可被 validation / qodex-protocol 等无环引用。
 */
export const QODEX_PROVIDERS = ["glm"] as const;

export const qodexProviderSchema = z.enum(QODEX_PROVIDERS);

export type QodexProvider = (typeof QODEX_PROVIDERS)[number];
