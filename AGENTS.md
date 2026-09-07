# AGENTS.md — ICStudio Open Custom-IC Design and Signoff Platform

> **Status:** Authoritative engineering constitution  
> **Project:** ICStudio  
> **Programme:** Twelve-month massively parallel parity assault, followed by capability-driven continuation  
> **Audience:** Human maintainers, Luna subagents, external contributors, reviewers, release engineers, and research partners  
> **Authority:** This file is the single source of truth for product scope, architecture, milestones, agent behaviour, validation, and pause/resume procedure.

---

## 0. How to use this file

This repository is intended to support hundreds of parallel work packages without dissolving into incompatible prototypes. Every human and automated agent must read this file before changing code.

The words **MUST**, **MUST NOT**, **REQUIRED**, **SHALL**, **SHALL NOT**, **SHOULD**, **SHOULD NOT**, and **MAY** are normative.

When code and this file disagree, either:

1. the code is wrong and must be changed; or
2. this file must be amended through an architecture decision before the conflicting code is merged.

No agent may silently reinterpret a requirement.

This project may pause for days, months, or years. It may also gain a large open-source contributor base. Therefore all progress is recorded as machine-readable capability state, reproducible artifacts, tests, and accepted decisions—not as conversational context.

---

# 1. Mission

Build **ICStudio**, an MIT-licensed, open, local-first, cross-platform custom-IC design and signoff environment that independently implements the solver classes and professional workflows required by a Cadence Virtuoso Studio-class flow.

ICStudio is also an **AI-native engineering platform**. Every supported design, analysis, verification, and automation operation must be available through stable headless APIs and a first-class Model Context Protocol (MCP) server so users can build circuits and physical designs with any compatible LLM host while retaining explicit control over data and mutations.

The platform must eventually allow a designer to complete:

```text
PDK installation
→ library/cell/view creation
→ schematic capture
→ symbol and testbench creation
→ analog, RF, digital, and mixed-signal simulation
→ experiment orchestration, corners, optimization, Monte Carlo, and yield
→ custom layout and parameterized-cell generation
→ DRC, ERC, LVS, and antenna verification
→ parasitic extraction
→ post-layout simulation
→ electromagnetic, power-integrity, thermal, and reliability analysis
→ GDSII/OASIS release and tapeout evidence
```

The project is not a themed frontend around unrelated command-line tools. External engines may be used during bootstrap and as independent test oracles, but the declared end state is that every core solver class is available as a project-owned implementation.

The project is also not a pixel-identical clone of any commercial product. We reproduce capabilities and interoperable workflows using clean-room engineering, public standards, published research, open PDKs, and independently created tests.

---

# 2. Scope boundary

## 2.1 Meaning of “every Cadence solver” in this programme

The year-one programme targets every solver class directly required by, launched from, or tightly coupled to a professional **custom-IC/Virtuoso-style workflow**:

1. transistor-level SPICE simulation;
2. accelerated/FastSPICE simulation;
3. RF periodic steady-state and frequency-conversion simulation;
4. event-driven digital simulation sufficient for AMS co-simulation;
5. analog/mixed-signal co-simulation;
6. design-rule checking;
7. electrical-rule checking and antenna checking;
8. layout-versus-schematic comparison;
9. parasitic RC/RLC extraction;
10. planar and three-dimensional electromagnetic solving;
11. power-grid voltage-drop and electromigration analysis;
12. thermal analysis and electrothermal co-simulation;
13. device reliability, aging, and safe-operating-area analysis;
14. photonic compact-model, eigenmode, and electromagnetic workflows;
15. optimization, sensitivity, statistical variation, Monte Carlo, and yield analysis.

The programme does **not** claim year-one parity with every product in the full Cadence corporate catalogue. PCB authoring, mechanical CFD, full digital place-and-route, package board design, and unrelated system-analysis products are out of the mandatory year-one boundary unless a custom-IC workflow requires a narrow interoperability feature.

## 2.2 Year-one success tiers

Success is reported honestly using four tiers:

- **Tier 0 — Scaffold:** API and test harness exist; no correctness claim.
- **Tier 1 — Reference:** independent implementation is correct on canonical and small public benchmarks.
- **Tier 2 — Practical:** implementation handles useful open-PDK designs with documented limits.
- **Tier 3 — Competitive:** accuracy, robustness, and performance approach established commercial workflows on the supported scope.

The twelve-month objective is:

- Tier 3 for the integrated workbench, schematic, layout, experiment, waveform, and open-PDK workflow;
- Tier 2 or better for SPICE, DRC, LVS, RC extraction, and basic mixed-signal;
- Tier 1 or better for FastSPICE, advanced RF, 3D EM, EM/IR, thermal, reliability, and photonics;
- a clear, tested path from each Tier 1 engine to Tier 2 and Tier 3.

No release may use “full parity” without publishing the capability matrix and benchmark evidence described in this file.

---

# 3. Legal, ethical, and clean-room rules

## 3.1 Prohibited inputs

Contributors and agents MUST NOT use:

- proprietary Cadence source code;
- decompiled or disassembled commercial binaries;
- leaked code, rule decks, PDKs, models, documentation, or training data;
- confidential screenshots, internal APIs, private support cases, or files obtained under NDA;
- proprietary test suites copied from commercial installations;
- behaviour obtained by violating licence terms, access controls, or applicable law;
- trademarks or product branding in a way that suggests affiliation or endorsement.

## 3.2 Allowed inputs

The implementation MAY use:

- public standards and specifications;
- peer-reviewed papers, books, theses, and patents where legally usable;
- public product descriptions solely to identify broad capabilities;
- open-source implementations under compatible licences;
- open PDKs and openly licensed models;
- public benchmark suites;
- independently created circuits, layouts, meshes, and expected results;
- analytical solutions and manufactured-solution tests;
- black-box comparison against tools that legally permit such use.

## 3.3 Clean-room compatibility procedure

When implementing compatibility with a proprietary file format, scripting API, or observed behaviour:

1. a **specification agent** documents only legally obtained public behaviour and creates neutral tests;
2. an **implementation agent** that has not consumed prohibited material implements from that neutral specification;
3. an **independent validation agent** runs the tests and records provenance;
4. the pull request includes a `clean_room` section in its work-package manifest.

Any contributor with possible contamination must disclose it. Maintainers may isolate that contributor from the affected implementation track.

## 3.4 Project licence

ICStudio original source code, schemas, examples, documentation, and project-owned solver implementations are licensed under the **MIT License** unless a file is explicitly and lawfully marked otherwise.

Required repository policy:

- the root `LICENSE` file contains the canonical MIT License text;
- package manifests use the SPDX expression `MIT` for project-owned components;
- contributions are accepted under the same MIT terms under an inbound-equals-outbound policy;
- contributors sign off commits using the Developer Certificate of Origin unless maintainers adopt a different public contribution mechanism through an ADR;
- no contributor licence agreement is required by default;
- copyright notices must not imply ownership of third-party code;
- generated files identify their generator and source licence where relevant;
- public releases include an SBOM and third-party notices.

MIT licensing is a distribution policy, not a waiver of clean-room, export-control, patent, trademark, PDK, model, or foundry obligations. Patent-sensitive solver work must record public technical provenance and may require independent legal review before a commercial claim or tapeout qualification.

**ICStudio** is the working and repository name. Before a public 1.0 launch, maintainers must complete trademark and package-name clearance. A rename, if legally necessary, must be performed through one migration ADR and preserve CLI aliases for at least one major release.

## 3.5 Third-party licences

- Permissively licensed dependencies compatible with MIT are preferred.
- GPL tools MAY be invoked as separate processes through documented adapters.
- GPL or AGPL code MUST NOT be copied, statically linked, or incorporated into MIT components unless legal review explicitly approves the resulting distribution and the affected component is isolated and clearly licensed.
- LGPL and other reciprocal dependencies require a documented linkage and redistribution analysis.
- Optional OpenAccess, commercial-tool, foundry, and proprietary-format adapters must be separately buildable and must not prevent distribution of the MIT core.
- Every dependency requires a machine-readable software-bill-of-materials entry and licence provenance.
- No dependency may be added merely to save a small amount of implementation effort if it compromises portability, reproducibility, clean-room independence, or licensing.

---

# 4. Product principles

1. **Correctness before feature count.** A wrong simulator is worse than no simulator.
2. **Independent engines, integrated experience.** Solver boundaries are explicit; the user experience is unified.
3. **Local first.** Designs, PDKs, and results remain on the user’s machine unless the user deliberately dispatches work elsewhere.
4. **Headless first, graphical second.** Every meaningful workflow must be reproducible through a stable CLI/API before relying on UI gestures.
5. **Determinism by default.** Randomized algorithms require explicit seeds and provenance.
6. **No hidden state.** A project can be rebuilt from versioned source files, PDK references, tool versions, and run manifests.
7. **Open formats where possible.** Proprietary interoperability is implemented through isolated import/export adapters.
8. **Parallelism without fragmentation.** Agents work independently only behind frozen contracts.
9. **Reference implementation before acceleration.** Every optimized kernel must be checked against a simple trusted implementation.
10. **Evidence-based parity.** Claims come from public benchmarks, tolerances, and reproducible reports.
11. **Pause-safe engineering.** Every accepted milestone leaves a complete restart point.
12. **No throwaway prototypes on main.** Experimental code lives behind feature flags or in `research/` until promoted through a gate.
13. **AI-native, never AI-dependent.** MCP and LLM workflows expose the same deterministic commands available to humans; no core capability requires a model or network service.
14. **Human authority over mutations.** AI-requested writes, expensive jobs, exports, and external data transfers are permissioned, reviewable, cancellable, and auditable.

---

# 5. High-level architecture

ICStudio is a **Java 25 application end to end**. Swing is the desktop substrate; the UI and engineering core share one authoritative in-process Java object model. Module boundaries exist for maintainability, testing, and deliberate fault containment, not to recreate a frontend/backend web architecture.

