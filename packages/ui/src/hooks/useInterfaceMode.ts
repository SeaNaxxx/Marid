import { useMaridStoreWithDefault } from "@/store/StoreProvider.js";

export function useIsOfficeMode(): boolean {
  return useMaridStoreWithDefault((state) => state.interfaceMode === "office", false);
}
