set dotenv-load := false

bootstrap:
    cargo fetch --locked

ui-bootstrap:
    node apps/workbench/scripts/run-npm.mjs install --ignore-scripts --no-audit --no-fund

format:
    cargo fmt --all

format-check:
    cargo fmt --all -- --check

lint:
    cargo clippy --workspace --all-targets --locked -- -D warnings

build:
    cargo build --workspace --all-targets --locked

ui-build: ui-bootstrap
    node apps/workbench/scripts/run-npm.mjs run build

test-fast:
    cargo test --workspace --all-targets --locked

ui-test: ui-bootstrap
    node apps/workbench/scripts/run-npm.mjs test

ui-license-check: ui-bootstrap
    node apps/workbench/scripts/run-npm.mjs run license-check

ui-check: ui-build ui-test ui-license-check

test-m1-geometry:
    cargo test --release --locked -p icstudio-geometry indexes_and_queries_one_million_simple_shapes -- --ignored

validate:
    cargo run --locked --quiet --bin icstudio -- validate

truth:
    cargo run --locked --quiet --bin icstudio -- truth

capability-report:
    cargo run --locked --quiet --bin icstudio -- capabilities --output artifacts/capability-report.md

rust-sbom:
    cargo run --locked --quiet --bin icstudio -- sbom --output artifacts/icstudio.spdx.json

ui-sbom: ui-bootstrap
    node apps/workbench/scripts/run-npm.mjs run sbom

sbom: rust-sbom ui-sbom

license-check:
    cargo run --locked --quiet --bin icstudio -- license-check

mcp-smoke:
    cargo test --locked --test mcp_stdio

checkpoint name:
    cargo run --locked --quiet --bin icstudio -- checkpoint --name {{name}}

resume-check name:
    cargo run --locked --quiet --bin icstudio -- resume-check --checkpoint {{name}}

ci: format-check lint build test-fast ui-check validate license-check mcp-smoke
