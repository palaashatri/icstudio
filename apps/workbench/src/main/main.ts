import { app, BrowserWindow, ipcMain } from "electron";
import { execFile } from "node:child_process";
import { access } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { promisify } from "node:util";

const execFileAsync = promisify(execFile);
const moduleDirectory = path.dirname(fileURLToPath(import.meta.url));
const workbenchRoot = path.resolve(moduleDirectory, "..");
const rendererEntry = path.join(workbenchRoot, "dist-renderer", "index.html");
const preloadEntry = path.join(moduleDirectory, "preload.js");

function option(name: string): string | undefined {
  const index = process.argv.indexOf(name);
  if (index < 0) {
    return undefined;
  }
  const value = process.argv[index + 1];
  if (!value || value.startsWith("--")) {
    throw new Error(`${name} requires a value`);
  }
  return value;
}

function projectPath(): string {
  const value = option("--project") ?? process.env.ICSTUDIO_ACTIVE_PROJECT;
  if (!value) {
    throw new Error(
      "No active project. Launch with --project PATH or set ICSTUDIO_ACTIVE_PROJECT."
    );
  }
  return path.resolve(value);
}

async function bridgeBinary(): Promise<string> {
  const executable = process.platform === "win32" ? "icstudio-ui-bridge.exe" : "icstudio-ui-bridge";
  const configured = process.env.ICSTUDIO_UI_BRIDGE;
  const candidates = [
    configured,
    path.join(process.resourcesPath, executable),
    path.resolve(process.cwd(), "target", "debug", executable),
    path.resolve(workbenchRoot, "..", "..", "target", "debug", executable)
  ].filter((candidate): candidate is string => Boolean(candidate));

  for (const candidate of candidates) {
    try {
      await access(candidate);
      return candidate;
    } catch {
      // Continue to the next deterministic candidate.
    }
  }
  throw new Error(
    `Unable to locate ${executable}. Set ICSTUDIO_UI_BRIDGE to the built bridge binary.`
  );
}

async function readSnapshot(): Promise<string> {
  const bridge = await bridgeBinary();
  const { stdout, stderr } = await execFileAsync(bridge, ["--path", projectPath()], {
    encoding: "utf8",
    maxBuffer: 1024 * 1024,
    windowsHide: true,
    timeout: 5000
  });
  if (stderr.trim()) {
    console.warn(stderr.trim());
  }
  return stdout.trim();
}

function createWindow(): BrowserWindow {
  const window = new BrowserWindow({
    width: 1180,
    height: 760,
    minWidth: 820,
    minHeight: 560,
    backgroundColor: "#0b0f17",
    show: false,
    title: "ICStudio Workbench",
    webPreferences: {
      preload: preloadEntry,
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });

  window.once("ready-to-show", () => window.show());
  void window.loadFile(rendererEntry);
  return window;
}

ipcMain.handle("project:snapshot", async () => readSnapshot());

void app.whenReady().then(() => {
  createWindow();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") {
    app.quit();
  }
});
