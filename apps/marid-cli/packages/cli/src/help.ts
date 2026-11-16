import { getMaridCopy, type SupportedLocale, type UiLocale } from "@marid/i18n";

export function formatCliHelp(
  version: string,
  locale?: UiLocale,
  detectedLocale?: SupportedLocale,
): string {
  return getMaridCopy(locale, detectedLocale).cli.help(version);
}
