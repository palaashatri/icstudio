# AGENTS.md — ICStudio

ICStudio is an MIT-licensed, local-first custom-IC design and signoff environment (long-term goal: a clean-room alternative to a Virtuoso-class flow). It is **pre-alpha**: there is a project/geometry kernel, a CLI/MCP surface and UI foundations, but no schematic editor, layout editor, simulator, DRC or LVS yet.

This file is the short instruction sheet for coding agents. The full product, solver, architecture, legal and milestone contract is [`docs/CONSTITUTION.md`](docs/CONSTITUTION.md). Machine-readable state lives in `.project/` (`truth.json`, `capabilities.json`, `workpacks/`, `decisions/`, `checkpoints/`).

## Current state (2026-10-20)

- **Stack migration in progress.** ADR-0001 (`.project/decisions/ADR-0001-java25-swing-pivot.toml`, accepted) makes **Java 25 + Swing** (Skia canvases, FFM for native code) the authoritative stack. The existing Rust crates and the Electron/React workbench are now a *conformance oracle only*: keep them building and passing, but add no new product capability there.
- Live truth score is **0/100** (`.project/truth.json`). The README's "8/100, M0 and M1 accepted" refers to the pre-pivot Rust checkpoint `CP-M1-KERNEL`.
- On `main`, Java has only `java/icstudio-core` (`ObjectId`) and `java/icstudio-ui` (Swing `Studio*` components). Open PR #4 (`implementation/java-project-kernel`) adds `icstudio-project` and `icstudio-command`; its CI is green.
- Next gates: **J0** (Java factory: wrapper, SBOM, licence evidence, cross-OS CI) → **J1** (Java reproduces the M1 kernel; checkpoint `CP-JAVA-M1-KERNEL`) → **J2** (delete the Rust/Electron app code). No M2 (schematic/simulation) work before J1 is accepted.

## Commands

Toolchains: Rust 1.85.1 (pinned in `rust-toolchain.toml`), JDK 25, Maven 3.9.x, Node 22, [`just`](https://github.com/casey/just).

Rust oracle and repository gates (all wired through `just`):

```bash
just bootstrap          # cargo fetch --locked
just ci                 # format-check, clippy -D warnings, build, tests, workbench build/test/licences, validate, licence check, MCP smoke
just test-fast          # cargo test --workspace --all-targets --locked
just lint               # cargo clippy --workspace --all-targets --locked -- -D warnings
just validate           # repository/project-state validation
just truth              # print the live truth score
just resume-check CP-M1-KERNEL
```

Java (the authoritative stack). There is **no Maven wrapper and no `just` recipe for Java yet**, even though the constitution mentions `mvnw` and `just test`/`just studio` (that is J0 work):

```bash
mvn -B -ntp -Denforcer.skip=true test                        # whole reactor
mvn -B -ntp -Denforcer.skip=true -pl java/icstudio-ui -am test # one module
```

`-Denforcer.skip=true` is needed unless you have exactly Maven 3.9.16; CI passes it too. The enforcer also requires JDK `[25,26)`.

Workbench (oracle only): `just ui-check` (or `node apps/workbench/scripts/run-npm.mjs ci|run build|test`).

## Repository layout

| Path | What it is | Status |
|---|---|---|
| `java/icstudio-core`, `java/icstudio-ui` | Java reactor modules (`pom.xml` at root) | Authoritative, growing |
| `crates/` | Rust: `project`, `geometry`, `netlist`, `pdk`, `results`, `rpc`, `formats`, `platform` (CLI `icstudio`, `icstudio-mcp`, worker, UI bridge) | Oracle — maintain, don't extend |
| `apps/workbench/` | Electron + React + WebGPU shell | Oracle — maintain, don't extend |
| `schemas/` | Language-neutral project/RPC/result/PDK/UI-snapshot schemas | Shared by both stacks |
| `.project/` | Truth, capabilities, workpacks, ADRs, checkpoints | Update with every accepted change |
| `docs/CONSTITUTION.md` | Full contract | Reference |

## Rules

1. **Clean room.** Never use Cadence/Synopsys/Siemens binaries, docs behind licence walls, leaked material, or proprietary PDK content. Public specs, papers and open-source tools with compatible licences only (constitution §3).
2. **No fake capability.** A solver, editor or format counts only when its acceptance tests pass. Scaffolds and interfaces are `planned`, not `implemented`. Don't raise `.project/truth.json` or capability statuses without the evidence the constitution asks for.
3. **One authoritative state.** CLI, UI and MCP must read and write the same project model through the same services; there is a test that checks CLI/UI/MCP equivalence.
4. **Deterministic and exact.** Integer geometry, stable 128-bit IDs, revision-checked transactions, deterministic serialization. Don't introduce floating-point geometry or non-deterministic ordering in persisted data.
5. Pin versions. Don't add dependencies without licence evidence (`just license-check`, `just ui-license-check`; Java SBOM is a J0 task).
6. Don't add planning or status Markdown files. Engineering state goes in `.project/` or the constitution.
7. `crates/platform/tests/java_architecture_contract.rs` checks that the constitution still names Java 25, Swing, Skia and FFM. If you change the stack decision, do it through a new ADR, not by editing the test.

## Git workflow

- Never push to `main`. Use a short-lived branch (`implementation/<area>`, `claude/<topic>`), open a PR and wait for CI.
- One workpack (`.project/workpacks/WP-xxxx.yaml`) per PR where possible; update its `status` and evidence in the same PR.
- In the PR description, list the commands you ran and their results.

## Definition of done for a change

- `just ci` passes, plus `mvn -B -ntp -Denforcer.skip=true test` if you touched Java.
- New behaviour has tests; Java changes have tests on Linux, macOS and Windows in CI.
- `.project/` reflects the change (workpack status, capability evidence) and nothing claims more than the tests show.

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
