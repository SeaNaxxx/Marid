/**
 * Generates golden test vectors for the Kotlin RPC implementation by running
 * the REAL packages/rpc TypeScript serializer/framer under Bun.
 *
 * Output: android/app/src/test/resources/rpc-golden-vectors.json
 *
 * The Kotlin unit test (RpcGoldenVectorTest) asserts byte-identical output.
 */
import { serialize } from "/home/z/qodex-repo/packages/rpc/src/serialization.ts";
import {
  writeProtocolMessage,
  ProtocolMessageType,
} from "/home/z/qodex-repo/packages/rpc/src/protocol.ts";
import { BufferWriter } from "/home/z/qodex-repo/packages/rpc/src/serialization.ts";
import { writeFileSync, mkdirSync } from "node:fs";

function hex(bytes) {
  return Buffer.from(bytes).toString("hex");
}

function serializeCase(name, header, body) {
  const writer = new BufferWriter();
  serialize(writer, header);
  serialize(writer, body);
  return { name, header: JSON.stringify(header), body: body === undefined ? "undefined" : JSON.stringify(body), hex: hex(writer.buffer.buffer) };
}

function frameCase(name, payloadHex) {
  const payload = Buffer.from(payloadHex, "hex");
  return { name, hex: hex(writeProtocolMessage({ type: ProtocolMessageType.Regular, id: 0, ack: 0, data: payload }).buffer) };
}

const vectors = {
  cases: [
    // Request: Promise initialize
    serializeCase("request-initialize", [100, 0, "qodex-agent", "initialize"], { workspacePath: "/data/data/com.qodex.app/files/workspace" }),
    // Request: sendConversationCommandV4 with v4 envelope
    serializeCase("request-send-text", [100, 1, "qodex-agent", "sendConversationCommandV4"], {
      workspacePath: "/data/data/com.qodex.app/files/workspace",
      envelope: {
        commandId: "0194a2b3-c4d5-7e89-f012-3456789abcde",
        clientId: "qodex-android",
        sessionId: null,
        type: "sendText",
        payload: { text: "هلا" },
        issuedAt: 1763240000000,
        inputOrigin: "mobile",
      },
    }),
    // Event listen
    serializeCase("request-event-listen", [102, 2, "qodex-agent", "onDynamicSessionEvent"], {
      workspacePath: "/data/data/com.qodex.app/files/workspace",
      deliveryKind: "live",
    }),
    // Event dispose
    serializeCase("request-event-dispose", [103, 2], undefined),
    // Server Initialize: [200] + undefined
    serializeCase("response-initialize", [200], undefined),
    // PromiseSuccess with string
    serializeCase("response-success-string", [201, 42], "ok"),
    // PromiseSuccess with nested object (model selection view)
    serializeCase("response-success-view", [201, 43], {
      revision: 7,
      providers: [
        { id: "z-ai", models: [{ id: "glm-5.3" }, { id: "glm-4.7" }] },
        { id: "mock-provider", models: [{ id: "mock-model" }] },
      ],
      preferredSelection: { providerId: "z-ai", modelId: "glm-5.3" },
    }),
    // PromiseError payload
    serializeCase("response-error", [202, 44], { message: "Provider Registry 中不存在 Model", name: "Error", stack: ["at f ()"] }),
    // EventFire with Arabic + numbers
    serializeCase("response-event-fire", [204, 7], {
      kind: "session.event",
      seq: 12,
      parts: [{ type: "text", value: "هلا" }],
      ratio: 0.75,
    }),
    // Int edge values
    serializeCase("value-int-zero", [0], 0),
    serializeCase("value-int-maxish", [6], 268435455),
    serializeCase("value-string-unicode", [1], "مرحبا Qodex"),
    serializeCase("value-array-mixed", [4], [1, "two", true, null, { three: 3 }]),
  ],
  frames: [],
};

// Frame cases: wrap a couple of payloads in Regular frames.
for (const c of vectors.cases.slice(0, 3)) {
  vectors.frames.push(frameCase(`frame-of-${c.name}`, c.hex));
}

mkdirSync("/home/z/qodex-repo/android/app/src/test/resources", { recursive: true });
writeFileSync(
  "/home/z/qodex-repo/android/app/src/test/resources/rpc-golden-vectors.json",
  JSON.stringify(vectors, null, 2),
);
console.log(`wrote ${vectors.cases.length} cases + ${vectors.frames.length} frames`);
