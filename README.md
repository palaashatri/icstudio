# ICStudio

ICStudio is an MIT-licensed, local-first custom-IC design and signoff platform under active clean-room development.

The authoritative product, solver, architecture, validation, and milestone contract is [`docs/CONSTITUTION.md`](docs/CONSTITUTION.md). Instructions for coding agents (commands, rules, workflow) are in [`AGENTS.md`](AGENTS.md).

## Current implementation state

**Live truth score: 8/100** (`.project/truth.json`). The authoritative stack is **Java 25 + Swing** (ADR-0001). M0 and M1 are accepted at checkpoints `CP-JAVA-M0-FACTORY` (2/100) and `CP-JAVA-M1-KERNEL` (8/100). The earlier Rust/Electron implementation reached the same M1 contract at `CP-M1-KERNEL`; it has been retired, and its behaviour is preserved as a conformance corpus under `java/icstudio-conformance/src/test/resources/rust-oracle/`.

The Java implementation contains:

- a reproducible JDK 25 factory: Maven Wrapper pinned to 3.9.16, a Temurin 25 non-root image, CycloneDX SBOM, and a dependency licence allowlist enforced in CI on Ubuntu, macOS and Windows;
- an authoritative library/cell/view project hierarchy with stable 128-bit IDs and deterministic serialization;
- revision-checked transactions with a write-ahead journal and crash recovery;
- exact signed 64-bit rectangle geometry, a reference spatial index, and the accepted one-million-shape baseline;
- a versioned worker protocol with an isolated worker JVM, and proof that a worker crash cannot corrupt project state;
- a headless CLI and a read-only MCP gateway that share the same project services, with identical state and revision;
- a Swing workbench with an ICStudio design system and a Skia-rendered engineering canvas;
- deterministic PDK metadata with layer-purpose and GDS mapping checks, a structural SPICE netlist IR, a result store with exact IEEE-754 samples, and GDSII record framing;
- a conformance suite that replays the recorded Rust M1 behaviour byte-for-byte or message-for-message.

M1 does **not** include a schematic editor, layout editor, circuit simulator, DRC, LVS, extraction, EM, thermal, reliability, photonics, optimization, or qualified tapeout flow. The workbench is a read-only project-state view. Production hierarchical geometry, undo/redo, concurrent-writer coordination, PCell and rule-deck execution, complete GDSII/SPICE interoperability, result streaming, native material effects, signed release packaging and installers remain future work.

## Roadmap

1. **Open a real PDK project:** load SkyWater SKY130 or GF180MCU layers and devices into the PDK model, so the workbench shows real technology data.
2. **Schematic capture and simulation through ngspice:** draw a small circuit, netlist it, run DC and transient analysis in ngspice as an external engine, and plot waveforms. Driving mature open engines (ngspice or Xyce, KLayout or Magic for DRC, Netgen for LVS) through the process boundary gets designers useful results before in-house solvers exist.
3. **Layout view with KLayout-backed DRC** for the same PDK.
4. **MCP write tools** (semantic design patches) so an AI agent can make reviewed, revision-safe edits.

## Developer commands

Install JDK 25 and [`just`](https://github.com/casey/just). On Linux, also install `libegl1` and `libgl1` (Skia links against them). Then:

```bash
just bootstrap
just test-fast
just verify
just validate
just truth
just license-check
```

Run the explicit M1 geometry baseline and the MCP and surface-equivalence gates:

```bash
just test-m1-geometry
just mcp-smoke
```

Generate programme and supply-chain evidence:

```bash
just capability-report
just sbom
```

Verify the current accepted checkpoint. `CP-JAVA-M0-FACTORY` is historical: it records the J0 state and resumes only against that state.

```bash
just resume-check CP-JAVA-M1-KERNEL
```

Without `just`, use the Maven Wrapper directly: `./mvnw -B -ntp verify` (on Windows, `.\mvnw.cmd -B -ntp verify`).

## Project workflow

Create a project, inspect revision zero, and apply revision-safe hierarchy edits:

```bash
just cli project create --path demo.icstudio --name demo
just cli project show --path demo.icstudio

just cli project add-library --path demo.icstudio --name analog --expected-revision 0
just cli project add-cell --path demo.icstudio --library analog --name inverter --expected-revision 1
just cli project add-view --path demo.icstudio --library analog --cell inverter \
  --name schematic --kind schematic --expected-revision 2
```

Every mutating command validates the caller's expected revision. A stale edit fails rather than silently overwriting newer project state.

## Desktop workbench

```bash
just studio --project demo.icstudio
```

The workbench opens the project read-only through the same transactional service the CLI uses. It shows the library tree, the Skia hierarchy canvas, and the project inspector. In a headless environment, `just studio` prints the authoritative summary instead of opening a window.

## MCP server

The gateway speaks MCP revision `2025-11-25` over stdio. Configure it with `ICSTUDIO_PROJECT_ROOT` (the programme root) and `ICSTUDIO_ACTIVE_PROJECT` (the project directory), then run the `dev.icstudio.mcp.McpMain` entry point from the reactor classes. It exposes:

- programme resource: `icstudio://status`
- active-project resource: `icstudio://project/revision/{revision}`
- tools: `capability.report`, `project.inspect`
- prompts: `icstudio.m0.status`, `icstudio.project.review`

The MCP surface is read-only. Transactional semantic design patches arrive in a later milestone.

## Contracts

- `schemas/project-v1.txt`, `schemas/rpc-v1.txt`, `schemas/pdk-v1.txt`, `schemas/results-v1.txt`, `schemas/ui-snapshot-v1.txt`
- `java/icstudio-conformance/src/test/resources/rust-oracle/` — the recorded M1 behaviour that the Java implementation must keep reproducing

The SPICE and GDSII implementations are deliberately bounded parser scaffolds. They do not claim simulation semantics or full import and export fidelity.

## Licence

Project-owned code and documentation are licensed under the [MIT License](LICENSE). Third-party dependencies are listed in the CycloneDX SBOM and must pass the allowlist in `SbomLicencePolicyTest`. See `docs/CONSTITUTION.md` for third-party, clean-room, PDK, model, and solver provenance requirements.
