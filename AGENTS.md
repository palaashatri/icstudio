# AGENTS.md — ICStudio

ICStudio is an MIT-licensed, local-first custom-IC design and signoff environment (long-term goal: a clean-room alternative to a Virtuoso-class flow). It is **pre-alpha**: there is a project and geometry kernel, a CLI and MCP surface, and a Swing workbench shell, but no schematic editor, layout editor, simulator, DRC or LVS yet.

This file is the short instruction sheet for coding agents. The full product, solver, architecture, legal and milestone contract is [`docs/CONSTITUTION.md`](docs/CONSTITUTION.md). Machine-readable state lives in `.project/` (`truth.json`, `capabilities.json`, `workpacks/`, `decisions/`, `checkpoints/`).

## Current state

- **One authoritative stack: Java 25 + Swing** (ADR-0001, `.project/decisions/ADR-0001-java25-swing-pivot.toml`). Skia (via Skija) renders engineering canvases; Java FFM is the native boundary.
- **Migration complete.** J0 is accepted at `CP-JAVA-M0-FACTORY` (2/100). J1 is accepted at `CP-JAVA-M1-KERNEL` (8/100). J2 removed the Rust crates and the Electron/React workbench. The behaviour recorded from the Rust M1 kernel survives as a conformance corpus under `java/icstudio-conformance/src/test/resources/rust-oracle/`.
- Live truth score: `just truth` (8/100, from `.project/truth.json`).
- M2 (schematic to reference simulation) is unblocked by J1 acceptance, but no M2 capability is implemented yet. Start M2 only through a workpack.

## Commands

