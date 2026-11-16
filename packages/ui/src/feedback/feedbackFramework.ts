import {
  DEFAULT_FEEDBACK_TICKET_FRAMEWORK,
  type QodexProvider,
  type FeedbackTicketFramework,
} from "@qodex/shared";

export function inferFeedbackFrameworkFromAgentProvider(
  _provider: QodexProvider | null | undefined,
): FeedbackTicketFramework {
  return DEFAULT_FEEDBACK_TICKET_FRAMEWORK;
}
