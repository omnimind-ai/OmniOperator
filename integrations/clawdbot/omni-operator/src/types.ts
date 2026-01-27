/**
 * OmniOperator Plugin Type Definitions
 */

// Plugin configuration types
export type DeviceConfig = {
  host: string;
  port?: number;
  label?: string;
};

export type OmniOperatorConfig = {
  devices?: Record<string, DeviceConfig>;
  defaultDevice?: string;
  timeout?: number;
};

// DevServer API response types
export type DevServerResponse<T = unknown> = {
  success: boolean;
  message: string;
  data?: T;
};

export type ScreenshotData = {
  imageBase64: string | null;
};

export type XmlData = {
  xml: string | null;
};

export type MetadataData = {
  packageName: string | null;
  activityName: string | null;
};

export type TimestampsData = {
  screenshot: number;
  xml: number;
};

export type InstalledAppsData = {
  packageNames: string[];
  applicationNames: string[];
};

export type CommandInfo = {
  name: string;
  description: string;
  argNames: string[];
};

// Tool parameter types
export type ScreenshotParams = {
  action: "image" | "xml" | "metadata" | "timestamps";
  device?: string;
  record?: boolean;
};

export type InteractParams = {
  action: "click" | "longClick" | "scroll";
  x?: number;
  y?: number;
  nodeId?: string;
  direction?: "up" | "down" | "left" | "right" | "forward" | "backward";
  distance?: number;
  device?: string;
};

export type InputParams = {
  action: "inputText" | "inputToFocused" | "copyToClipboard" | "injectIME";
  text: string;
  nodeId?: string;
  device?: string;
};

export type AppParams = {
  action: "launch" | "list";
  packageName?: string;
  device?: string;
};

export type NavigateParams = {
  action: "home" | "back";
  device?: string;
};

export type DialogParams = {
  action: "showMessage" | "confirm" | "choice";
  title?: string;
  content?: string;
  prompt?: string;
  options?: string;
  device?: string;
};
