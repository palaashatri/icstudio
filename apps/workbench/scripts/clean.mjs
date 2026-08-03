import { rm } from "node:fs/promises";

await Promise.all([
  rm(new URL("../dist-main", import.meta.url), { recursive: true, force: true }),
  rm(new URL("../dist-renderer", import.meta.url), { recursive: true, force: true })
]);
