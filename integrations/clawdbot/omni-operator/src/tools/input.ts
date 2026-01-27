/**
 * Text Input Tools
 */

import { callDevServer, formatResult, resolveDevice } from "../client.js";
import type { InputParams, OmniOperatorConfig } from "../types.js";

export function createInputTool(config: OmniOperatorConfig | undefined) {
  return {
    name: "omni_input",
    description:
      "Input text on Android device. " +
      "Actions: 'inputText' (type into specific node), 'inputToFocused' (type into focused element), " +
      "'copyToClipboard' (copy text), 'injectIME' (inject via IME). " +
      "For inputText, provide nodeId from XML capture.",
    parameters: {
      type: "object",
      properties: {
        action: {
          type: "string",
          enum: ["inputText", "inputToFocused", "copyToClipboard", "injectIME"],
          description: "Type of input action",
        },
        text: {
          type: "string",
          description: "Text to input or copy",
        },
        nodeId: {
          type: "string",
          description: "Node ID of the input field (required for inputText)",
        },
        device: {
          type: "string",
          description: "Device ID (optional)",
        },
      },
      required: ["action", "text"],
    },
    execute: async (_id: string, args: unknown) => {
      const params = (args && typeof args === "object" ? args : {}) as InputParams;
      const device = resolveDevice(config, params.device);

      if (!device) {
        return {
          content: [{ type: "text" as const, text: "Error: No device configured" }],
        };
      }

      if (!params.text) {
        return {
          content: [{ type: "text" as const, text: "Error: text is required" }],
        };
      }

      const timeout = config?.timeout;

      switch (params.action) {
        case "inputText": {
          if (!params.nodeId) {
            return {
              content: [{ type: "text" as const, text: "Error: nodeId required for inputText" }],
            };
          }
          const response = await callDevServer({
            device,
            endpoint: "/inputText",
            query: { nodeId: params.nodeId, text: params.text },
            timeout,
          });
          return formatResult(response);
        }

        case "inputToFocused": {
          const response = await callDevServer({
            device,
            endpoint: "/inputTextToFocusedNode",
            query: { text: params.text },
            timeout,
          });
          return formatResult(response);
        }

        case "copyToClipboard": {
          const response = await callDevServer({
            device,
            endpoint: "/copyToClipboard",
            query: { text: params.text },
            timeout,
          });
          return formatResult(response);
        }

        case "injectIME": {
          const response = await callDevServer({
            device,
            endpoint: "/injectTextByIME",
            query: { text: params.text },
            timeout,
          });
          return formatResult(response);
        }

        default:
          return {
            content: [{ type: "text" as const, text: `Unknown action: ${params.action}` }],
          };
      }
    },
  };
}
