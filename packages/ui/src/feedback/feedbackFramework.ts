import {
  DEFAULT_FEEDBACK_TICKET_FRAMEWORK,
  type MaridProvider,
  type FeedbackTicketFramework,
} from "@marid/shared";

export function inferFeedbackFrameworkFromAgentProvider(
  _provider: MaridProvider | null | undefined,
): FeedbackTicketFramework {
  return DEFAULT_FEEDBACK_TICKET_FRAMEWORK;
}
