/** Swift bridge placeholder — real detection not implemented yet. */

export const isSwiftAvailable = (): boolean => {
  if (process.platform !== "darwin") {
    return false;
  }
  return false;
};

export const detectSwiftVersion = async (): Promise<string | null> => {
  if (!isSwiftAvailable()) {
    return null;
  }
  return null;
};