```text
┌──────────────────────────────────────────────────────────────────────────────┐
│ ICStudio Desktop, Headless, and AI Surfaces                                 │
│ Swing + Studio* design system | Skija/Skia canvases | CLI | SDK | MCP      │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │ typed in-process Java query/command APIs
┌───────────────────────────────▼──────────────────────────────────────────────┐
│ Authoritative Java 25 Domain Services                                       │
│ project/session | commands/transactions | geometry/connectivity | PDK        │
│ netlist/result IR | experiments | jobs | provenance | permission policy      │
└───────────────┬──────────────────────┬───────────────────────┬───────────────┘
                │                      │                       │
┌───────────────▼────────────┐ ┌───────▼──────────────┐ ┌──────▼──────────────┐
│ Design/Geometry Domain     │ │ Simulation IR        │ │ Verification IR      │
│ Java 25                    │ │ Java 25              │ │ Java 25              │
│ DBU, hierarchy, topology,  │ │ devices, equations,  │ │ layers, rules, nets, │
│ spatial index, formats     │ │ analyses, results    │ │ markers, extraction  │
└───────────────┬────────────┘ └────────┬─────────────┘ └────────┬─────────────┘
                │                       │                         │
┌───────────────▼───────────────────────▼─────────────────────────▼────────────┐
│ Isolated Java Worker JVMs                                                   │
│ SPICE | FastSPICE | RF | Digital | AMS | DRC | LVS | PEX | EM | EMIR |      │
│ Thermal | Reliability | Photonics | Optimization | plugins | hostile import │
└───────────────────────────────┬──────────────────────────────────────────────┘
                                │ Java FFM / versioned worker protocols
┌───────────────────────────────▼──────────────────────────────────────────────┐
│ Mature Native Libraries, PDKs, Models, Standards, and External Oracles      │
│ permissive C/C++ libraries through FFM | separate-process reciprocal tools   │
└──────────────────────────────────────────────────────────────────────────────┘
```

## 5.1 Process isolation

One language does not mean one process. Simulation, DRC, LVS, PEX, EM, thermal, reliability, photonics, optimization, untrusted PDK execution, plugins, and hostile file import may run in isolated Java worker JVMs. A crash, malformed model, runaway job, or malicious extension must not corrupt authoritative project state or terminate the workbench.

## 5.2 Authoritative state

Java domain services own authoritative project state. Swing components, CLI commands, SDK calls, and MCP adapters consume the same typed Java query and command interfaces. UI objects may own presentation state such as selection, zoom, window placement, and transient editing affordances, but they must never become the only copy of engineering state.

Ordinary desktop interaction is in-process. ICStudio MUST NOT serialize state through JSON, HTTP, REST, subprocess IPC, or a local web server merely to move data between its UI and engineering core.

## 5.3 Communication and native interoperability

- In-process desktop control uses typed Java interfaces and immutable value objects.
- Worker-process control uses versioned schemas carrying project ID, expected revision, request/idempotency identity, actor, tool version, cancellation state, and structured diagnostics.
- Large geometry, matrix, mesh, and waveform payloads use memory-mapped files, shared memory where safe, or chunked binary streams rather than oversized JSON.
- The **Foreign Function and Memory API (FFM)** is the preferred Java/native boundary for mature permissively licensed C/C++ numerical, geometry, and interoperability libraries.
- JNI is fallback-only for new project-owned bindings and requires an architecture decision explaining why FFM is insufficient.
- GPL/reciprocal tools remain external processes or differential-test oracles unless an explicit legal and architectural review approves another relationship.

## 5.4 MCP architectural boundary

The ICStudio MCP server is implemented in Java and delegates to the same query/command services used by Swing and CLI. It may run as a separate least-privilege process for host integration, but it MUST NOT duplicate engineering semantics or independently parse/modify authoritative project files.

- Local integrations use `stdio` by default.
- Remote integrations use Streamable HTTP only when explicitly enabled.
- Protocol revision negotiation is mandatory. The initial locked baseline remains MCP `2025-11-25` until changed through conformance tests and an ADR.
- MCP tools invoke typed commands; resources expose bounded, redacted, revision-addressed context; prompts package recommended workflows.
- Equivalent desktop, CLI, SDK, and MCP operations must resolve to equivalent command manifests and project revisions.

## 5.5 Workbench and product-design requirements

The desktop experience is professional, comfortable, and informed by Apple Human Interface Guidelines without pixel-cloning an Apple product or violating platform conventions.

- **Approachable by default, powerful on demand.** Advanced parameters use progressive disclosure rather than permanent visual noise.
- **One product, not beginner/expert modes.** Power emerges through expansion, customization, commands, shortcuts, scripting, and saved workspaces.
- **Studio-owned components.** Application screens use reusable `Studio*` controls and semantic design tokens; raw look-and-feel keys do not become screen-level APIs.
- **Flexible workspaces.** Dock, split, tab, float, collapse, resize, save, restore, and keyboard navigation are first-class.
- **Engineering surfaces use Skija/Skia.** Schematic, layout, waveform, mesh, and field canvases are custom rendering surfaces rather than enormous Swing component trees.
- **Semantic materials.** Themes request roles such as solid, sidebar, toolbar, inspector, popover, menu, HUD, and canvas. macOS may map these to AppKit visual-effect materials through FFM; Windows may map them to supported composition materials; Linux uses compositor-aware blur/translucency when available with deterministic fallbacks.
- **Legibility wins over glass.** Schematic, layout, waveform, console, and dense-data surfaces default to controlled solid backgrounds even when surrounding chrome uses transparency or blur.
- **Accessibility is mandatory.** Respect platform reduce-motion/reduce-transparency preferences where available, provide a fully opaque high-contrast path, preserve visible keyboard focus, and maintain usable keyboard-only workflows.

---

# 6. Repository layout

```text
/
├── AGENTS.md
├── README.md
├── LICENSE
├── SECURITY.md
├── pom.xml                        # Java 25 Maven reactor
├── mvnw / mvnw.cmd               # Maven 3.9.16 wrapper
├── .mvn/wrapper/
├── justfile                      # Canonical human/agent entry points
├── toolchains/                   # Locked protocol/toolchain manifests
├── schemas/                      # Language-neutral project/RPC/result schemas
├── java/
│   ├── icstudio-core/            # IDs, revisions, errors, immutable values
│   ├── icstudio-project/         # project/library/cell/view and persistence
│   ├── icstudio-command/         # transactions, history, journaling, recovery
│   ├── icstudio-geometry/        # exact integer geometry and indexing
│   ├── icstudio-connectivity/    # electrical/physical connectivity
│   ├── icstudio-pdk/             # PDK runtime/package model
│   ├── icstudio-netlist-ir/      # simulator-neutral netlist IR
│   ├── icstudio-result-db/       # waveform/result storage
│   ├── icstudio-experiment/      # ADE-style orchestration
│   ├── icstudio-platform/        # platform integration and FFM adapters
│   ├── icstudio-worker/          # isolated worker protocol/runtime
│   ├── icstudio-cli/             # stable headless CLI
│   ├── icstudio-mcp/             # MCP gateway over shared services
│   ├── icstudio-ui/              # Swing Studio* design system and Skia hosts
│   ├── icstudio-app/             # desktop application assembly/entry point
│   └── icstudio-conformance/     # differential and cross-surface tests
├── native/
│   ├── numeric/                  # optional project-owned native kernels/adapters
│   ├── geometry/
│   └── openaccess-adapter/       # optional; separately built/licensed
├── engines/                      # solver modules/workers by capability
├── adapters/                     # external/open-tool interoperability
├── formats/                      # public format fixtures/specification material
├── pdk/                          # SDK, examples, validators
├── benchmarks/
├── corpus/
├── research/
├── examples/
├── tests/
├── ops/
└── .project/
    ├── capabilities.json
    ├── milestones/
    ├── workpacks/
    ├── decisions/
    ├── baselines/
    └── checkpoints/
```

During ADR-0001 migration, the existing Rust crates and Electron/React workbench remain temporarily in their current paths as a **historical conformance oracle only**. They receive no new product capability work and are removed only after `CP-JAVA-M1-KERNEL` is accepted. Language-neutral schemas, test corpora, and historical checkpoints are retained.

The project MUST NOT accumulate miscellaneous planning Markdown files. Engineering state belongs in this file or in structured files under `.project/`.

---

# 7. Language and toolchain policy

## 7.1 Languages

- **Java 25:** authoritative language for the desktop, project model, commands, geometry, connectivity, PDK runtime, formats, orchestration, CLI, MCP, solver orchestration, and project-owned reference solvers.
- **C/C++23:** permitted behind Java FFM for mature ecosystem integration or kernels with demonstrated technical need. New native project-owned code requires a Java reference or conformance path where practical.
- **Python 3.12+:** research, model generation, golden references, corpus generation, teaching, and user automation; never authoritative project state.
- **CUDA/HIP and other accelerator languages:** optional acceleration behind tested CPU/reference behaviour.
- **WASM:** preferred portable sandbox target where deterministic PCells/plugins benefit from it.
- **Rust and TypeScript:** migration-only legacy implementation languages under ADR-0001. No new product capability is implemented in the old Rust/Electron stack after the architecture switch.

## 7.2 Java and build baseline

The migration baseline is:

- Eclipse Temurin **25.0.4+7** for CI/reference JDK distribution;
- Java language/API target **25**;
- Apache Maven **3.9.16** through the Maven Wrapper;
- JUnit **6.1.2** for Java tests;
- FlatLaf **3.7.2** as optional Swing look-and-feel plumbing, never as the product design system;
- Skija/Skia **0.143.17** for engineering-canvas rendering;
- CycloneDX Maven Plugin **2.9.3** for Java dependency SBOM evidence.

Maven 4 preview/RC builds are not production build dependencies until a later ADR adopts a GA release.

Canonical developer entry points remain exposed through `just` so humans and agents do not need to know the underlying build topology:

```bash
just bootstrap
just build
just test
just test-fast
just test-capability CAP-SIM-DC
just bench
just studio
just checkpoint
just resume-check
```

Internally:

- Maven Wrapper manages the authoritative Java reactor.
- CMake + Ninja manage optional project-owned C/C++ code.
- `uv` manages Python research/automation environments.
- OCI containers provide reproducible CI images.
- Java dependency resolution is locked by explicit versions, repository policy, SBOM, licence evidence, and reproducibility checks.
- `just` may invoke the legacy Cargo/npm gates only while they remain required as migration-oracle evidence.

Agents MUST call canonical `just` targets rather than inventing undocumented build commands.

## 7.3 Supported platforms

Production desktop targets are:

- Windows 11 x86-64; ARM64 becomes mandatory when the chosen JDK/native dependency set is supportable without emulation-only product claims;
- macOS current and previous major release, Apple Silicon first, Intel while dependencies remain supportable;
- Linux x86-64, with Ubuntu LTS and Fedora as reference distributions.

