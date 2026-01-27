/**
 * Touch Interaction Tools (click, scroll, etc.)
 */

import { callDevServer, formatResult, resolveDevice } from "../client.js";
import type { InteractParams, OmniOperatorConfig } from "../types.js";

export function createInteractTool(config: OmniOperatorConfig | undefined) {
  return {
    name: "omni_interact",
    description:
      "Interact with Android device screen. " +
      "Actions: 'click' (tap), 'longClick' (long press), 'scroll' (swipe). " +
      "Use coordinates (x, y) for screen position, or nodeId for specific UI elements. " +
      "For scroll, specify direction (up/down/left/right) and distance.",
    parameters: {
      type: "object",
      properties: {
        action: {
          type: "string",
          enum: ["click", "longClick", "scroll"],
          description: "Type of interaction",
        },
        x: {
          type: "number",
          description: "X coordinate (pixels) for coordinate-based actions",
        },
        y: {
          type: "number",
          description: "Y coordinate (pixels) for coordinate-based actions",
        },
        nodeId: {
          type: "string",
          description: "Node ID for element-based actions (from XML capture)",
        },
        direction: {
          type: "string",
          enum: ["up", "down", "left", "right", "forward", "backward"],
          description: "Scroll direction (up/down/left/right for coordinates, forward/backward for nodes)",
        },
        distance: {
          type: "number",
          description: "Scroll distance in pixels (for coordinate-based scroll)",
        },
        device: {
          type: "string",
          description: "Device ID (optional)",
        },
      },
      required: ["action"],
    },
    execute: async (_id: string, args: unknown) => {
      const params = (args && typeof args === "object" ? args : {}) as InteractParams;
      const device = resolveDevice(config, params.device);

      if (!device) {
        return {
          content: [{ type: "text" as const, text: "Error: No device configured" }],
        };
      }

      const timeout = config?.timeout;

      // Determine if using coordinates or node
      const useNode = Boolean(params.nodeId);

      switch (params.action) {
        case "click": {
          if (useNode) {
            const response = await callDevServer({
              device,
              endpoint: "/clickNode",
              query: { nodeId: params.nodeId! },
              timeout,
            });
            return formatResult(response);
          }
          if (params.x === undefined || params.y === undefined) {
            return {
              content: [{ type: "text" as const, text: "Error: x and y coordinates required for click" }],
            };
          }
          const response = await callDevServer({
            device,
            endpoint: "/clickCoordinate",
            query: { x: params.x, y: params.y },
            timeout,
          });
          return formatResult(response);
        }

        case "longClick": {
          if (useNode) {
            const response = await callDevServer({
              device,
              endpoint: "/longClickNode",
              query: { nodeId: params.nodeId! },
              timeout,
            });
            return formatResult(response);
          }
          if (params.x === undefined || params.y === undefined) {
            return {
              content: [{ type: "text" as const, text: "Error: x and y coordinates required for longClick" }],
            };
          }
          const response = await callDevServer({
            device,
            endpoint: "/longClickCoordinate",
            query: { x: params.x, y: params.y },
            timeout,
          });
          return formatResult(response);
        }

        case "scroll": {
          if (useNode) {
            if (!params.direction || !["forward", "backward"].includes(params.direction)) {
              return {
                content: [{ type: "text" as const, text: "Error: direction (forward/backward) required for node scroll" }],
              };
            }
            const response = await callDevServer({
              device,
              endpoint: "/scrollNode",
              query: { nodeId: params.nodeId!, direction: params.direction },
              timeout,
            });
            return formatResult(response);
          }
          if (params.x === undefined || params.y === undefined) {
            return {
              content: [{ type: "text" as const, text: "Error: x and y coordinates required for scroll" }],
            };
          }
          if (!params.direction || !["up", "down", "left", "right"].includes(params.direction)) {
            return {
              content: [{ type: "text" as const, text: "Error: direction (up/down/left/right) required for scroll" }],
            };
          }
          if (params.distance === undefined) {
            return {
              content: [{ type: "text" as const, text: "Error: distance required for scroll" }],
            };
          }
          const response = await callDevServer({
            device,
            endpoint: "/scrollCoordinate",
            query: {
              x: params.x,
              y: params.y,
              direction: params.direction,
              distance: params.distance,
            },
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
