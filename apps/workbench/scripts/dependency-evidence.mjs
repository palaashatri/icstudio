import { mkdir, readFile, readdir, realpath, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const workbenchRoot = path.resolve(scriptDirectory, "..");
const repositoryRoot = path.resolve(workbenchRoot, "../..");
const artifactsDirectory = path.join(repositoryRoot, "artifacts");
const checkOnly = process.argv.includes("--check-only");

const allowedLicenseIds = new Set([
  "0BSD",
  "Apache-2.0",
  "BSD-2-Clause",
  "BSD-3-Clause",
  "BlueOak-1.0.0",
  "CC0-1.0",
  "ISC",
  "MIT",
  "MIT-0",
  "Python-2.0",
  "Unlicense"
]);
const expressionOperators = new Set(["AND", "OR"]);

function licenseExpression(manifest) {
  if (typeof manifest.license === "string") {
    return manifest.license.trim();
  }
  if (manifest.license && typeof manifest.license.type === "string") {
    return manifest.license.type.trim();
  }
  if (Array.isArray(manifest.licenses)) {
    const values = manifest.licenses
      .map((entry) => (typeof entry === "string" ? entry : entry?.type))
      .filter((entry) => typeof entry === "string" && entry.trim().length > 0);
    if (values.length > 0) {
      return values.join(" OR ");
    }
  }
  return "NOASSERTION";
}

function isAllowed(expression) {
  if (expression === "NOASSERTION") {
    return false;
  }
  const tokens = expression
    .replaceAll("(", " ")
    .replaceAll(")", " ")
    .split(/\s+/)
    .filter(Boolean);
  return tokens.every(
    (token) => expressionOperators.has(token) || allowedLicenseIds.has(token)
  );
}

function spdxId(name, version) {
  return `SPDXRef-NPM-${name}-${version}`.replace(/[^A-Za-z0-9.-]/g, "-");
}

const packages = new Map();
const visitedDirectories = new Set();

async function collectPackage(packageDirectory) {
  let resolved;
  try {
    resolved = await realpath(packageDirectory);
  } catch {
    return;
  }
  if (visitedDirectories.has(resolved)) {
    return;
  }
  visitedDirectories.add(resolved);

  let manifest;
  try {
    manifest = JSON.parse(await readFile(path.join(resolved, "package.json"), "utf8"));
  } catch {
    return;
  }
  if (typeof manifest.name !== "string" || typeof manifest.version !== "string") {
    throw new Error(`invalid package manifest at ${resolved}`);
  }
  const license = licenseExpression(manifest);
  const key = `${manifest.name}@${manifest.version}`;
  packages.set(key, {
    name: manifest.name,
    version: manifest.version,
    license
  });
  await collectNodeModules(path.join(resolved, "node_modules"));
}

async function collectNodeModules(nodeModulesDirectory) {
  let entries;
  try {
    entries = await readdir(nodeModulesDirectory, { withFileTypes: true });
  } catch {
    return;
  }

  for (const entry of entries) {
    if (entry.name.startsWith(".")) {
      continue;
    }
    const entryPath = path.join(nodeModulesDirectory, entry.name);
    if (entry.name.startsWith("@")) {
      const scopedEntries = await readdir(entryPath, { withFileTypes: true });
      for (const scopedEntry of scopedEntries) {
        if (scopedEntry.isDirectory() || scopedEntry.isSymbolicLink()) {
          await collectPackage(path.join(entryPath, scopedEntry.name));
        }
      }
    } else if (entry.isDirectory() || entry.isSymbolicLink()) {
      await collectPackage(entryPath);
    }
  }
}

await collectNodeModules(path.join(workbenchRoot, "node_modules"));
if (packages.size === 0) {
  throw new Error("no installed workbench dependencies were found");
}

const ordered = [...packages.values()].sort((left, right) =>
  `${left.name}@${left.version}`.localeCompare(`${right.name}@${right.version}`)
);
const rejected = ordered.filter((entry) => !isAllowed(entry.license));
if (rejected.length > 0) {
  throw new Error(
    `workbench dependency licence policy rejected: ${rejected
      .map((entry) => `${entry.name}@${entry.version} (${entry.license})`)
      .join(", ")}`
  );
}

if (!checkOnly) {
  await mkdir(artifactsDirectory, { recursive: true });
  const notices = {
    schemaVersion: 1,
    packageManager: "npm",
    packages: ordered
  };
  const sbom = {
    spdxVersion: "SPDX-2.3",
    dataLicense: "CC0-1.0",
    SPDXID: "SPDXRef-DOCUMENT",
    name: "icstudio-workbench",
    documentNamespace: `https://github.com/palaashatri/icstudio/sbom/workbench-${ordered.length}`,
    creationInfo: {
      creators: ["Tool: icstudio-workbench-dependency-evidence"]
    },
    packages: ordered.map((entry) => ({
      name: entry.name,
      SPDXID: spdxId(entry.name, entry.version),
      versionInfo: entry.version,
      downloadLocation: "NOASSERTION",
      filesAnalyzed: false,
      licenseConcluded: entry.license,
      licenseDeclared: entry.license,
      copyrightText: "NOASSERTION"
    }))
  };
  await writeFile(
    path.join(artifactsDirectory, "workbench-third-party.json"),
    `${JSON.stringify(notices, null, 2)}\n`
  );
  await writeFile(
    path.join(artifactsDirectory, "workbench.spdx.json"),
    `${JSON.stringify(sbom, null, 2)}\n`
  );
}

console.log(`validated ${ordered.length} installed workbench packages`);