Headless workers support Linux first when a capability requires platform-specific acceleration, but ordinary desktop use MUST NOT require a Linux VM.

## 7.4 ADR-0001 migration gate

The accepted Rust/Electron `CP-M1-KERNEL` checkpoint is historical evidence, not current implementation credit after this architecture switch.

Migration proceeds in three gates:

1. **J0 — Java factory:** Java 25/Maven builds, tests, licence/SBOM evidence, checkpoints, and Windows/macOS/Linux/container CI are accepted. Live truth becomes **2/100**.
2. **J1 — Java M1 parity:** Java reproduces the M1 project, recovery, geometry, worker, PDK/netlist/result, CLI/MCP, Swing shell, Skia scene, and cross-surface equivalence contracts. Live truth becomes **8/100** and `CP-JAVA-M1-KERNEL` is created.
3. **J2 — Legacy retirement:** old production Rust/Electron/Node application code is removed after J1 evidence is archived. Removing superseded code does not itself increase truth score.

Immediately after this architecture switch, live truth is **0/100**. The previous 8/100 remains preserved in `CP-M1-KERNEL` and historical evidence. No M2 schematic/simulation implementation may begin before J1 is accepted.

---

# 8. Core data model

## 8.1 Library/cell/view hierarchy

Every design object lives under:

```text
workspace / library / cell / view / revision
```

Standard view types include:

- schematic;
- symbol;
- layout;
- extracted;
- testbench;
- behavioural;
- configuration;
- abstract;
- model;
- results;
- verification.

View types are extensible through schemas, not hard-coded UI assumptions.

## 8.2 Coordinates

Physical geometry uses signed 64-bit integer database units. Floating-point world coordinates are forbidden in authoritative layout storage.

Each technology defines database units per micron and manufacturing grid. Conversion must detect overflow, rounding, and off-grid data.

## 8.3 Stable identity

Every persistent object receives a stable 128-bit ID. IDs survive save/load, hierarchy traversal, undo/redo, and collaboration. Array members and generated PCell shapes also have reproducible derived IDs.

## 8.4 Transactions

All edits are commands inside ACID-like project transactions:

- validate preconditions;
- apply atomically;
- produce inverse information or persistent history;
- increment project revision;
- emit change events;
- journal before acknowledgement.

Crash recovery replays only complete transactions.

## 8.5 File strategy

The native project format is:

- schema-versioned;
- deterministic;
- content-addressed for large immutable blobs;
- textual for reviewable metadata and small schematics;
- chunked binary for geometry, meshes, matrices, and waveforms;
- independently documented;
- forward-readable when possible;
- migrated only through tested converters.

Git must produce meaningful diffs for project metadata and moderate schematics. Large generated results are excluded or stored through content-addressed artifact management.

## 8.6 OpenAccess

OpenAccess support is an optional interoperability adapter, not the authoritative internal database. Coalition access and licensing constraints must not prevent building or using the core platform.

---

# 9. PDK architecture

A PDK package is a signed, versioned bundle containing declarative metadata and optional sandboxed code.

## 9.1 Required PDK content model

- technology identity and semantic version;
- units and manufacturing grid;
- layer/purpose map;
- display rules;
- connectivity and conductor/via definitions;
- schematic symbols and CDF-like parameter schemas;
- compact models and simulation corners;
- PCells and parameter constraints;
- DRC/ERC/antenna rules;
- device-recognition and LVS rules;
- extraction stack and material properties;
- EM, thermal, reliability, and statistical data where available;
- fill and density rules;
- documentation and examples;
- provenance and licence metadata.

## 9.2 PCell requirements

PCell generation MUST be:

- deterministic;
- pure with respect to declared inputs;
- bounded by time and memory quotas;
- reproducibly cached;
- able to emit geometry, pins, connectivity, properties, and handles;
- validated against manufacturing grid and technology rules;
- callable headlessly.

Preferred implementation order:

1. declarative PCell DSL;
2. sandboxed WASM API;
3. Python adapter for trusted local development;
4. limited clean-room SKILL compatibility where legally justified.

## 9.3 Initial supported PDKs

The first qualification targets are open, redistributable technologies such as SKY130 and GF180MCU. PDK-specific code must remain outside generic engines.

A PDK is “qualified” only when its published qualification suite passes schematic, simulation, layout, DRC, LVS, extraction, and post-layout tests.

---

# 10. Solver architecture and requirements

Every solver follows the same lifecycle:

1. parse or receive typed IR;
2. validate and normalize;
3. produce a deterministic run plan;
4. execute in an isolated worker;
5. stream structured progress and diagnostics;
6. write versioned result artifacts;
7. emit provenance, tolerances, warnings, and convergence evidence;
8. support cancellation and resumable checkpoints where mathematically valid.

Every optimized solver contains or references a simpler validation implementation.

---

## 10.1 Transistor-level SPICE engine (`engines/spice`)

### Required analyses

- operating point;
- DC sweep;
- AC small signal;
- transient;
- noise;
- transfer function;
- pole-zero;
- sensitivity;
- distortion where practical;
- temperature sweep;
- parameter sweep;
- Monte Carlo and mismatch hooks.

### Numerical core

- modified nodal analysis and extensible equation stamping;
- sparse matrix assembly;
- direct sparse LU reference solver;
- iterative options where suitable;
- Newton-Raphson with line search/damping;
- GMIN stepping;
- source stepping;
- pseudo-transient continuation;
- robust singularity and topology diagnostics;
- adaptive timestep control;
- trapezoidal, backward-Euler, and variable-order BDF/Gear integration;
- local truncation error estimation;
- breakpoint and discontinuity handling;
- deterministic convergence policy.

### Device model API

The API must support charge-conserving, temperature-aware, noise-aware compact models with analytic or automatic derivatives.

Initial models:

- R, L, C, mutual inductance;
- independent and controlled sources;
- switches and transmission lines;
- diode;
- BJT;
- JFET;
- MOS level-1/3 for bring-up;
- BSIM4;
- BSIM-CMG;
- PSP or another modern bulk model;
- EKV family;
- behavioural sources.

Compact models must be separate versioned modules with dedicated conformance suites.

### Verilog-A

Build an independent Verilog-A frontend and compiler path:

```text
source → parser → semantic model → analog operator validation
→ symbolic/automatic differentiation → device kernel IR → native/JIT module
```

An open compiler may be used as a bootstrap oracle, but native parity requires the project-owned frontend and runtime.

### SPICE Tier-2 exit criteria

- all analytic RLC and controlled-source benchmarks pass;
- canonical diode/BJT/MOS circuits converge without special manual options;
- DC/AC results meet declared error budgets against high-precision references;
- transient integration passes stiff and discontinuous benchmark suites;
- at least 95% of the public regression corpus completes without crash;
- results are reproducible across repeated runs on the same architecture;
- diagnostics identify floating nodes, contradictory sources, and convergence failure causes.

---

## 10.2 FastSPICE engine (`engines/fastspice`)

FastSPICE is not implemented by adding threads to the reference SPICE engine.

Required research tracks:

- circuit partitioning and weak-coupling detection;
- multirate integration;
- event-driven analog scheduling;
- table/model compaction;
- repeated-instance and memory-array exploitation;
- nonlinear model-order reduction;
- waveform relaxation;
- incremental matrix updates;
- GPU batch device evaluation;
- accuracy-control modes;
- hierarchical parasitic reduction.

The engine exposes explicit accuracy presets and reports every approximation.

Tier-2 target:

- 10× geometric-mean speedup over the project reference simulator on selected SRAM, memory-periphery, and post-layout benchmarks;
- user-selected voltage/current error envelopes respected at observed nodes;
- no silent topology or device-model substitutions.

---

## 10.3 RF solver (`engines/rf`)

Required analyses:

- S/Y/Z parameter analysis;
- periodic steady state using shooting Newton;
- harmonic balance;
- periodic AC;
- periodic transfer;
- periodic noise;
- quasi-periodic steady state;
- envelope following;
- oscillator autonomous PSS;
- phase noise;
- large-signal stability and conversion-gain measures;
- n-port and Touchstone import/export.

Numerical requirements:

- frequency-domain sparse block systems;
- FFT-aware Jacobian operations;
- continuation in drive amplitude/frequency;
- autonomous oscillator phase condition;
- Krylov acceleration for large harmonic systems;
- adaptive harmonic count and time-grid control.

Tier-1 gate:

- mixers, ring oscillators, LC oscillators, LNAs, and switched-capacitor public benchmarks;
- agreement with transient-derived references within published tolerances;
- conservation and symmetry checks for passive n-port networks.

---

## 10.4 Digital event simulator (`engines/digital`)

The digital engine exists primarily to support mixed-signal workflows but must be independently useful.

Required semantics:

- four-state logic;
- event queues and delta cycles;
- inertial and transport delays where applicable;
- Verilog and progressively expanding SystemVerilog;
- continuous assignments, procedural blocks, tasks/functions, interfaces, packages, assertions subset;
- SDF annotation;
- VCD/FST-style waveform output;
- DPI and VPI-compatible extension boundary;
- deterministic multithreaded scheduling where semantics permit.

Do not copy a commercial simulator’s undocumented scheduling quirks. Implement documented language standards and publish deviations.

Tier-2 gate:

- language conformance corpus;
- gate-level timing examples with SDF;
- mixed analog/digital handshake tests;
- deterministic repeated execution;
- race diagnostics and event-queue observability.

---

## 10.5 AMS co-simulation (`engines/ams`)

Required concepts:

- analog and digital domain partitioning;
- disciplines and natures;
- connect modules;
- real-number modelling;
- threshold and hysteresis bridges;
- conservative and signal-flow semantics;
- synchronized rollback-safe time advancement;
- event localization;
- analog breakpoints caused by digital events;
- digital events caused by analog crossings;
- checkpoint/restart across both kernels.

The first production implementation may coordinate the project-owned SPICE and digital engines as separate workers. A later optimization may share scheduling infrastructure.

Tier-1 gate:

- ADC, DAC, PLL-control, power-on-reset, switched-mode supply control, and mixed-signal sensor examples;
- no missed threshold crossings within configured tolerances;
- deterministic synchronization and reproducible event ordering.

---

## 10.6 DRC engine (`engines/drc`)

Required geometry operations:

