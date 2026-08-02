# ICStudio

ICStudio is an MIT-licensed, local-first custom-IC design and signoff platform under active clean-room development.

The authoritative product, solver, architecture, validation, and milestone contract is [`AGENTS.md`](AGENTS.md).

## Current implementation state

The repository is at **M0: reproducible factory candidate**. It currently contains:

- a dependency-free Rust workspace;
- the `icstudio` programme-state and checkpoint CLI;
- the `icstudio-mcp` read-only stdio MCP smoke server;
- machine-readable capability and truth state;
- tests and CI definitions for Linux, macOS, and Windows;
- release-build targets for platform-native CLI and MCP binaries.

It does **not** yet contain a schematic editor, layout editor, PDK runtime, circuit simulator, verification engine, extraction engine, desktop workbench, or tapeout flow.

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

Generate programme evidence:

```bash
just capability-report
just sbom
just license-check
```

Create and verify a pause-safe checkpoint:

```bash
just checkpoint CP-M0-FACTORY
just resume-check CP-M0-FACTORY
```

## MCP smoke server

Run:

```bash
cargo run --locked --bin icstudio-mcp
```

The M0 server negotiates MCP revision `2025-11-25` over stdio and exposes:

- resource: `icstudio://status`
- tool: `capability.report`
- prompt: `icstudio.m0.status`

This surface is read-only. Transactional design tools arrive in later milestones.

## Licence

Project-owned code and documentation are licensed under the [MIT License](LICENSE). See `AGENTS.md` for third-party, clean-room, PDK, model, and solver provenance requirements.
