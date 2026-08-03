# ICStudio

ICStudio is an MIT-licensed, local-first custom-IC design and signoff platform under active clean-room development.

The authoritative product, solver, architecture, validation, and milestone contract is [`AGENTS.md`](AGENTS.md). Long-lived implementation work occurs on `milestone/m0-reproducible-factory` until the project adopts a differently named implementation branch through an explicit decision.

## Current implementation state

**Truth score: 5/100.** M0 is accepted and M1 is approximately 50% complete.

The implementation branch currently contains:

- reproducible Rust builds, strict CI, evidence generation, checkpoints, and resume validation;
- an authoritative library/cell/view project hierarchy with stable 128-bit IDs;
- optimistic revision-safe transactions and deterministic native metadata serialization;
- write-ahead journaling and recovery after injected termination;
- exact signed 64-bit rectangle geometry and a deterministic spatial-index reference implementation;
- an accepted one-million-shape build/query baseline;
- a versioned typed worker protocol and an isolated worker process;
- proof that a worker crash does not corrupt project state or terminate the platform CLI;
- CLI project creation, inspection, and hierarchy mutation commands;
- read-only, revision-addressed MCP project resources and `project.inspect`;
- tests and build targets for Linux, macOS, and Windows.

M1 is not complete. The desktop shell and UI state consumer, PDK base, netlist IR, result database, and GDSII/SPICE parser scaffolds remain unimplemented.

ICStudio still has no schematic editor, layout editor, circuit simulator, DRC, LVS, extraction, EM, thermal, reliability, photonics, optimization, or qualified tapeout flow.

## Developer commands

Install Rust 1.85.1 and [`just`](https://github.com/casey/just), then run:

```bash
just bootstrap
just build
just test-fast
just validate
just truth
just mcp-smoke
```

Run the explicit M1 geometry baseline:

```bash
just test-m1-geometry
```

Generate programme evidence:

```bash
just capability-report
just sbom
just license-check
```

Create and verify a pause-safe checkpoint:

```bash
just checkpoint CP-M1-KERNEL-WIP
just resume-check CP-M1-KERNEL-WIP
```

## Headless project workflow

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

## Native binaries

Release-target workflows build:

- `icstudio`
- `icstudio-mcp`
- `icstudio-worker`

Targets are Linux x86-64, Windows x86-64, Apple Silicon macOS, and Intel macOS. Signing, notarization, installers, and the graphical workbench are not implemented.

## Licence

Project-owned code and documentation are licensed under the [MIT License](LICENSE). See `AGENTS.md` for third-party, clean-room, PDK, model, and solver provenance requirements.