- width, spacing, enclosure, extension, overlap, area, notch, density;
- Boolean layer operations;
- edge selection and edge-to-edge relations;
- angle and orientation constraints;
- neighbourhood and context-sensitive rules;
- antenna accumulation;
- pattern matching;
- multi-pattern colouring constraints;
- hierarchical and flat modes;
- incremental local checking;
- tiling with halo management;
- waiver and marker databases.

Required implementation components:

- robust integer polygon kernel;
- sweep-line and spatial-index algorithms;
- exact predicates;
- deterministic polygon canonicalization;
- hierarchy-aware shape iterators;
- rule-plan compiler;
- parallel tile scheduler;
- marker deduplication and stable IDs.

The native rule language must be declarative, versioned, testable, and translatable from common open rule-deck concepts without copying proprietary syntax.

Tier-2 gate:

- all PDK qualification-cell expected markers reproduced;
- no false negatives in the accepted open-PDK corpus;
- false-positive rate measured and published;
- deterministic results independent of worker count;
- million-shape layouts processed without unbounded memory growth.

---

## 10.7 ERC and antenna (`engines/erc`)

Required checks:

- unconnected and multiply driven nets;
- power-domain and voltage-compatibility checks;
- device terminal legality;
- well/substrate connectivity;
- latch-up-related topology rules where expressible;
- gate-oxide voltage and safe-operating limits;
- antenna ratios across process steps;
- user-defined graph and property predicates.

ERC consumes both schematic and extracted-layout graphs and reports cross-probable evidence.

---

## 10.8 LVS engine (`engines/lvs`)

Pipeline:

```text
layout geometry
→ conductor connectivity
→ device recognition
→ extracted device/net graph
→ canonicalization and reductions
→ hierarchical graph matching
→ property comparison
→ explainable mismatch report
```

Required capabilities:

- hierarchical and flat comparison;
- device recognition rules;
- series/parallel reduction;
- symmetric-pin handling;
- parameter tolerances;
- black boxes and equivalence classes;
- permutation-aware matching;
- source/drain swapping where legal;
- global and inherited nets;
- shorts/opens/device mismatch diagnosis;
- schematic-layout cross-probing.

Tier-2 gate:

- open-PDK qualification cells;
- generated mutation corpus containing known opens, shorts, swaps, missing devices, and parameter errors;
- every injected mutation either correctly classified or listed as an explicit unsupported case;
- mismatch reports point to minimal useful evidence rather than only saying “not equivalent.”

---

## 10.9 Parasitic extraction (`engines/pex`)

The extraction programme has progressive engines:

1. rule-based lumped RC extraction;
2. pattern-matched and analytical 2D extraction;
3. 2.5D boundary-element field extraction;
4. 3D field-solver-backed RLC extraction;
5. model-order reduction and multi-corner extraction.

Required outputs:

- SPEF-like graph;
- DSPF/SPICE extracted netlist;
- hierarchical extracted view;
- coupling-capacitance matrix;
- frequency-dependent R/L models where supported;
- reduction/error report;
- cross-probing back to shapes.

Correctness strategy:

- canonical parallel plate, fringe, wire-over-plane, coupled-line, via, and interconnect-stack structures;
- mesh-refinement convergence;
- reciprocity, passivity, and positive-semidefinite checks;
- comparison against analytic values and independent open field solvers;
- regression across PVT/extraction corners.

Tier-2 gate:

- median error under 5% and documented worst-case bounds on the qualified canonical corpus;
- stable netlist topology under harmless shape segmentation changes;
- extracted post-layout simulations reproduce reference trends and pass circuit-level tolerances.

---

## 10.10 Electromagnetic solver (`engines/em`)

Required solver families:

- planar/2.5D method of moments for on-chip passives and interconnect;
- 3D finite-element or finite-difference frequency-domain solver;
- optional FDTD for broadband and photonic applications;
- electrostatic and magnetostatic modes;
- adaptive meshing;
- wave, lumped, and circuit ports;
- conductor and dielectric loss;
- skin and proximity effects;
- S/Y/Z parameters;
- field visualization;
- passivity-preserving model-order reduction.

Architecture rule: meshing, linear algebra, boundary conditions, material models, and result reduction are separate modules with reference tests.

Tier-1 gate:

- transmission line, microstrip, coplanar waveguide, spiral inductor, transformer, cavity, and via-stack examples;
- convergence under mesh refinement;
- passive networks remain passive after reduction;
- S-parameter comparison against analytical or independently generated references.

---

## 10.11 EM/IR and power integrity (`engines/emir`)

Required capabilities:

- power-network extraction;
- static voltage drop;
- dynamic current waveform ingestion;
- current density and via-current analysis;
- temperature-dependent resistivity;
- electromigration limits and lifetime models;
- vectorless and vector-based modes;
- hierarchical reporting;
- hotspot visualization;
- what-if analysis for straps, vias, and widths.

Tier-1 gate:

- resistor-grid analytical benchmarks;
- KCL residual and energy-balance reporting;
- monotonic improvement after known grid reinforcements;
- coupled thermal feedback demonstration.

---

## 10.12 Thermal solver (`engines/thermal`)

Required capabilities:

- steady-state and transient heat equation;
- anisotropic and temperature-dependent materials;
- convection, fixed-temperature, heat-flux, and radiation boundary models;
- chip, package, interposer, and multi-die stacks;
- mapped power density from circuits and EM/IR;
- temperature feedback into device and interconnect models;
- adaptive mesh and multi-resolution coupling.

Tier-1 gate:

- slab, line-source, layered-stack, and transient step-response analytical tests;
- energy conservation report;
- mesh convergence;
- electrothermal ring-oscillator or power-grid demonstration.

---

## 10.13 Reliability and aging (`engines/reliability`)

Required capabilities:

- safe-operating-area checks;
- voltage/current/temperature stress aggregation;
- BTI, HCI, TDDB, and electromigration model interfaces;
- time-dependent device-parameter degradation;
- mission profiles;
- aging-aware re-simulation;
- statistical variation and confidence intervals;
- sensitivity and critical-device ranking.

Foundry-specific models are plugins. The generic engine must not fabricate missing reliability parameters.

Tier-1 gate:

- published compact aging-model examples;
- monotonic stress/lifetime relationships;
- reproducible mission-profile integration;
- explicit uncertainty reporting.

---

## 10.14 Photonics (`engines/photonics`)

Required capabilities:

- photonic schematic elements and optical connectivity;
- compact-model time/frequency simulation;
- eigenmode solving for waveguides;
- FDTD or frequency-domain EM path;
- curved geometry and path-length-aware layout;
- optical ports and S-parameters;
- electro-optic and thermal tuning co-simulation;
- photonic PCells and design-rule checks.

Tier-1 gate:

- straight and bent waveguides;
- directional coupler;
- ring resonator;
- Mach-Zehnder interferometer;
- modulator compact model;
- agreement with analytical coupling/resonance expectations.

---

## 10.15 Optimization, variation, and yield (`engines/optimize`)

Required capabilities:

- parameter sweeps and multidimensional corners;
- Latin hypercube and quasi-random sampling;
- Monte Carlo and mismatch;
- local and global optimization;
- gradient, finite-difference, adjoint, and derivative-free methods;
- sensitivity analysis;
- worst-case search;
- surrogate modelling;
- yield estimation and confidence intervals;
- reproducible distributed scheduling;
- failed-run classification and retry policy.

The optimizer must never conceal simulator failures by treating them as ordinary objective values unless the experiment explicitly defines that policy.

---

# 11. Workbench and desktop UX

## 11.1 Technology

- Electron provides a controlled cross-platform runtime.
- React manages shell-level UI only.
- WebGPU renders schematic, layout, waveforms, meshes, and fields.
- Large engineering data must not be represented as DOM elements or copied through React state.

## 11.2 Required workbench capabilities

- project/library browser;
- multi-window and multi-document editing;
- dockable and detachable panels;
- command palette;
- discoverable shortcuts;
- property inspector;
- hierarchy navigator;
- search across objects and results;
- task/job monitor;
- console and structured logs;
- error/marker browser;
- contextual documentation;
- persistent workspace layouts;
- dark/light/high-contrast themes;
- accessibility and keyboard-only operation;
- crash recovery and session restore.

## 11.3 Schematic editor

- hierarchical editing;
- wires, buses, bundles, taps, labels, pins, and junctions;
- orthogonal and free-angle routing;
- connectivity preview;
- parameter forms;
- symbol generation;
- configurable views;
- operating-point annotation;
- probes and outputs;
- cross-selection with layout and results;
- electrical rule feedback;
- reusable templates.

## 11.4 Layout editor

- hierarchical rendering and editing;
- rectangles, polygons, paths, curves, texts, instances, arrays, and vias;
- layer palette and visibility sets;
- rulers, guides, grids, snapping, alignment, distribution;
- edit-in-place;
- PCell handles;
- assisted guard-ring/via/fill generation;
- connectivity display;
- schematic-driven placement;
- cross-probing;
- live DRC markers;
- density and pattern overlays;
- GDSII/OASIS import/export.

## 11.5 Experiment and waveform environment

- tests, corners, variables, models, outputs, specifications;
- run plans and dependency DAGs;
- local and remote workers;
- waveform plotting with lazy loading;
- calculator expressions;
- cursors and measurements;
- families, overlays, histograms, scatter plots, Smith charts, eye diagrams;
- result comparison and regression history;
- provenance visible from every plot.

## 11.6 UX performance budgets

On reference hardware:

- pointer-to-feedback p95 below 16 ms during ordinary editing;
- command acknowledgement below 100 ms unless explicitly asynchronous;
- viewport pan/zoom at 60 fps for qualified scenes;
- workbench startup below 3 seconds warm and 8 seconds cold;
- no operation blocks the UI event loop longer than 8 ms;
- large-data loading is cancellable and progressive.

---

# 12. Automation, scripting, and MCP

## 12.1 Stable automation surfaces

1. headless `icstudio` CLI;
2. versioned local/remote RPC;
3. first-class MCP server;
4. Python SDK generated from schemas;
5. Rust SDK;
6. sandboxed WASM plugin SDK;
7. optional clean-room compatibility language layers.

## 12.2 Automation rules

- Every UI action that changes engineering state maps to a command accessible headlessly.
- Scripts and MCP tools operate inside transactions.
- APIs are versioned and deprecations are documented.
- Scripts and agents can query provenance, permissions, supported PDK features, and capability availability.
- Remote execution is opt-in and authenticated.
- Plugins declare permissions for filesystem, network, subprocess, GPU, and project mutation.
- No automation surface may expose an unrestricted shell, arbitrary native-code execution, raw SQL-like project mutation, or unbounded filesystem traversal.
- Human-authored and LLM-authored operations use identical validators, design rules, numerical engines, and acceptance gates.

