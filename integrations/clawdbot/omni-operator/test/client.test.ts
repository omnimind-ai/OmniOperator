import assert from "node:assert/strict";
import test from "node:test";

import { buildUrl, callDevServer, formatImageResult } from "../src/client.ts";

test("buildUrl should compose host, port, path and query", () => {
  const url = buildUrl(
    { host: "192.168.1.10", port: 8081 },
    "/captureScreenshotXml",
    { record: false, page: 1 },
  );

  assert.equal(
    url,
    "http://192.168.1.10:8081/captureScreenshotXml?record=false&page=1",
  );
});

test("callDevServer should return timeout failure", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = ((_: string, init?: RequestInit) => {
    return new Promise((_, reject) => {
      init?.signal?.addEventListener("abort", () => {
        reject(new DOMException("Aborted", "AbortError"));
      });
    });
  }) as typeof fetch;

  try {
    const res = await callDevServer({
      device: { host: "127.0.0.1", port: 8080 },
      endpoint: "/health",
      timeout: 10,
    });
    assert.equal(res.success, false);
    assert.match(res.message, /timeout/i);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("callDevServer should map non-2xx to failure", async () => {
  const originalFetch = globalThis.fetch;
  globalThis.fetch = (async () => {
    return new Response("{}", { status: 500, statusText: "Server Error" });
  }) as typeof fetch;

  try {
    const res = await callDevServer({
      device: { host: "127.0.0.1", port: 8080 },
      endpoint: "/health",
    });
    assert.equal(res.success, false);
    assert.match(res.message, /HTTP 500/);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test("formatImageResult should remove data uri prefix and detect jpeg mime", () => {
  const jpegBase64 = Buffer.from([0xff, 0xd8, 0xff, 0xdb, 0x00]).toString("base64");
  const response = {
    success: true,
    message: "ok",
    data: {
      imageBase64: `data:image/png;base64,${jpegBase64}`,
    },
  };

  const result = formatImageResult(response);
  const imagePart = result.content.find((item) => item.type === "image");

  assert.ok(imagePart);
  assert.equal(imagePart.type, "image");
  assert.equal(imagePart.mimeType, "image/jpeg");
  assert.equal(imagePart.data, jpegBase64);
});

