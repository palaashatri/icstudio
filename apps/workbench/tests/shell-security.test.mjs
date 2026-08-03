import assert from "node:assert/strict";
import { readFile, readdir } from "node:fs/promises";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const workbenchRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

test("compiled Electron shell preserves the least-privilege boundary", async () => {
  const main = await readFile(path.join(workbenchRoot, "dist-main", "main.js"), "utf8");
  const preload = await readFile(path.join(workbenchRoot, "dist-main", "preload.js"), "utf8");

  assert.match(main, /contextIsolation:\s*true/);
  assert.match(main, /nodeIntegration:\s*false/);
  assert.match(main, /sandbox:\s*true/);
  assert.match(main, /project:snapshot/);
  assert.match(main, /icstudio-ui-bridge/);
  assert.match(preload, /exposeInMainWorld\("icstudio"/);
  assert.match(preload, /readProjectSnapshot/);
  assert.doesNotMatch(preload, /readFile|writeFile|execFile|spawn/);
});

test("renderer build contains the project snapshot and WebGPU scene", async () => {
  const rendererDirectory = path.join(workbenchRoot, "dist-renderer");
  const index = await readFile(path.join(rendererDirectory, "index.html"), "utf8");
  const assetNames = await readdir(path.join(rendererDirectory, "assets"));
  const scripts = assetNames.filter((name) => name.endsWith(".js"));
  assert.ok(scripts.length > 0, "renderer JavaScript bundle is missing");
  const bundle = await readFile(path.join(rendererDirectory, "assets", scripts[0]), "utf8");

  assert.match(index, /ICStudio Workbench/);
  assert.match(bundle, /readProjectSnapshot/);
  assert.match(bundle, /requestAdapter/);
  assert.match(bundle, /Authoritative revision/);
});
