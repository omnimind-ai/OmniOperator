/**
 * OmniOperator Plugin - Control Android devices via HTTP API
 *
 * This plugin enables Clawdbot to control Android devices running OmniOperator DevServer.
 * It provides tools for:
 * - Screen capture (screenshot, UI XML, app metadata)
 * - Touch interactions (click, long click, scroll)
 * - Text input
 * - Application management
 * - Navigation (home, back)
 * - User dialogs
 *
 * Configuration example:
 * {
 *   "plugins": {
 *     "entries": {
 *       "omni-operator": {
 *         "enabled": true,
 *         "config": {
 *           "devices": {
 *             "phone": { "host": "192.168.1.100", "port": 8080, "label": "My Phone" }
 *           },
 *           "defaultDevice": "phone",
 *           "timeout": 30000
 *         }
 *       }
 *     }
 *   }
 * }
 */

import type { ClawdbotPluginApi } from "clawdbot/plugin-sdk";

import type { OmniOperatorConfig } from "./src/types.js";
import { createScreenshotTool } from "./src/tools/screenshot.js";
import { createInteractTool } from "./src/tools/interaction.js";
import { createInputTool } from "./src/tools/input.js";
import { createAppTool } from "./src/tools/app.js";
import { createNavigateTool } from "./src/tools/navigation.js";
import { createDialogTool } from "./src/tools/dialog.js";
import { createStatusTool } from "./src/tools/status.js";

export default {
  id: "omni-operator",
  name: "OmniOperator",
  description: "Control Android devices via OmniOperator DevServer HTTP API",
  version: "1.0.0",

  register(api: ClawdbotPluginApi) {
    const config = api.pluginConfig as OmniOperatorConfig | undefined;

    // Log device configuration
    const deviceCount = config?.devices ? Object.keys(config.devices).length : 0;
    if (deviceCount > 0) {
      const deviceNames = Object.entries(config!.devices!).map(
        ([id, dev]) => `${id} (${dev.label ?? dev.host})`
      );
      api.logger.info(`OmniOperator loaded with ${deviceCount} device(s): ${deviceNames.join(", ")}`);
    } else {
      api.logger.warn("OmniOperator loaded but no devices configured");
    }

    // Register all tools
    api.registerTool(createScreenshotTool(config), {
      name: "omni_screenshot",
    });

    api.registerTool(createInteractTool(config), {
      name: "omni_interact",
    });

    api.registerTool(createInputTool(config), {
      name: "omni_input",
    });

    api.registerTool(createAppTool(config), {
      name: "omni_app",
    });

    api.registerTool(createNavigateTool(config), {
      name: "omni_navigate",
    });

    api.registerTool(createDialogTool(config), {
      name: "omni_dialog",
    });

    api.registerTool(createStatusTool(config), {
      name: "omni_status",
    });

    api.logger.info("OmniOperator plugin registered 7 tools");
  },
};
