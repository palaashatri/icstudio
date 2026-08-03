# ICStudio

ICStudio is an MIT-licensed, local-first custom-IC design and signoff platform under active clean-room development.

The authoritative product, solver, architecture, validation, and milestone contract is [`AGENTS.md`](AGENTS.md). Long-lived implementation work occurs on `milestone/m0-reproducible-factory` until the project adopts a differently named implementation branch through an explicit decision.

## Current implementation state

**Truth score: 8/100. M0 and M1 are accepted. M2 has not started.**

The implementation branch currently contains:

- reproducible Rust 2024 builds, SHA-pinned GitHub Actions, evidence generation, immutable checkpoints, and resume validation;
- a digest-pinned reproducible Linux image that runs as a non-root user;
- an authoritative library/cell/view project hierarchy with stable 128-bit IDs;
- optimistic revision-safe transactions, deterministic native metadata serialization, journaling, and recovery after injected termination;
- exact signed 64-bit rectangle geometry and a deterministic spatial-index reference implementation;
- an accepted one-million-shape build/query baseline;
- a versioned typed worker protocol, isolated worker process, and worker-crash isolation proof;
- CLI project creation, inspection, and hierarchy mutation commands with precise option validation;
- a bounded top-level JSON-RPC parser and read-only revision-addressed MCP project resources;
- deterministic PDK technology metadata with layer-purpose/GDS mapping and safe model references;
- a structural netlist IR and bounded SPICE parser scaffold with source-line diagnostics;
- a deterministic result manifest and scalar signal-vector store using exact IEEE-754 bit patterns;
- a bounded GDSII record-framing parser with length, parity, offset, and truncation validation;
- an Electron and React desktop shell with an operational WebGPU scene prototype;
- a least-privilege Rust UI bridge that exposes the same immutable project summary used by CLI and MCP;
- an automated test proving CLI, UI, and MCP expose identical project state and revision;
- a locked npm dependency graph, permissive-licence enforcement, and separate Rust and workbench SPDX 2.3 evidence;
- strict tests and build targets for Linux, macOS, Apple Silicon macOS, Intel macOS, and Windows.

The accepted restart point is `CP-M1-KERNEL`.

M1 does **not** include a schematic editor, layout editor, circuit simulator, DRC, LVS, extraction, EM, thermal, reliability, photonics, optimization, or qualified tapeout flow. The workbench is currently a read-only project-state and WebGPU scene prototype. Production hierarchical geometry, undo/redo, concurrent-writer coordination, PCell and rule-deck execution, complete GDSII/SPICE interoperability, result streaming, application packaging, signing, notarization, and installers remain future work.

## Developer commands

Install Rust 1.85.1, Node.js 22 with npm, and [`just`](https://github.com/casey/just), then run:

```bash
just bootstrap
just build
just test-fast
just ui-check
just validate
just truth
just mcp-smoke
```

Run the explicit M1 geometry baseline:

```bash
just test-m1-geometry
```

Generate programme and supply-chain evidence:

```bash
just capability-report
just sbom
just license-check
just ui-license-check
```

Verify the accepted restart point:

```bash
just resume-check CP-M1-KERNEL
```

## Project workflow

Create a project and inspect revision zero:

```bash
cargo run --locked --bin icstudio -- project create --path demo.icstudio --name demo
cargo run --locked --bin icstudio -- project show --path demo.icstudio
```

Apply revision-safe hierarchy edits:

```bash
cargo run --locked --bin icstudio -- project add-library \
  --path demo.icstudio --name analog --expected-revision 0

cargo run --locked --bin icstudio -- project add-cell \
  --path demo.icstudio --library analog --name inverter --expected-revision 1

cargo run --locked --bin icstudio -- project add-view \
  --path demo.icstudio --library analog --cell inverter \
  --name schematic --kind schematic --expected-revision 2
```

Every mutating command validates the caller's expected revision. Stale edits fail rather than silently overwriting newer project state.

## Desktop workbench

Build the Rust bridge and the locked Electron/React workbench:

```bash
cargo build --locked --bin icstudio-ui-bridge
just ui-build
```

Launch it against an active project on Linux or macOS:

```bash
ICSTUDIO_ACTIVE_PROJECT=demo.icstudio \
ICSTUDIO_UI_BRIDGE=target/debug/icstudio-ui-bridge \
node apps/workbench/scripts/run-npm.mjs start
```

On Windows, set the same two environment variables and point `ICSTUDIO_UI_BRIDGE` to `target\debug\icstudio-ui-bridge.exe` before running the Node command.

The Electron renderer is sandboxed, uses context isolation, has Node integration disabled, and receives only one immutable project-snapshot IPC method. It does not parse project files or own engineering state.

## MCP server

Run against an active project:

```bash
ICSTUDIO_ACTIVE_PROJECT=demo.icstudio \
  cargo run --locked --bin icstudio-mcp
```

The server negotiates MCP revision `2025-11-25` over stdio and exposes:

- programme resource: `icstudio://status`
- active-project resource: `icstudio://project/revision/{revision}`
- tools: `capability.report`, `project.inspect`
- prompts: `icstudio.m0.status`, `icstudio.project.review`

The MCP project surface remains read-only. Transactional semantic design patches arrive in a later milestone.

## M1 contracts

- `schemas/project-v1.txt`
- `schemas/rpc-v1.txt`
- `schemas/pdk-v1.txt`
- `schemas/results-v1.txt`
- `schemas/ui-snapshot-v1.txt`

The SPICE and GDSII implementations are deliberately bounded parser scaffolds. They do not claim simulation semantics or full import/export fidelity.

## Native binaries

Release-target workflows build:

- `icstudio`
- `icstudio-mcp`
- `icstudio-ui-bridge`
- `icstudio-worker`

Targets are Linux x86-64, Windows x86-64, Apple Silicon macOS, and Intel macOS. Electron application packaging, signing, notarization, and installers are not implemented.

## Licence

Project-owned code and documentation are licensed under the [MIT License](LICENSE). See `AGENTS.md` for third-party, clean-room, PDK, model, and solver provenance requirements.
