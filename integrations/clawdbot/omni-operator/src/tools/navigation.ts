/**
 * Navigation Tools (Home, Back)
 */

import { callDevServer, formatResult, resolveDevice } from "../client.js";
import type { NavigateParams, OmniOperatorConfig } from "../types.js";

export function createNavigateTool(config: OmniOperatorConfig | undefined) {
  return {
    name: "omni_navigate",
    description:
      "Navigate on Android device. " +
      "Actions: 'home' (go to home screen), 'back' (go back/previous page). " +
      "Use these for basic navigation without needing coordinates.",
    parameters: {
      type: "object",
      properties: {
        action: {
          type: "string",
          enum: ["home", "back"],
          description: "Navigation action",
        },
        device: {
          type: "string",
          description: "Device ID (optional)",
        },
      },
      required: ["action"],
    },
    execute: async (_id: string, args: unknown) => {
      const params = (args && typeof args === "object" ? args : {}) as NavigateParams;
      const device = resolveDevice(config, params.device);

      if (!device) {
        return {
          content: [{ type: "text" as const, text: "Error: No device configured" }],
        };
      }

      const timeout = config?.timeout;

      switch (params.action) {
        case "home": {
          const response = await callDevServer({
            device,
            endpoint: "/goHome",
            timeout,
          });
          return formatResult(response);
        }

        case "back": {
          const response = await callDevServer({
            device,
            endpoint: "/goBack",
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
