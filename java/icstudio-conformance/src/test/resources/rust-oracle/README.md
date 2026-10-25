# Rust M1 oracle corpus

Recorded from the retired Rust/Electron M1 kernel (CP-M1-KERNEL) by the throwaway
`oracle-gen` harness that linked the Rust crates by path. The Java conformance suite
replays these inputs and compares outputs byte-for-byte or message-for-message.

* `project/`   project file, transaction sequence, expected final bytes, and rejection messages
* `pdk/`, `netlist/`, `results/`, `formats/`, `rpc/` case corpora (`.input` + `.expected` or `.error`)
* `geometry/`  one-million-shape query evidence and rectangle error messages
* `worker/`    worker stdin/stdout/exit transcripts
* `mcp/`       JSON-RPC request/response transcript (`environment.txt` names the roots)
* `cli/`       CLI argument/stdout/stderr/exit records (`@PROGRAMME@`, `@FINAL@`, `@BASE@` are placeholders)
* `evidence/`  programme snapshot and truth/capability/checkpoint-hash evidence

Requires the Rust CLI at the commit that produced these files; it stays available in git history.
