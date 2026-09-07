import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const workbenchRoot = path.resolve(scriptDirectory, "..");
const arguments_ = process.argv.slice(2);

if (arguments_.length === 0) {
  console.error("run-npm.mjs requires an npm command");
  process.exit(2);
}

const result = spawnSync("npm", arguments_, {
  cwd: workbenchRoot,
  stdio: "inherit",
  shell: process.platform === "win32",
  windowsHide: true
});

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}
process.exit(result.status ?? 1);
