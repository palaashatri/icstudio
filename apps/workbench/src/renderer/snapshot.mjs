/**
 * @typedef {Readonly<{
 *   schemaVersion: number,
 *   projectId: string,
 *   name: string,
 *   revision: number,
 *   libraries: number,
 *   cells: number,
 *   views: number
 * }>} ProjectSnapshot
 */

const objectIdPattern = /^[0-9a-f]{32}$/;

/** @param {unknown} value @param {string} field */
function nonNegativeInteger(value, field) {
  if (!Number.isSafeInteger(value) || value < 0) {
    throw new Error(`${field} must be a non-negative safe integer`);
  }
  return value;
}

/**
 * Decode the exact bounded JSON contract emitted by Project::summary_json().
 * @param {string} text
 * @returns {ProjectSnapshot}
 */
export function decodeSnapshot(text) {
  const value = JSON.parse(text);
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    throw new Error("project snapshot must be an object");
  }
  const record = /** @type {Record<string, unknown>} */ (value);
  const expectedKeys = [
    "schemaVersion",
    "projectId",
    "name",
    "revision",
    "libraries",
    "cells",
    "views"
  ];
  const actualKeys = Object.keys(record).sort();
  if (actualKeys.join("\n") !== [...expectedKeys].sort().join("\n")) {
    throw new Error("project snapshot contains missing or unexpected fields");
  }
  if (record.schemaVersion !== 1) {
    throw new Error(`unsupported project snapshot schema ${String(record.schemaVersion)}`);
  }
  if (typeof record.projectId !== "string" || !objectIdPattern.test(record.projectId)) {
    throw new Error("projectId must be a 128-bit lowercase hexadecimal identifier");
  }
  if (typeof record.name !== "string" || record.name.trim().length === 0) {
    throw new Error("project name must be a non-empty string");
  }

  return Object.freeze({
    schemaVersion: 1,
    projectId: record.projectId,
    name: record.name,
    revision: nonNegativeInteger(record.revision, "revision"),
    libraries: nonNegativeInteger(record.libraries, "libraries"),
    cells: nonNegativeInteger(record.cells, "cells"),
    views: nonNegativeInteger(record.views, "views")
  });
}

/**
 * @param {ProjectSnapshot} snapshot
 * @returns {ReadonlyArray<Readonly<{label: string, value: string}>>}
 */
export function snapshotRows(snapshot) {
  return Object.freeze([
    Object.freeze({ label: "Revision", value: String(snapshot.revision) }),
    Object.freeze({ label: "Libraries", value: String(snapshot.libraries) }),
    Object.freeze({ label: "Cells", value: String(snapshot.cells) }),
    Object.freeze({ label: "Views", value: String(snapshot.views) })
  ]);
}
