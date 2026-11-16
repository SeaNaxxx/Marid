import { getQodexCopy, type SupportedLocale, type UiLocale } from "@qodex/i18n";

export function formatCliHelp(
  version: string,
  locale?: UiLocale,
  detectedLocale?: SupportedLocale,
): string {
  return getQodexCopy(locale, detectedLocale).cli.help(version);
}
