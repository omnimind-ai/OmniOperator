/**
 * Application Management Tools
 */

import { callDevServer, formatResult, resolveDevice } from "../client.js";
import type { AppParams, InstalledAppsData, OmniOperatorConfig } from "../types.js";

export function createAppTool(config: OmniOperatorConfig | undefined) {
  return {
    name: "omni_app",
    description:
      "Manage applications on Android device. " +
      "Actions: 'launch' (start an app by package name), 'list' (list installed apps). " +
      "Use 'list' to find package names, then 'launch' to open specific apps.",
    parameters: {
      type: "object",
      properties: {
        action: {
          type: "string",
          enum: ["launch", "list"],
          description: "App management action",
        },
        packageName: {
          type: "string",
          description: "Package name to launch (e.g., com.tencent.mm for WeChat)",
        },
        device: {
          type: "string",
          description: "Device ID (optional)",
        },
      },
      required: ["action"],
    },
    execute: async (_id: string, args: unknown) => {
      const params = (args && typeof args === "object" ? args : {}) as AppParams;
      const device = resolveDevice(config, params.device);

      if (!device) {
        return {
          content: [{ type: "text" as const, text: "Error: No device configured" }],
        };
      }

      const timeout = config?.timeout;

      switch (params.action) {
        case "launch": {
          if (!params.packageName) {
            return {
              content: [{ type: "text" as const, text: "Error: packageName required for launch" }],
            };
          }
          const response = await callDevServer({
            device,
            endpoint: "/launchApplication",
            query: { packageName: params.packageName },
            timeout,
          });
          return formatResult(response);
        }

        case "list": {
          const response = await callDevServer<InstalledAppsData>({
            device,
            endpoint: "/listInstalledApplications",
            timeout,
          });
          if (!response.success) {
            return formatResult(response);
          }
          const data = response.data;
          if (!data?.packageNames?.length) {
            return {
              content: [{ type: "text" as const, text: "No applications found" }],
            };
          }
          // Format as a readable list
          const appList = data.packageNames.map((pkg, i) => {
            const name = data.applicationNames?.[i] ?? pkg;
            return `- ${name} (${pkg})`;
          }).join("\n");
          return {
            content: [
              {
                type: "text" as const,
                text: `Installed applications (${data.packageNames.length}):\n\n${appList}`,
              },
            ],
            details: data,
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
