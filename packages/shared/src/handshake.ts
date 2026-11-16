export interface HelloMessage {
  type: "marid-hello";
  version: string;
  platform: string;
  arch: string;
  pid: number;
}

export interface HelloAckMessage {
  type: "marid-hello-ack";
  version: string;
  clientId: string;
}