## 12.3 MCP product objective

Any compatible MCP host must be able to inspect, create, modify, simulate, verify, optimize, and export an ICStudio project without screen scraping.

The MCP surface is intended for:

- interactive LLM assistants;
- autonomous but permission-bounded design agents;
- teaching and guided laboratory workflows;
- CI agents that diagnose regressions and failed verification;
- domain-specific copilots built by third parties;
- multi-agent design exploration and optimization.

MCP is not a privileged back door. It is a typed, policy-controlled presentation of the public ICStudio automation model.

## 12.4 MCP protocol support

The server MUST implement and test:

- JSON-RPC lifecycle and capability negotiation;
- `stdio` transport for local hosts;
- Streamable HTTP transport for explicitly enabled local-network or remote use;
- tools, resources, resource templates, prompts, progress, cancellation, logging, and list-change notifications where supported by the locked protocol revision;
- structured tool output plus resource links for large artifacts;
- standard HTTP authorization for remote deployments, with loopback-only unauthenticated development mode permitted only through an explicit flag;
- protocol-version downgrade or refusal with actionable diagnostics.

Optional client-dependent features:

- roots, for host-approved project and PDK boundaries;
- elicitation, for missing user decisions;
- sampling, for nested LLM assistance;
- task-augmented requests for long solver jobs.

Optional features must be negotiated. Core ICStudio correctness must never depend on sampling or an external model.

## 12.5 MCP resource model

Resources are read-only, revision-addressed, bounded views such as:

```text
icstudio://projects
icstudio://project/{project_id}/manifest
icstudio://project/{project_id}/libraries
icstudio://project/{project_id}/cell/{library}/{cell}/{view}
icstudio://project/{project_id}/hierarchy/{cell}/{view}
icstudio://project/{project_id}/pdk/capabilities
icstudio://project/{project_id}/experiments/{experiment_id}
icstudio://project/{project_id}/jobs/{job_id}
icstudio://project/{project_id}/results/{run_id}/summary
icstudio://project/{project_id}/results/{run_id}/signal/{signal}
icstudio://project/{project_id}/verification/{run_id}/markers
icstudio://project/{project_id}/artifacts/{artifact_id}
icstudio://docs/{topic}
```

Resource requirements:

- every mutable-project resource declares project revision and content hash;
- large layouts and waveforms use summaries, windows, level-of-detail queries, pagination, or artifact links;
- proprietary PDK files and encrypted models are never exposed merely because a project references them;
- resource templates document units, coordinate systems, tolerances, and schema versions;
- subscriptions emit invalidation or revision notifications rather than silently serving stale data;
- binary content uses an appropriate MIME type and may be returned through a short-lived local artifact endpoint.

## 12.6 MCP tool taxonomy

Every tool belongs to one risk class:

- `observe`: no mutation and no expensive computation;
- `design-write`: transactional project mutation;
- `compute`: starts or controls solver work;
- `export`: writes externally consumable artifacts;
- `environment`: changes PDKs, plugins, workers, or security settings;
- `dangerous`: exceptional operation requiring per-call interactive approval.

Initial tool families:

```text
project.list                 project.inspect              project.create
library.list                 library.create               cell.list
cell.inspect                 cell.create                  hierarchy.query
schematic.query              schematic.validate          schematic.apply_patch
symbol.generate              netlist.preview              netlist.generate
layout.query                 layout.validate              layout.apply_patch
layout.generate              pcell.instantiate            pdk.capabilities
experiment.inspect           experiment.configure         experiment.clone
simulation.plan              simulation.run               simulation.cancel
results.summarize            results.query                results.measure
verification.plan            verification.run             verification.cancel
verification.explain         verification.apply_fix
optimization.plan            optimization.run             optimization.cancel
artifact.list                artifact.inspect             artifact.export
project.diff                 project.checkpoint           project.rollback
capability.report            diagnostics.explain
```

Tool names and schemas are stable public API. Experimental tools use an `experimental.` prefix and may not be enabled by default.

## 12.7 Semantic patch protocol

LLMs must not rewrite raw project files. Mutations use the project-owned semantic patch format under `crates/ai-patch`.

Every patch contains:

- target project and base revision;
- ordered typed operations;
- stable object IDs or constrained selectors;
- units and coordinate space;
- preconditions and invariants;
- expected affected objects and nets;
- idempotency key;
- author/actor provenance;
- optional rationale kept separate from authoritative engineering state.

Every mutating tool supports, where meaningful:

1. `dry_run`, returning validation results and a semantic diff;
2. explicit commit against `expected_revision`;
3. atomic rollback on failure;
4. checkpoint creation for high-impact changes;
5. conflict diagnostics rather than last-writer-wins behaviour.

The server rejects ambiguous selectors, stale revisions, unitless geometry, unconstrained mass deletion, and patches that exceed configured mutation budgets.

## 12.8 Permissions and consent

Permission scopes are project-local and deny by default:

```text
project:read
project:write
simulation:run
verification:run
optimization:run
artifact:export
pdk:manage
plugin:manage
worker:remote
network:egress
```

Required behaviour:

- read-only local access may be pre-approved by the user;
- mutation, costly computation, export, remote dispatch, network egress, and environment changes require policy approval;
- hosts receive clear tool titles, risk classes, estimated impact, and affected project before approval;
- a single approval may cover a user-defined bounded plan, but not unrelated future actions;
- credentials are never passed through to downstream services;
- remote server authorization uses least-privilege, audience-bound tokens;
- the server records who requested, approved, executed, cancelled, and reverted every operation;
- users can globally disable MCP, force read-only mode, cap compute, or restrict accessible projects.

## 12.9 LLM-oriented engineering responses

Tools return compact, structured, actionable results. A result should contain:

- status and capability/tier used;
- project revision before and after;
- diagnostics with stable codes;
- numerical values with units and tolerances;
- affected object IDs;
- artifact/resource links;
- next valid operations when useful;
- provenance and reproducibility manifest.

The server must not dump an entire flattened layout, raw waveform database, mesh, or PDK into model context. Query tools provide hierarchy, region, signal, time-window, and level-of-detail controls.

## 12.10 MCP prompts and agent recipes

ICStudio publishes versioned prompts for common workflows, including:

- create and verify a CMOS inverter;
- size a current mirror against specifications;
- design and characterize a differential pair;
- investigate simulation convergence;
- diagnose an LVS mismatch;
- explain and repair DRC markers;
- generate a common-centroid layout plan;
- compare pre-layout and post-layout performance;
- run corner and Monte Carlo qualification;
- prepare a tapeout-readiness report.

Prompts are educational and orchestration aids, not hidden policy. Their source is MIT-licensed, reviewable, testable, and usable with any model.

## 12.11 MCP conformance and adversarial QA

CI must test:

- protocol initialization and version negotiation;
- tool/resource/prompt discovery and list-change notifications;
- JSON-schema validation for every request and response;
- cancellation and progress for long jobs;
- stale-revision and idempotency behaviour;
- permission denial and approval flows;
- prompt injection contained in project names, labels, model text, logs, and PDK metadata;
- path traversal, oversized payloads, decompression bombs, malformed URIs, and resource-exhaustion attempts;
- consistency between CLI, SDK, UI, RPC, and MCP outcomes;
- reconnect and recovery after server or solver failure;
- zero network egress in local-only mode;
- no exposure of secrets, proprietary PDK content, or unrelated projects.

---

# 13. Massive parallel agent operating model

## 13.1 Roles

### Programme maintainers

Humans with final authority over scope, releases, legal policy, and architecture changes.

### Architecture agents

Own subsystem boundaries, schemas, invariants, and cross-track decisions. They do not bulk-implement leaf features while reviewing the same area.

### Domain lead agents

One persistent lead per capability family:

- platform/database;
- geometry/layout;
- schematic/connectivity;
- SPICE/models;
- FastSPICE;
- RF;
- digital/AMS;
- DRC/ERC/LVS;
- PEX;
- EM;
- EMIR/thermal/reliability;
- photonics;
- PDK;
- workbench;
- infrastructure;
- QA/benchmarks.

### Implementation agents

Work only on bounded work packages with frozen inputs and tests.

### Adversarial QA agents

Attempt to break implementations using fuzzing, metamorphic tests, pathological inputs, numerical stress, and recovery tests. They must not be the author of the implementation they certify.

### Integration agents

Merge compatible work, resolve mechanical conflicts, run integration suites, and reject architectural drift.

### Documentation agents

Generate reference docs, examples, migration notes, and tutorial projects from accepted behaviour. Documentation is tested where possible.

## 13.2 Agent independence rule

No capability is accepted based solely on the authoring agent’s report. Acceptance requires:

- implementation review;
- independent QA evidence;
- green integration run;
- capability-gate approval.

## 13.3 Parallelism hierarchy

```text
Programme
└── Capability track
    └── Milestone objective
        └── Work package
            └── Leaf task
```

A leaf task should normally require 1–8 focused agent-hours. Larger tasks must be decomposed before implementation.

Agents may work in parallel only when their work packages do not mutate the same unfrozen contracts.

## 13.4 Work-package manifest

Every task has `.project/workpacks/WP-XXXX.yaml`:

```yaml
id: WP-0421
title: Implement transient breakpoint scheduler
capability: CAP-SIM-TRAN
milestone: M3
status: ready # proposed|blocked|ready|active|review|accepted|rejected|paused
priority: critical
owner: luna-agent-id
reviewer: independent-agent-id
qa_owner: independent-agent-id
depends_on: [WP-0390, WP-0402]
contracts:
  inputs: [schemas/netlist-ir/v3, engines/spice/integrator-api-v2]
  outputs: [engines/spice/breakpoints]
allowed_paths:
  - engines/spice/src/transient/breakpoints/**
forbidden_paths:
  - schemas/**
acceptance:
  commands:
    - just test-capability CAP-SIM-TRAN
  tests:
    - TRAN-BREAK-001
    - TRAN-BREAK-002
  performance_budget: "<2% overhead on TRAN reference corpus"
clean_room:
  proprietary_inputs_used: false
  public_sources: []
artifacts:
  expected: [test-report.json, benchmark-delta.json]
checkpoint:
  last_green_commit: null
  notes: null
```

An agent must refuse a work package whose allowed paths are insufficient or whose contracts are ambiguous. The agent proposes a manifest amendment rather than editing unrelated areas.

