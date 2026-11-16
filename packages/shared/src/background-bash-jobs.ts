import {
  collectVisibleQodexBackgroundTaskControlItems,
  getQodexBackgroundTaskControlItemElapsedMs,
  isActiveQodexBackgroundTaskControlItem,
  parseQodexBackgroundTaskControlItems,
  type QodexBackgroundTaskControlItem,
  type QodexBackgroundTaskControlStatus,
} from "./background-task-controls.js";

export type QodexBackgroundBashJobStatus = QodexBackgroundTaskControlStatus;
export type QodexBackgroundBashJob = QodexBackgroundTaskControlItem & {
  taskKind: "bash";
};

export function parseQodexBackgroundBashJobs(value: unknown): QodexBackgroundBashJob[] {
  return parseQodexBackgroundTaskControlItems(value).filter(isBackgroundBashJob);
}

export function isActiveQodexBackgroundBashJob(job: QodexBackgroundBashJob): boolean {
  return isActiveQodexBackgroundTaskControlItem(job);
}

export function getQodexBackgroundBashJobElapsedMs(
  job: QodexBackgroundBashJob,
  now = Date.now(),
): number {
  return getQodexBackgroundTaskControlItemElapsedMs(job, now);
}

export function collectVisibleQodexBackgroundBashJobs(
  jobs: readonly QodexBackgroundBashJob[],
  now = Date.now(),
  thresholdMs = 30_000,
): Array<QodexBackgroundBashJob & { elapsedMs: number }> {
  return collectVisibleQodexBackgroundTaskControlItems(jobs, now, thresholdMs) as Array<
    QodexBackgroundBashJob & { elapsedMs: number }
  >;
}

function isBackgroundBashJob(job: QodexBackgroundTaskControlItem): job is QodexBackgroundBashJob {
  return job.taskKind === "bash";
}
