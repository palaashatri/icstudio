import assert from "node:assert/strict";
import test from "node:test";
import { decodeSnapshot, snapshotRows } from "../src/renderer/snapshot.mjs";

const canonical =
  '{"schemaVersion":1,"projectId":"0123456789abcdef0123456789abcdef","name":"demo","revision":3,"libraries":1,"cells":1,"views":1}';

test("renderer preserves authoritative state and revision", () => {
  const snapshot = decodeSnapshot(canonical);
  assert.equal(snapshot.name, "demo");
  assert.equal(snapshot.revision, 3);
  assert.equal(snapshot.projectId, "0123456789abcdef0123456789abcdef");
  assert.deepEqual(snapshotRows(snapshot), [
    { label: "Revision", value: "3" },
    { label: "Libraries", value: "1" },
    { label: "Cells", value: "1" },
    { label: "Views", value: "1" }
  ]);
  assert.equal(Object.isFrozen(snapshot), true);
});

test("renderer rejects expanded or malformed project contracts", () => {
  assert.throws(
    () => decodeSnapshot(canonical.replace("}", ',"rawProjectFile":"forbidden"}')),
    /missing or unexpected fields/
  );
  assert.throws(
    () => decodeSnapshot(canonical.replace('"revision":3', '"revision":-1')),
    /revision must be a non-negative safe integer/
  );
  assert.throws(
    () => decodeSnapshot(canonical.replace('"schemaVersion":1', '"schemaVersion":2')),
    /unsupported project snapshot schema/
  );
});