## 13.5 Branches and worktrees

- One branch and isolated worktree per work package.
- Branch format: `wp/WP-XXXX-short-name`.
- Agents never commit directly to `main`.
- Commits are small, buildable, and signed where infrastructure permits.
- Generated files are committed only when policy explicitly requires them.
- A branch must rebase or merge the latest integration baseline before review.

## 13.6 Merge trains

There are three protected trains:

1. **fast train:** docs, tests, non-contractual implementation changes;
2. **core train:** shared libraries and frozen internal APIs;
3. **schema train:** file formats, RPC, PDK, result, and public API schemas.

Schema-train changes require architecture approval, migration tests, and two independent reviewers.

## 13.7 Agent prompt contract

Every subagent prompt must include:

- work-package ID;
- exact objective;
- allowed and forbidden paths;
- applicable sections of this file;
- inputs and frozen contracts;
- required tests and commands;
- acceptance evidence;
- instruction to stop rather than silently broaden scope.

Never send an agent “build the simulator” or “finish the layout editor.”

---

# 14. OpenStack compute and orchestration

## 14.1 Node roles

A single large OpenStack instance may host multiple isolated runners. When more instances are available, use these roles:

- **controller:** Git mirror, scheduler, metadata database, dashboards;
- **build runners:** compilation and ordinary tests;
- **numeric runners:** high-memory CPU simulations and solver benchmarks;
- **GPU runners:** WebGPU visual tests, CUDA/HIP acceleration, EM/field kernels;
- **fuzz runners:** sanitizers, property tests, mutation campaigns;
- **release runners:** clean reproducible builds and signing;
- **artifact store:** S3-compatible object storage backed by Swift/Ceph or equivalent.

## 14.2 Isolation

Each work package executes inside a locked OCI image with:

- CPU, memory, disk, process, and time quotas;
- read-only dependency caches;
- writable worktree and artifact directory;
- no network by default after dependencies are materialized;
- explicit secrets only for release or external-service tasks.

## 14.3 Scheduling classes

- `lint`: seconds to minutes;
- `unit`: under 15 minutes;
- `integration`: under 60 minutes;
- `numeric`: up to 6 hours;
- `fuzz`: renewable 6-hour shards;
- `overnight`: full corpus and performance baselines;
- `release`: fully clean reproducible build.

A failed runner must leave logs, core dumps where safe, seeds, environment manifests, and partial artifacts.

## 14.4 Caching

Use content-addressed caches for:

- source dependencies;
- compiler outputs;
- generated compact-model kernels;
- meshes;
- factorizations when input hashes match;
- PCell results;
- simulation operating points;
- benchmark datasets.

Cache correctness is verified by hashes including tool version, schema version, compile flags, PDK version, architecture, and numerical mode.

## 14.5 Resource-aware parallelism

The scheduler must avoid impressive-looking oversubscription that slows numerical work. Jobs declare:

- cores;
- RAM;
- scratch disk;
- GPU and VRAM;
- expected wall time;
- deterministic/non-deterministic mode.

The scheduler records actual consumption and updates future estimates.

---

# 15. Verification and QA strategy

## 15.1 Test pyramid

1. pure unit tests;
2. property-based tests;
3. analytical numerical tests;
4. component integration tests;
5. differential tests;
6. metamorphic tests;
7. fuzzing and mutation tests;
8. end-to-end PDK flows;
9. visual regression tests;
10. performance and scalability tests;
11. crash/recovery and fault injection;
12. tapeout/shuttle evidence.

## 15.2 Numerical correctness

Every numerical result includes:

- solver and version;
- tolerances;
- convergence status;
- residual norms;
- iteration counts;
- warnings;
- precision mode;
- deterministic seed;
- platform information;
- input hashes.

Tests compare using physically meaningful absolute and relative tolerances. Arbitrary screenshot comparison is not numerical validation.

## 15.3 Differential testing

Open tools may serve as oracles, including ngspice, Xyce, KLayout, Magic, Netgen, OpenROAD components, Verilator, and open field solvers, subject to licence policy.

Differential disagreement does not automatically mean our result is wrong. The QA report must triangulate with analytical solutions, higher precision, or a third implementation.

## 15.4 Metamorphic properties

Examples:

- renaming nets does not change simulation;
- reordering independent devices does not change results;
- translating a layout preserves DRC/LVS/PEX results;
- splitting a polygon edge without changing geometry preserves extraction;
- equivalent series/parallel representations preserve circuit response;
- refining a mesh converges toward a stable result;
- increasing conductor width must not increase DC resistance under identical materials;
- passive extracted networks remain passive;
- repeating a seeded Monte Carlo run reproduces samples.

## 15.5 Fuzzing

Required fuzz targets:

- native project parser;
- GDSII/OASIS/LEF/DEF/SPICE/Verilog/Touchstone importers;
- geometry Boolean operations;
- PDK and rule-deck parsers;
- netlist normalization;
- matrix assembly;
- compact-model parameter validation;
- RPC and shared-memory protocols;
- plugin sandbox;
- undo/redo transaction sequences.

All discovered failures become minimized permanent regression cases.

## 15.6 Visual QA

The UI test harness must:

- launch cleanly on all supported OS targets;
- execute deterministic interaction scripts;
- capture editor and panel screenshots;
- compare with perceptual thresholds;
- detect clipping, overlap, text truncation, incorrect z-order, and theme contrast;
- test multiple DPI scales and window sizes;
- record videos for animation regressions where useful.

Visual QA does not replace functional assertions.

## 15.7 Performance QA

Each capability has a baseline dataset and budget. Every pull request reports statistically meaningful changes for affected benchmarks.

No optimization is accepted if it weakens accuracy outside declared modes.

---

# 16. Capability graph

Capabilities use stable IDs. A capability is green only when all mandatory acceptance tests pass on the protected baseline.

Core IDs:

```text
CAP-INFRA-BOOT       reproducible build and CI
CAP-PROJ-DB          project/library/cell/view database
CAP-TXN               transactions, undo, crash recovery
CAP-RPC               typed worker protocol
CAP-MCP-BASE          MCP lifecycle, discovery, transports, and policy
CAP-MCP-READ          revisioned engineering resources and query tools
CAP-MCP-MUTATE        transactional semantic design patches
CAP-MCP-JOBS          cancellable simulation/verification/optimization jobs
CAP-MCP-AGENTFLOW     end-to-end LLM-driven qualified design flow
CAP-PDK-BASE          technology and PDK runtime
CAP-SCH-EDIT          schematic and symbol editing
CAP-CONNECT           connectivity and ERC foundation
CAP-LAY-EDIT          hierarchical layout editing
CAP-GEO-KERNEL        exact integer geometry
CAP-IO-GDS            GDSII import/export
CAP-IO-OASIS          OASIS import/export
CAP-EXP               experiment orchestration
CAP-WAVE              waveform/result platform
CAP-SIM-OP            operating point
CAP-SIM-DC            DC analysis
CAP-SIM-AC            AC analysis
CAP-SIM-TRAN          transient analysis
CAP-SIM-NOISE         noise analysis
CAP-MODELS            compact-model framework
CAP-VERILOGA          Verilog-A compiler/runtime
CAP-FASTSPICE         accelerated circuit simulation
CAP-RF                RF periodic analyses
CAP-DIGITAL           four-state event simulation
CAP-AMS               analog/mixed-signal co-simulation
CAP-DRC               design-rule checking
CAP-ERC               electrical/antenna checking
CAP-LVS               layout-versus-schematic
CAP-PEX-RC            rule/analytic RC extraction
CAP-PEX-FIELD         field-solver extraction
CAP-EM-PLANAR         planar electromagnetic solving
CAP-EM-3D             three-dimensional EM
CAP-EMIR              voltage drop/electromigration
CAP-THERMAL           steady/transient thermal
CAP-RELIABILITY       aging and stress
CAP-PHOTONICS         photonic design/simulation
CAP-OPTIMIZE          optimization/variation/yield
CAP-FLOW-OPENPDK      qualified end-to-end open-PDK flow
CAP-RELEASE           signed cross-platform release
```

`.project/capabilities.json` records status, evidence artifacts, accepted commit, validator, and known limitations.

---

# 17. Twelve-month milestone programme

Milestones are gates, not promises tied only to dates. Multiple tracks execute in parallel. A milestone may be partially green, but the programme only declares it complete when all mandatory gates pass.

## M0 — Constitution and reproducible factory (Weeks 1–2)

### Objectives

- establish the ICStudio repository and MIT licence policy;
- freeze initial schemas, MCP baseline, and directory structure;
- implement work-package scheduler and status model;
- create reproducible Linux build image;
- configure Windows/macOS build strategy;
- establish artifact store, cache, dashboards, and self-hosted runners;
- seed benchmark and regression repositories;
- create security, clean-room, MCP threat-model, and licence checks.

### Mandatory gates

- `just bootstrap && just build && just test-fast` passes from a clean clone;
- an agent can claim, execute, and submit a sample work package;
- checkpoint and resume-check commands work;
- SBOM and licence scan verify the MIT core and all exceptions;
- capability dashboard generated from `.project/` state;
- an MCP conformance smoke test negotiates the locked revision and lists at least one resource, tool, and prompt over `stdio`.

### Restart point

`CP-M0-FACTORY`

---

## M1 — Design kernel and headless skeleton (Weeks 3–6)

### Parallel tracks

- project database and transactions;
- typed RPC, MCP gateway, and worker lifecycle;
- integer geometry and spatial index;
- netlist IR and result DB;
- PDK base schema;
- desktop shell and WebGPU scene prototype;
- GDSII/SPICE parser scaffolds.

### Mandatory gates

- create/save/reopen project with libraries, cells, and views;
- crash recovery after injected termination during edit;
- deterministic serialization round trip;
- one million simple shapes indexed and queried within baseline budget;
- worker crash does not terminate platform service;
- CLI, UI, and MCP read resources display the same project state and revision.

### Restart point

`CP-M1-KERNEL`

---

## M2 — Schematic-to-reference-simulation vertical slice (Weeks 7–12)

### Parallel tracks

- schematic/symbol editor;
- connectivity and netlisting;
- SPICE parser and device API;
- MNA, sparse solve, Newton iteration;
- operating point, DC, and AC;
- waveform storage and plotting;
- SKY130/GF180 primitive-device package scaffolds;
- adapter oracles for ngspice and Xyce.

