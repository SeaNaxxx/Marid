export interface HelloMessage {
  type: "qodex-hello";
  version: string;
  platform: string;
  arch: string;
  pid: number;
}

export interface HelloAckMessage {
  type: "qodex-hello-ack";
  version: string;
  clientId: string;
}
