export const CLI_COMMAND_NAME = "marid";
export const CLI_PROCESS_NAME = "marid-cli";

interface ProcessTitleTarget {
  title: string;
}

export const setCliProcessTitle = (
  target: ProcessTitleTarget = process,
): void => {
  target.title = CLI_PROCESS_NAME;
};