### Mandatory gates

- create and simulate inverter, current mirror, differential pair, and RLC filter;
- hierarchical netlisting and parameter propagation;
- operating-point annotations in schematic;
- analytic DC/AC corpus passes;
- independent SPICE engine, not an adapter, produces accepted results;
- visual, CLI, SDK, and MCP workflows share the same run manifest;
- an MCP client can inspect the inverter, apply one dry-run semantic schematic patch, commit it transactionally, launch the simulation, and retrieve bounded result summaries.

### Restart point

`CP-M2-SCHEMATIC-SPICE`

---

## M3 — Layout, transient simulation, and physical verification slice (Weeks 13–20)

### Parallel tracks

- hierarchical layout editor;
- GDSII/OASIS;
- PCells and layer technology;
- transient integration and behavioural devices;
- DRC geometry/rule compiler;
- LVS extraction and graph matching;
- basic rule-based RC extraction;
- cross-probing and marker browser;
- MCP layout, verification-job, and marker-resource surfaces;
- visual QA harness.

### Mandatory gates

- draw inverter schematic and layout;
- native DRC identifies seeded violations;
- native LVS matches correct layout and diagnoses mutations;
- native RC extraction emits post-layout netlist;
- post-layout transient simulation runs in native SPICE engine;
- GDSII round trip preserves qualified geometry;
- undo/redo and crash recovery pass randomized edit sequences;
- an MCP client can dry-run and commit a bounded layout patch, launch/cancel DRC, LVS, and PEX, and navigate structured markers without access to raw proprietary rule decks.

### Restart point

`CP-M3-FIRST-CLOSED-LOOP`

This is the first critical public alpha because it demonstrates an independent end-to-end custom-IC loop.

---

## M4 — ADE-class experiments, models, and open-PDK qualification (Weeks 21–28)

### Parallel tracks

- transient robustness and noise;
- BSIM4/modern model integration;
- Verilog-A frontend;
- corners, sweeps, Monte Carlo, specifications;
- distributed run scheduler;
- advanced waveform calculator;
- PDK package manager and validators;
- DRC/LVS/PEX open-PDK qualification corpus;
- MCP agent recipes, model-context budgets, and end-to-end fixtures;
- documentation and teaching modules.

### Mandatory gates

- reproducible corner and Monte Carlo experiments;
- noise and transient benchmark suites accepted;
- at least one modern MOS model passes model conformance tests;
- deterministic PCell cache and package installation;
- full inverter, ring oscillator, op-amp, and SRAM-bitcell modules;
- public capability report generated automatically;
- a model-agnostic MCP host can complete a guarded open-PDK characterization workflow using only published resources, tools, and prompts, with every mutation and run represented in provenance.

### Restart point

`CP-M4-OPENPDK-BETA`

---

## M5 — Parallel solver expansion I (Weeks 29–36)

All tracks run concurrently behind frozen IRs.

### Track A: FastSPICE

- partitioning reference implementation;
- repeated-instance acceleration;
- multirate/event-driven prototype;
- accuracy modes and error reports.

### Track B: RF

- n-port/S-parameter engine;
- shooting-Newton PSS;
- harmonic-balance reference;
- oscillator tests.

### Track C: Digital/AMS

- four-state scheduler;
- language subset;
- SDF/VCD;
- analog/digital bridges and synchronized time.

### Track D: PEX/EM

- 2D/2.5D electrostatic extraction;
- mesher infrastructure;
- planar MoM prototype;
- passivity-aware network reduction.

### Track E: UI and collaboration

- experiment assembler;
- layout connectivity and constraint aids;
- review packages and project diffs;
- remote job monitoring;
- bounded multi-agent exploration through MCP checkpoints, branches, and result comparison.

### Mandatory gates

- Tier-1 evidence for each solver track;
- no track bypasses common IR/provenance contracts;
- end-to-end regression remains green;
- concurrent MCP agents cannot overwrite one another silently and conflicts produce semantic diffs;
- full checkpoint includes all benchmark artifacts and model versions.

### Restart point

`CP-M5-SOLVER-FANOUT`

---

## M6 — Parallel solver expansion II (Weeks 37–44)

### Parallel tracks

- FastSPICE scaling and GPU batches;
- RF PAC/PNoise/envelope/QPSS;
- AMS connect modules and real-number modelling;
- 3D EM reference solver;
- field-solver PEX;
- EM/IR engine;
- steady/transient thermal solver;
- reliability/aging framework;
- photonic eigenmode and compact-model flow;
- optimization, sensitivity, and yield engine;
- MCP schemas and resources for every new solver class.

### Mandatory gates

- canonical Tier-1 benchmark for every in-scope solver class;
- coupled demonstrations: electrothermal, AMS, post-layout RF, EM-extracted passive;
- independent QA reproduces each demonstration from a clean clone;
- every in-scope solver can be planned, launched, observed, cancelled, and summarized through MCP without exposing unbounded result data;
- resource scaling and cancellation verified on OpenStack runners.

### Restart point

`CP-M6-ALL-SOLVER-CLASSES`

---

## M7 — Convergence, acceleration, and parity audit (Weeks 45–48)

### Objectives

- close high-severity correctness gaps;
- compare every engine with analytical and independent references;
- optimize critical kernels without changing accepted semantics;
- publish unsupported constructs and accuracy envelopes;
- complete cross-platform installers;
- perform security and recovery audits;
- complete MCP compatibility, authorization, injection-resistance, and host-interoperability audit;
- freeze release schemas.

### Mandatory gates

- no open blocker in core open-PDK flow;
- all capability reports generated from CI evidence;
- benchmark baselines signed and archived;
- public parity matrix reviewed by independent domain experts where available;
- MCP conformance corpus passes against the locked protocol revision and at least two independent compatible hosts;
- no unexplained numerical disagreement in release corpus.

### Restart point

`CP-M7-RC`

---

## M8 — Tapeout evidence and 1.0 release (Weeks 49–52)

### Objectives

- complete one or more open-PDK shuttle-ready designs;
- run independent verification cross-checks;
- archive GDS/OASIS, reports, models, manifests, and hashes;
- ship desktop installers and headless containers;
- publish architecture, PDK SDK, benchmark methods, and known limits;
- publish the MCP tool/resource/prompt catalogue, permission model, host setup examples, conformance evidence, and agent recipes;
- create contributor onboarding and post-1.0 roadmap.

### Release gates

- full schematic-to-GDS flow reproducible from a clean environment;
- native solver path used for all mandatory capability demonstrations;
- external tools used only as optional corroborating oracles in release evidence;
- no critical data-loss, crash, security, or correctness defect;
- release artifacts reproduce bit-for-bit where platform permits;
- capability matrix clearly distinguishes Tier 1, 2, and 3;
- a clean installation can reproduce the qualified end-to-end design through desktop, CLI, SDK, and MCP entry points with equivalent manifests;
- tapeout package passes all available independent checks.

### Restart point

`CP-M8-1.0`

Silicon return is a later evidence update if fabrication timing exceeds the programme window.

---

# 18. Pause and resume protocol

A project pause is an expected operation, not a failure.

## 18.1 Creating a checkpoint

Run:

```bash
just checkpoint --name CP-<MILESTONE>-<LABEL>
```

The command must:

1. require a clean or explicitly recorded working tree;
2. run mandatory fast and capability-gate tests;
3. snapshot `.project/capabilities.json`;
4. record accepted and active work packages;
5. record dependency lockfiles and container digests;
6. hash benchmark datasets, PDKs, models, and artifacts;
7. store last-green commits for every track;
8. export open blockers and ownership state;
9. archive dashboards and test reports;
10. write an immutable checkpoint manifest under `.project/checkpoints/` and artifact storage.

## 18.2 Pausing active work packages

Every active agent must:

- commit buildable work where possible;
- update work-package status to `paused`;
- record exact next action;
- record failing tests and seeds;
- upload uncommitted generated artifacts that matter;
- release locks and compute reservations.

No “90% done” narrative is accepted without machine-readable evidence.

## 18.3 Resuming

Run:

```bash
just resume-check --checkpoint CP-M4-OPENPDK-BETA
```

The command verifies:

- toolchain availability;
- artifact integrity;
- schema compatibility;
- current branch ancestry;
- benchmark corpus availability;
- work-package dependency graph;
- capability status.

The scheduler then prioritizes:

1. restore broken baseline;
2. complete near-finished critical-path work;
3. refresh stale dependencies;
4. rerun capability gates;
5. open new work packages only after the baseline is green.

## 18.4 Long pauses

After a pause longer than 90 days:

- create a toolchain-refresh work package;
- audit dependency security and licences;
- rerun file-format migration tests;
- rerun a representative numerical corpus before feature development;
- do not bulk-upgrade dependencies and feature code in the same pull request.

---

# 19. Open-source momentum protocol

## 19.1 Contributor entry levels

- **Starter:** docs, examples, UI polish, isolated tests.
- **Contributor:** bounded implementation work packages.
- **Domain contributor:** numerical models, PDKs, rule decks, solver algorithms.
- **Maintainer:** reviews, release duties, capability acceptance.
- **Domain lead:** owns contracts and roadmap for a solver family.

## 19.2 Good first issues

Good first issues must be real, bounded work—not fake chores. Each includes test commands, allowed paths, and expected output.

## 19.3 Governance

- technical decisions are evidence-based;
- maintainers publish conflicts of interest;
- capability claims require public artifacts;
- no vendor may privately redefine an open format or gate community access;
- security embargoes are permitted for responsible disclosure;
- a lightweight DCO is required unless governance later adopts a CLA.

## 19.4 External solver and PDK contributions

New engines and PDKs must implement stable interfaces and ship qualification tests. A contribution is not accepted merely because it runs one example.

---

# 20. Security and trust

Threats include malicious PDKs, malformed layout files, compromised plugins, poisoned caches, untrusted remote workers, and proprietary-data leakage.

Required controls:

- sandbox PDK and plugin execution;
- memory-safe parsers where practical;
- fuzz all externally supplied formats;
- signed packages and release artifacts;
- dependency pinning and SBOMs;
- least-privilege worker tokens;
- encrypted remote transport;
- design-data redaction in telemetry;
- telemetry disabled by default;
- secrets never stored in project files;
- reproducible runner images;
- artifact provenance and hash verification;
- MCP disabled or read-only by policy in high-security environments;
- MCP project allowlists, revision preconditions, mutation budgets, and approval logs;
- prompt-injection resistance: project/PDK/user strings are untrusted data, never executable policy;
- no arbitrary shell tool, unrestricted file URI, credential passthrough, or implicit network egress;
- remote MCP deployments bind to loopback by default and require explicit secure configuration to listen elsewhere.

