import {
  collectVisibleMaridBackgroundTaskControlItems,
  getMaridBackgroundTaskControlItemElapsedMs,
  isActiveMaridBackgroundTaskControlItem,
  parseMaridBackgroundTaskControlItems,
  type MaridBackgroundTaskControlItem,
  type MaridBackgroundTaskControlStatus,
} from "./background-task-controls.js";

export type MaridBackgroundBashJobStatus = MaridBackgroundTaskControlStatus;
export type MaridBackgroundBashJob = MaridBackgroundTaskControlItem & {
  taskKind: "bash";
};

export function parseMaridBackgroundBashJobs(value: unknown): MaridBackgroundBashJob[] {
  return parseMaridBackgroundTaskControlItems(value).filter(isBackgroundBashJob);
}

export function isActiveMaridBackgroundBashJob(job: MaridBackgroundBashJob): boolean {
  return isActiveMaridBackgroundTaskControlItem(job);
}

export function getMaridBackgroundBashJobElapsedMs(
  job: MaridBackgroundBashJob,
  now = Date.now(),
): number {
  return getMaridBackgroundTaskControlItemElapsedMs(job, now);
}

export function collectVisibleMaridBackgroundBashJobs(
  jobs: readonly MaridBackgroundBashJob[],
  now = Date.now(),
  thresholdMs = 30_000,
): Array<MaridBackgroundBashJob & { elapsedMs: number }> {
  return collectVisibleMaridBackgroundTaskControlItems(jobs, now, thresholdMs) as Array<
    MaridBackgroundBashJob & { elapsedMs: number }
  >;
}

function isBackgroundBashJob(job: MaridBackgroundTaskControlItem): job is MaridBackgroundBashJob {
  return job.taskKind === "bash";
}