Toolchains: JDK 25 (Temurin 25.0.4+7 in CI), the Maven Wrapper (pins Maven 3.9.16), and [`just`](https://github.com/casey/just). Use the wrapper, not a system Maven.

```bash
just bootstrap          # install reactor artifacts without running tests
just build              # package every module without tests
just test-fast          # unit and conformance tests
just verify             # full reactor verify, including the CycloneDX SBOM
just validate           # programme-state validation (CLI)
just truth              # print the live truth score
just capability-report  # write artifacts/capability-report.md
just sbom               # archive artifacts/icstudio.cdx.json
just license-check      # enforce the dependency licence allowlist against the SBOM
just checkpoint CP-...  # create an immutable checkpoint
just resume-check CP-...
just test-m1-geometry   # one-million-shape baseline
just mcp-smoke          # MCP transcript and CLI/MCP/Swing equivalence
just cli project show --path DIR   # headless CLI
just studio --project DIR          # Swing workbench (headless environments print the summary)
just ci                 # verify, validate, license-check, truth
```

Without `just`: `./mvnw -B -ntp verify` (Windows: `.\mvnw.cmd -B -ntp verify`). For one module: `./mvnw -B -ntp -pl java/icstudio-ui -am test`.

Linux needs `libegl1` and `libgl1` at runtime, because the Skia native library links against them.

## Repository layout

| Path | What it is |
|---|---|
| `java/icstudio-core` | `ObjectId`, `Revision`, text and ordering helpers that match the M1 oracle |
| `java/icstudio-project` | Project model, deterministic codec, store, and GDSII record framing |
| `java/icstudio-command` | Revision-checked transactions, write-ahead journal, crash recovery |
| `java/icstudio-geometry` | Exact integer rectangles and the reference spatial index |
| `java/icstudio-connectivity`, `icstudio-experiment` | Contract types only; no rules or solvers yet |
| `java/icstudio-pdk`, `icstudio-netlist-ir`, `icstudio-result-db` | PDK metadata, structural SPICE IR, exact-bit result store |
| `java/icstudio-platform` | JSON reader, OS identity, evidence services, SBOM licence policy |
| `java/icstudio-worker` | Versioned RPC envelopes and the isolated worker process |
| `java/icstudio-cli`, `icstudio-mcp` | Headless CLI and MCP gateway over the same services |
| `java/icstudio-ui`, `icstudio-app` | Studio design system, Skia scene, workbench, desktop entry |
| `java/icstudio-conformance` | Oracle replay, cross-surface equivalence, crash containment, architecture contract |
| `schemas/` | Language-neutral project, RPC, result, PDK and UI-snapshot contracts |
| `.project/` | Truth, capabilities, workpacks, ADRs, checkpoints |
| `docs/CONSTITUTION.md` | Full contract |

## Rules

1. **Clean room.** Never use Cadence, Synopsys or Siemens binaries, docs behind licence walls, leaked material, or proprietary PDK content. Public specs, papers, and open-source tools with compatible licences only (constitution §3).
2. **No fake capability.** A solver, editor or format counts only when its acceptance tests pass. Scaffolds and interfaces are `planned`, not `implemented`. Do not raise `.project/truth.json` or capability statuses without the evidence the constitution asks for.
3. **One authoritative state.** CLI, UI and MCP read and write the same project model through the same services. `SurfaceEquivalenceTest` checks that they agree.
4. **Deterministic and exact.** Integer geometry, stable 128-bit IDs, revision-checked transactions, deterministic serialization. Do not introduce floating-point geometry or non-deterministic ordering in persisted data.
5. **Pin versions.** Do not add a dependency without licence evidence. The SBOM licence gate (`SbomLicencePolicyTest`) must pass.
6. Do not add planning or status Markdown files. Engineering state goes in `.project/` or the constitution.
7. `java/icstudio-conformance/src/test/java/dev/icstudio/conformance/ArchitectureContractTest.java` checks that the constitution still names Java 25, Swing, Skia and FFM, and that the retired Rust/Electron stack is absent. Change the stack decision through a new ADR, never by editing that test.

## Git workflow

- Never push to `main`. Use a short-lived branch (`implementation/<area>`, `claude/<topic>`), open a PR and wait for CI.
- One workpack (`.project/workpacks/WP-xxxx.yaml`) per PR where possible. Update its `status` and evidence in the same PR.
- In the PR description, list the commands you ran and their results.

## Definition of done for a change

- `just verify` passes, plus `just validate` and `just license-check` for changes that touch state or dependencies.
- New behaviour has tests. Java changes run on Linux, macOS and Windows in CI.
- `.project/` reflects the change (workpack status, capability evidence). Nothing claims more than the tests show.
- Accepted checkpoints are immutable. Record new evidence in a new checkpoint, not by editing an existing one's hashed state.

## Where to read more in `docs/CONSTITUTION.md`

| Topic | Section |
|---|---|
| Scope, year-one tiers, what "parity" means | §1–2 |
| Legal and clean-room procedure | §3 |
| Architecture, process isolation, MCP boundary | §5 |
| Language/toolchain policy, ADR-0001 gates | §7 |
| Data model, transactions, files | §8 |
| PDKs | §9 |
| Solvers (SPICE, DRC, LVS, PEX, EM, …) | §10 |
| Workbench UX, automation/MCP | §11–12 |

## Future SLOPOS/PARAMA targets and edge-efficiency requirements

**FUTURE TARGET ONLY / NOT VERIFIED.** icstudio is an independently authored candidate for **optional original SLOPOS electronic-design/EDA workbench**. Preserve Java 25 Swing workbench with Skia and native computational service plans, existing docs/CONSTITUTION.md authority and the current project's authoritative engineering contract, milestones, license and cross-platform requirements. The project is *not* a currently shipped SLOPOS/PARAMA app by virtue of being listed. Any optional first-party distribution requires a source/asset/provenance audit, real packaging, signed/verifiable builds, supported platform test matrix and no eager runtime/SDK load in the 512 MB core SLOPOS installation.

- **Linux/SLOPOS target:** Linux/SLOPOS native launcher and xdg associations for project/GDS/netlist files, keyboard menus, clipboard and file pickers, GPU/CPU renderer fallback, dialogs that report actual solver state, legible high/low DPI, theme tokens only where Swing permits, and sandboxed simulator/process authority.
- **Pi Zero 2 W (4× Cortex-A53, 512 MB shared RAM) efficiency:** Lazy hierarchical schematic/layout tile rendering, visible-region clipping/damage only, capped vector/path caches, level-of-detail at low zoom, incremental PDK indexing, bounded netlist import and background DRC/simulation jobs. Provide progressive previews and cancellation, no full-chip solver on UI thread, no resident analysis toolchain when idle. Test tiny real schematics rather than claim Virtuoso-class signoff fits 512 MB.
- **Future native PARAMA/SURYA target:** Future PARAMA native port requires JDK 25 with Swing/AWT, Skia graphics/backend and native libraries, filesystem/PDK/toolchains, process isolation and SURYA; GUI might be deferred while headless solver libraries port first. Explicitly no present native port or copied Linux binary compatibility.
- **Evidence and error handling:** Pi Zero 2 W run tiny PDK/schematic fixtures only when JDK/Skia actually support the device. Measure memory on 512 MB incl runtime/native caches, pan/zoom input p95 at 720p, background DRC cancel/restart, correctness of exported netlists and explicit oversized-design refusal.
- **Methodology:** Pin actual OS/ABI/runtime/GC/driver, reference input fixtures and revision; measure cold/warm time-to-usable, process-tree PSS/RSS, whole-system `MemAvailable`, steady idle CPU, local GUI response p95 if applicable, compute throughput and correctness independently, timeout/cancellation, memory-pressure failure and restart. Cross-build/virtualization is NOT proof of real Pi hardware or future PARAMA runtime. Decline or gate unsupported heavy operations before OOM; allow long computations but never permit them to starve critical interactive tasks. Keep external I/O delays distinct from app-induced UI stalls.
