/**
 * OmniOperator DevServer HTTP Client
 */

import type { DeviceConfig, DevServerResponse, OmniOperatorConfig } from "./types.js";

const DEFAULT_PORT = 8080;
const DEFAULT_TIMEOUT = 30000;

/**
 * Resolve device configuration by ID
 */
export function resolveDevice(
  config: OmniOperatorConfig | undefined,
  deviceId?: string,
): DeviceConfig | null {
  if (!config?.devices) return null;

  const id = deviceId ?? config.defaultDevice;
  if (!id) {
    // Return first device if no default specified
    const firstKey = Object.keys(config.devices)[0];
    return firstKey ? config.devices[firstKey] : null;
  }

  return config.devices[id] ?? null;
}

/**
 * Build DevServer URL
 */
export function buildUrl(
  device: DeviceConfig,
  endpoint: string,
  query?: Record<string, string | number | boolean>,
): string {
  const port = device.port ?? DEFAULT_PORT;
  const url = new URL(`http://${device.host}:${port}${endpoint}`);

  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null) {
        url.searchParams.set(key, String(value));
      }
    }
  }

  return url.toString();
}

/**
 * Call DevServer HTTP API
 */
export async function callDevServer<T = unknown>(params: {
  device: DeviceConfig;
  endpoint: string;
  query?: Record<string, string | number | boolean>;
  timeout?: number;
}): Promise<DevServerResponse<T>> {
  const { device, endpoint, query, timeout = DEFAULT_TIMEOUT } = params;

  const url = buildUrl(device, endpoint, query);

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    const response = await fetch(url, {
      method: "GET",
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      return {
        success: false,
        message: `HTTP ${response.status}: ${response.statusText}`,
      };
    }

    const data = (await response.json()) as DevServerResponse<T>;
    return data;
  } catch (error) {
    if (error instanceof Error) {
      if (error.name === "AbortError") {
        return {
          success: false,
          message: `Request timeout after ${timeout}ms`,
        };
      }
      return {
        success: false,
        message: `Network error: ${error.message}`,
      };
    }
    return {
      success: false,
      message: `Unknown error: ${String(error)}`,
    };
  }
}

/**
 * Helper to format tool result
 */
export function formatResult(response: DevServerResponse<unknown>): {
  content: Array<{ type: "text"; text: string } | { type: "image"; data: string; mimeType: string }>;
  details?: unknown;
} {
  if (!response.success) {
    return {
      content: [{ type: "text" as const, text: `Error: ${response.message}` }],
    };
  }

  return {
    content: [{ type: "text" as const, text: response.message }],
    details: response.data,
  };
}

/**
 * Detect MIME type from base64 image data by checking magic bytes
 */
function detectImageMimeType(base64Data: string): string {
  // Remove data URI prefix if present
  const cleanBase64 = base64Data.replace(/^data:image\/[a-z]+;base64,/i, "");
  
  // Decode first few bytes to check magic bytes
  try {
    const bytes = Buffer.from(cleanBase64.slice(0, 20), "base64");
    
    // PNG: 89 50 4E 47
    if (bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47) {
      return "image/png";
    }
    
    // JPEG: FF D8 FF
    if (bytes[0] === 0xff && bytes[1] === 0xd8 && bytes[2] === 0xff) {
      return "image/jpeg";
    }
    
    // WebP: 52 49 46 46 ... 57 45 42 50
    if (bytes[0] === 0x52 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x46) {
      return "image/webp";
    }
    
    // GIF: 47 49 46 38
    if (bytes[0] === 0x47 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x38) {
      return "image/gif";
    }
  } catch {
    // Fall through to default
  }
  
  // Default to PNG (most common for screenshots)
  return "image/png";
}

/**
 * Helper to format result with image
 */
export function formatImageResult(
  response: DevServerResponse<{ imageBase64: string | null }>,
): {
  content: Array<{ type: "text"; text: string } | { type: "image"; data: string; mimeType: string }>;
  details?: unknown;
} {
  if (!response.success) {
    return {
      content: [{ type: "text" as const, text: `Error: ${response.message}` }],
    };
  }

  let imageBase64 = response.data?.imageBase64;
  if (!imageBase64) {
    return {
      content: [{ type: "text" as const, text: "Screenshot captured but no image data returned" }],
    };
  }

  // Detect actual MIME type from image data
  const mimeType = detectImageMimeType(imageBase64);
  
  // Remove data URI prefix if present (some APIs return with prefix)
  imageBase64 = imageBase64.replace(/^data:image\/[a-z]+;base64,/i, "");

  return {
    content: [
      { type: "text" as const, text: "Screenshot captured successfully" },
      { type: "image" as const, data: imageBase64, mimeType },
    ],
    details: { hasImage: true, mimeType },
  };
}
