/**
 * Device Status/Connection Check Tool
 */

import { resolveDevice, buildUrl } from "../client.js";
import type { OmniOperatorConfig } from "../types.js";

export function createStatusTool(config: OmniOperatorConfig | undefined) {
  return {
    name: "omni_status",
    description:
      "Check connection status of Android devices. " +
      "Actions: 'ping' (test connectivity and get version), 'devices' (list configured devices). " +
      "Use 'ping' before other operations to verify the device is reachable.",
    parameters: {
      type: "object",
      properties: {
        action: {
          type: "string",
          enum: ["ping", "devices"],
          description: "Status check type: ping=test connection, devices=list configured",
        },
        device: {
          type: "string",
          description: "Device ID to check (optional, uses default)",
        },
      },
      required: ["action"],
    },
    execute: async (_id: string, args: unknown) => {
      const params = (args && typeof args === "object" ? args : {}) as {
        action: string;
        device?: string;
      };

      switch (params.action) {
        case "devices": {
          // List all configured devices
          if (!config?.devices || Object.keys(config.devices).length === 0) {
            return {
              content: [
                {
                  type: "text" as const,
                  text: "No devices configured. Add devices in plugin config:\n\n" +
                    "clawdbot config set plugins.entries.omni-operator.config.devices.phone.host \"192.168.1.100\"\n" +
                    "clawdbot config set plugins.entries.omni-operator.config.devices.phone.port 8080\n" +
                    "clawdbot config set plugins.entries.omni-operator.config.defaultDevice \"phone\"",
                },
              ],
            };
          }

          const deviceList = Object.entries(config.devices)
            .map(([id, dev]) => {
              const isDefault = id === config.defaultDevice ? " (default)" : "";
              return `- ${id}${isDefault}: ${dev.host}:${dev.port ?? 8080} [${dev.label ?? "unnamed"}]`;
            })
            .join("\n");

          return {
            content: [
              {
                type: "text" as const,
                text: `Configured devices:\n\n${deviceList}`,
              },
            ],
            details: { devices: config.devices, defaultDevice: config.defaultDevice },
          };
        }

        case "ping": {
          const device = resolveDevice(config, params.device);
          if (!device) {
            return {
              content: [{ type: "text" as const, text: "Error: No device configured" }],
            };
          }

          const url = buildUrl(device, "/health");
          const startTime = Date.now();
          const timeout = config?.timeout ?? 5000;

          try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), timeout);

            const response = await fetch(url, {
              method: "GET",
              signal: controller.signal,
            });

            clearTimeout(timeoutId);
            const latency = Date.now() - startTime;

            if (response.ok) {
              const version = await response.text();
              return {
                content: [
                  {
                    type: "text" as const,
                    text: `✓ Connected to ${device.label ?? device.host}:${device.port ?? 8080}\n` +
                      `Version: ${version.trim()}\n` +
                      `Latency: ${latency}ms`,
                  },
                ],
                details: {
                  connected: true,
                  host: device.host,
                  port: device.port ?? 8080,
                  version: version.trim(),
                  latencyMs: latency,
                },
              };
            }

            return {
              content: [
                {
                  type: "text" as const,
                  text: `✗ Connection failed to ${device.host}:${device.port ?? 8080}\n` +
                    `HTTP ${response.status}: ${response.statusText}`,
                },
              ],
              details: { connected: false, status: response.status },
            };
          } catch (err) {
            const latency = Date.now() - startTime;
            let errorMsg: string;

            if (err instanceof Error) {
              if (err.name === "AbortError") {
                errorMsg = `Timeout after ${timeout}ms`;
              } else {
                errorMsg = err.message;
              }
            } else {
              errorMsg = String(err);
            }

            return {
              content: [
                {
                  type: "text" as const,
                  text: `✗ Connection failed to ${device.host}:${device.port ?? 8080}\n` +
                    `Error: ${errorMsg}\n` +
                    `(after ${latency}ms)`,
                },
              ],
              details: { connected: false, error: errorMsg, latencyMs: latency },
            };
          }
        }

        default:
          return {
            content: [{ type: "text" as const, text: `Unknown action: ${params.action}` }],
          };
      }
    },
  };
}