Remote workers and remote MCP clients must be treated as untrusted unless explicitly enrolled in a trusted pool. Sensitive designs may require local-only execution.

---

# 21. Documentation requirements

The documentation system generates:

- user manual;
- CLI, SDK, RPC, and MCP reference;
- PDK developer guide;
- solver theory and numerical-method notes;
- benchmark methodology;
- capability matrix;
- examples, MCP host configurations, agent recipes, and teaching modules;
- migration and compatibility notes;
- release known limitations.

Every public command and schema field must have reference documentation. Examples in documentation are compiled or executed in CI where possible.

Research claims include citations and distinguish established algorithms from project inventions.

---

# 22. Definition of done

A work package is done only when:

- implementation is complete within allowed scope;
- formatting, lint, static analysis, and sanitizers pass;
- unit and capability tests pass;
- independent QA evidence exists;
- performance budget is met or an approved exception exists;
- user-facing behaviour is documented;
- public APIs, MCP tools/resources/prompts, permission scopes, and schemas are documented where affected;
- provenance and licence metadata are complete;
- no placeholder, disabled test, unexplained tolerance, or silent fallback remains;
- integration train accepts the commit;
- work-package manifest status is `accepted`.

A capability is done only when its gate is accepted on a protected baseline.

A milestone is done only when every mandatory capability and artifact is green.

---

# 23. Failure policy

Agents must report failure directly.

Forbidden responses include:

- claiming a test passed without running it;
- replacing a native engine with an adapter and calling it parity;
- weakening tolerances solely to make a test green;
- deleting difficult tests;
- swallowing convergence or parsing errors;
- silently flattening unsupported hierarchy;
- ignoring units or grid errors;
- introducing nondeterminism without recording seeds;
- broad rewrites unrelated to the assigned work package;
- changing schemas to accommodate a local shortcut.

When blocked, the agent updates the manifest with:

- exact blocker;
- minimal reproducer;
- attempted approaches;
- current artifact paths;
- proposed next work packages.

---

# 24. Architecture decision procedure

Architecture decisions are stored as TOML under `.project/decisions/ADR-XXXX.toml`:

```toml
id = "ADR-0042"
title = "Use signed 64-bit integer DBU coordinates"
status = "accepted"
date = "2026-08-01"
owners = ["architecture-geometry"]

[context]
summary = "Authoritative layout coordinates require exact deterministic storage."

[decision]
summary = "Use i64 DBU in persistent and kernel geometry APIs."

[consequences]
positive = ["exact predicates", "deterministic serialization"]
negative = ["explicit overflow handling", "conversion boundaries"]

[validation]
commands = ["just test-capability CAP-GEO-KERNEL"]
```

Changing an accepted decision requires a superseding ADR, migration plan, and affected capability reruns.

---

# 25. Initial work-package fanout

The first orchestration wave should create at least the following independent packages after M0 contracts are frozen.

## Platform

1. project ID and revision schema;
2. transaction journal;
3. content-addressed blob store;
4. worker lifecycle and cancellation;
5. provenance manifest;
6. capability-state generator;
7. checkpoint command;
8. work-package scheduler.

## Geometry and layout

9. DBU/unit system;
10. box and transform primitives;
11. exact orientation predicates;
12. R-tree or equivalent spatial index;
13. polygon canonicalization;
14. Boolean-operation reference kernel;
15. hierarchy iterator;
16. GDSII reader corpus;
17. GDSII writer round-trip;
18. WebGPU layout scene prototype.

## Schematic and connectivity

19. schematic object schema;
20. symbol schema;
21. wire/junction connectivity;
22. bus and bundle semantics;
23. hierarchical net resolver;
24. parameter expression engine;
25. SPICE netlist emitter;
26. WebGPU schematic scene prototype.

## SPICE

27. netlist lexer/parser;
28. normalized netlist IR;
29. sparse matrix interface;
30. resistor/current/voltage source stamps;
31. nonlinear device API;
32. Newton loop reference;
33. operating-point analysis;
34. DC sweep;
35. AC linearization;
36. analytic circuit corpus.

## Verification

37. layer-expression IR;
38. geometry tiler;
39. width/spacing reference checks;
40. conductor connectivity extractor;
41. netlist graph normalization;
42. graph-matching reference;
43. marker database schema.

## UI and infrastructure

44. Electron shell;
45. docking and command system;
46. RPC client generation;
47. deterministic visual-test harness;
48. Linux runner image;
49. sanitizer runner;
50. benchmark dashboard.

## MCP and AI automation

51. MCP lifecycle and version negotiation;
52. `stdio` transport and host fixtures;
53. Streamable HTTP transport and authorization boundary;
54. MCP resource URI and pagination model;
55. MCP tool catalogue and schema generator;
56. semantic design-patch format and dry-run engine;
57. MCP permission/risk policy engine;
58. progress, cancellation, and long-job handles;
59. MCP conformance and adversarial corpus;
60. end-to-end LLM agent-flow fixture for an open-PDK inverter.

Each package must be refined into a manifest before assignment.

---

# 26. Parity reporting

Every release publishes a table with, at minimum:

- capability ID;
- tier;
- supported analyses/features;
- unsupported features;
- accuracy corpus and tolerances;
- performance corpus;
- external oracles used;
- last accepted commit;
- known severe bugs;
- PDK qualification status;
- platform status.

Marketing language must match this table.

“Independent” means the release path runs the project-owned engine. It does not mean the project invented every underlying numerical algorithm.

“Parity” means comparable user outcomes over an explicitly bounded and benchmarked domain. It does not mean undocumented bug-for-bug compatibility.

---

# 27. Release strategy

Versioning:

- `0.x`: schemas may evolve with migrations;
- `1.0`: native open-PDK end-to-end flow and stable project format;
- later minor releases: backward-compatible features;
- major releases: planned schema or API breaks with migration tools.

Release channels:

- nightly;
- milestone preview;
- release candidate;
- stable;
- long-term-support after the project has capacity.

Every stable release includes:

- installers/containers;
- checksums and signatures;
- SBOM;
- source archive;
- benchmark report;
- capability matrix;
- migration tool;
- example projects;
- MCP server binaries, tool/resource/prompt catalogue, host configurations, and conformance report;
- known limitations.

---

# 28. Canonical commands

The following commands must eventually exist and remain stable:

```bash
# Environment
just bootstrap
just doctor

# Build and test
just build
just test-fast
just test
just test-capability CAP-ID
just fuzz TARGET
just bench [SUITE]

# Application
just studio
just cli -- <args>
just mcp-stdio
just mcp-http --listen 127.0.0.1:8765

# Project flow
icstudio project create demo
icstudio pdk install <package>
icstudio netlist <cell:view>
icstudio simulate <experiment>
icstudio drc <cell:layout>
icstudio lvs <cell:layout> --against <cell:schematic>
icstudio extract <cell:layout>
icstudio export gds <cell:layout>

# MCP
icstudio mcp serve --transport stdio
icstudio mcp serve --transport http --listen 127.0.0.1:8765
icstudio mcp inspect
icstudio mcp conformance
icstudio mcp permissions --project <project>

# Orchestration
just workpack-validate WP-XXXX
just checkpoint --name CP-...
just resume-check --checkpoint CP-...
just capability-report
just release-audit
```

The `icstudio` executable and `icstudio://` resource scheme are canonical. After M1, incompatible naming changes require an ADR, automated migration, and compatibility aliases. A project-wide legal rename may occur before 1.0 only under the policy in section 3.4.

---

# 29. Non-goals and anti-patterns

The following are explicitly rejected:

- a browser-only SaaS that uploads private designs by default;
- a monolithic process containing UI, PDK code, and all solvers;
- storing layout coordinates as floating point;
- rendering large layouts as SVG/DOM nodes;
- Python as the authoritative design database;
- one universal intermediate representation forced onto incompatible solver domains;
- depending on a single external engine while claiming independence;
- prematurely implementing advanced-node proprietary PDK support without legal access and qualification partners;
- optimizing before a reference implementation and correctness corpus exist;
- hundreds of autonomous agents modifying shared APIs simultaneously;
- accepting generated code without independent tests and review;
- using conversation history as project state;
- hiding incomplete features behind polished UI.

---

# 30. Final directive to every agent

Your task is not to maximize lines of code or apparent progress. Your task is to move one capability from its current evidence-backed state to a higher evidence-backed state without damaging other capabilities.

Before editing:

1. read this file;
2. read the assigned work-package manifest;
3. inspect frozen contracts and dependent tests;
4. reproduce the current baseline;
5. confirm allowed paths.

Before submitting:

1. run required tests;
2. add adversarial and regression coverage;
3. record benchmark impact;
4. update structured status and provenance;
5. state limitations plainly;
6. leave the repository in a state another agent can resume without speaking to you.

The project succeeds when independent engineers can trust the results, reproduce the flow, extend the platform, pause safely, resume safely, and tape out useful silicon without requiring access to a closed custom-IC suite.

---

# 31. Public technical reference baseline

The following public resources are starting points, not implementation specifications and not substitutes for independent verification:

- Cadence public Virtuoso Layout Suite overview: https://www.cadence.com/en_US/home/tools/custom-ic-analog-rf-design/layout-design/virtuoso-layout-suite.html
- Cadence public Pegasus overview: https://www.cadence.com/en_US/home/tools/digital-design-and-signoff/silicon-signoff/pegasus-verification-system.html
- Cadence public Quantus overview: https://www.cadence.com/en_US/home/tools/digital-design-and-signoff/silicon-signoff/quantus-extraction-solution.html
- Si2 OpenAccess Coalition: https://si2.org/openaccess-coalition/
- Si2 public standards and LEF/DEF resources: https://si2.org/public-standards-and-solutions/
- ngspice shared-library interface and manual: https://ngspice.sourceforge.io/shared.html
- Xyce Parallel Electronic Simulator: https://xyce.sandia.gov/
- KLayout documentation: https://www.klayout.de/doc.html
- OpenROAD/OpenDB documentation: https://openroad.readthedocs.io/
- Verilator documentation: https://verilator.org/guide/latest/

Each solver track must expand its own machine-readable bibliography with public papers, standards, benchmark provenance, and licence metadata.
