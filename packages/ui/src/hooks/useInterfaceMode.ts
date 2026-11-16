import { useQodexStoreWithDefault } from "@/store/StoreProvider.js";

export function useIsOfficeMode(): boolean {
  return useQodexStoreWithDefault((state) => state.interfaceMode === "office", false);
}
