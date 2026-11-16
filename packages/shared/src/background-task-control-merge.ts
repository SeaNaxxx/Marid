import type { QodexBackgroundTaskControlItem } from "./background-task-controls.js";

export function mergeQodexBackgroundTaskControlItems(
  current: readonly QodexBackgroundTaskControlItem[],
  updates: readonly QodexBackgroundTaskControlItem[],
): QodexBackgroundTaskControlItem[] {
  const jobsById = new Map(current.map((job) => [job.jobId, job] as const));
  for (const job of updates) {
    jobsById.set(job.jobId, job);
  }
  return Array.from(jobsById.values());
}
