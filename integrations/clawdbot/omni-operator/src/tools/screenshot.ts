/**
 * Screenshot and Screen Capture Tools
 */

import { callDevServer, formatImageResult, formatResult, resolveDevice } from "../client.js";
import type {
  MetadataData,
  OmniOperatorConfig,
  ScreenshotData,
  ScreenshotParams,
  TimestampsData,
  XmlData,
} from "../types.js";

export function createScreenshotTool(config: OmniOperatorConfig | undefined) {
  return {
    name: "omni_screenshot",
    description:
      "Capture screen information from Android device. " +
      "Actions: 'image' (screenshot as image), 'xml' (UI tree structure), " +
      "'metadata' (current app info), 'timestamps' (last capture times). " +
      "Use 'image' to see the screen, 'xml' to understand UI structure for clicking.",
    parameters: {
      type: "object",
      properties: {
        action: {
          type: "string",
          enum: ["image", "xml", "metadata", "timestamps"],
          description: "Type of screen capture: image=screenshot, xml=UI tree, metadata=app info",
        },
        device: {
          type: "string",
          description: "Device ID (optional, uses default if not specified)",
        },
        record: {
          type: "boolean",
          description: "Whether to update timestamps (default: true)",
        },
      },
      required: ["action"],
    },
    execute: async (_id: string, args: unknown) => {
      const params = (args && typeof args === "object" ? args : {}) as ScreenshotParams;
      const device = resolveDevice(config, params.device);

      if (!device) {
        return {
          content: [{ type: "text" as const, text: "Error: No device configured" }],
        };
      }

      const timeout = config?.timeout;

      switch (params.action) {
        case "image": {
          const response = await callDevServer<ScreenshotData>({
            device,
            endpoint: "/captureScreenshotImage",
            query: params.record !== undefined ? { record: params.record } : undefined,
            timeout,
          });
          return formatImageResult(response);
        }

        case "xml": {
          const response = await callDevServer<XmlData>({
            device,
            endpoint: "/captureScreenshotXml",
            query: params.record !== undefined ? { record: params.record } : undefined,
            timeout,
          });
          if (!response.success) {
            return formatResult(response);
          }
          const xml = response.data?.xml ?? "No XML data";
          return {
            content: [
              { type: "text" as const, text: "UI Tree XML captured:\n\n" + xml },
            ],
            details: { xml },
          };
        }

        case "metadata": {
          const response = await callDevServer<MetadataData>({
            device,
            endpoint: "/getMetadata",
            timeout,
          });
          if (!response.success) {
            return formatResult(response);
          }
          const data = response.data;
          return {
            content: [
              {
                type: "text" as const,
                text: `Current app: ${data?.packageName ?? "unknown"}\nActivity: ${data?.activityName ?? "unknown"}`,
              },
            ],
            details: data,
          };
        }

        case "timestamps": {
          const response = await callDevServer<TimestampsData>({
            device,
            endpoint: "/timestamps",
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
