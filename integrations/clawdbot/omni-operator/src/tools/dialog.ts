/**
 * User Dialog/Interaction Tools
 */

import { callDevServer, formatResult, resolveDevice } from "../client.js";
import type { DialogParams, OmniOperatorConfig } from "../types.js";

export function createDialogTool(config: OmniOperatorConfig | undefined) {
  return {
    name: "omni_dialog",
    description:
      "Show dialogs and get user input on Android device. " +
      "Actions: 'showMessage' (display notification), 'confirm' (yes/no dialog), " +
      "'choice' (multiple choice dialog). " +
      "Use these to interact with the user through the device UI.",
    parameters: {
      type: "object",
      properties: {
        action: {
          type: "string",
          enum: ["showMessage", "confirm", "choice"],
          description: "Type of dialog",
        },
        title: {
          type: "string",
          description: "Dialog title (for showMessage)",
        },
        content: {
          type: "string",
          description: "Dialog content/body (for showMessage)",
        },
        prompt: {
          type: "string",
          description: "Prompt text (for confirm and choice)",
        },
        options: {
          type: "string",
          description: "Semicolon-separated options for choice dialog (e.g., 'Option A;Option B;Option C')",
        },
        device: {
          type: "string",
          description: "Device ID (optional)",
        },
      },
      required: ["action"],
    },
    execute: async (_id: string, args: unknown) => {
      const params = (args && typeof args === "object" ? args : {}) as DialogParams;
      const device = resolveDevice(config, params.device);

      if (!device) {
        return {
          content: [{ type: "text" as const, text: "Error: No device configured" }],
        };
      }

      const timeout = config?.timeout;

      switch (params.action) {
        case "showMessage": {
          if (!params.title || !params.content) {
            return {
              content: [{ type: "text" as const, text: "Error: title and content required for showMessage" }],
            };
          }
          const response = await callDevServer({
            device,
            endpoint: "/showMessage",
            query: { title: params.title, content: params.content },
            timeout,
          });
          return formatResult(response);
        }

        case "confirm": {
          if (!params.prompt) {
            return {
              content: [{ type: "text" as const, text: "Error: prompt required for confirm" }],
            };
          }
          const response = await callDevServer<string>({
            device,
            endpoint: "/requireUserConfirmation",
            query: { prompt: params.prompt },
            timeout: timeout ? Math.max(timeout, 60000) : 60000, // Give user time to respond
          });
          if (!response.success) {
            return formatResult(response);
          }
          const result = response.data;
          return {
            content: [
              {
                type: "text" as const,
                text: `User response: ${result}`,
              },
            ],
            details: { userChoice: result },
          };
        }

        case "choice": {
          if (!params.prompt) {
            return {
              content: [{ type: "text" as const, text: "Error: prompt required for choice" }],
            };
          }
          if (!params.options) {
            return {
              content: [{ type: "text" as const, text: "Error: options required for choice (semicolon-separated)" }],
            };
          }
          const response = await callDevServer<string>({
            device,
            endpoint: "/requireUserChoice",
            query: { prompt: params.prompt, options: params.options },
            timeout: timeout ? Math.max(timeout, 60000) : 60000,
          });
          if (!response.success) {
            return formatResult(response);
          }
          const result = response.data;
          return {
            content: [
              {
                type: "text" as const,
                text: `User selected: ${result}`,
              },
            ],
            details: { userChoice: result },
          };
        }

        default:
          return {
            content: [{ type: "text" as const, text: `Unknown action: ${params.action}` }],
          };
      }
    },
  };
}
