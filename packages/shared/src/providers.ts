import { z } from "zod";

/**
 * Marid agent 提供方的单一真源。
 *
 * 类型 MaridProvider、运行时 schema maridProviderSchema 都从这里派生,
 * 避免各处内联 z.enum([...]) 副本随新增/删除 provider 漂移。
 * 本模块只依赖 zod(叶子),可被 validation / marid-protocol 等无环引用。
 */
export const MARID_PROVIDERS = ["glm"] as const;

export const maridProviderSchema = z.enum(MARID_PROVIDERS);

export type MaridProvider = (typeof MARID_PROVIDERS)[number];
