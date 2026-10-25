set dotenv-load := false

# Authoritative stack: Java 25 + Maven Wrapper (ADR-0001). Every task runs through the reactor.
maven := "./mvnw -B -ntp"
cli-classpath := "java/icstudio-cli/target/classpath.txt"

bootstrap:
    {{maven}} -DskipTests install

build:
    {{maven}} -DskipTests package

test-fast:
    {{maven}} test

verify:
    {{maven}} verify

# One-million-shape reference geometry baseline (CP-M1-KERNEL parity gate).
test-m1-geometry:
    {{maven}} -pl java/icstudio-conformance -am test -Dtest=GeometryOracleTest -Dsurefire.failIfNoSpecifiedTests=false

# Stdio MCP and cross-surface equivalence gates.
mcp-smoke:
    {{maven}} -pl java/icstudio-conformance -am test -Dtest=McpOracleTest,SurfaceEquivalenceTest -Dsurefire.failIfNoSpecifiedTests=false

# Compile and record the headless CLI classpath once, then run commands against it.
cli-setup:
    {{maven}} -q -pl java/icstudio-cli -am install -DskipTests
    {{maven}} -q -pl java/icstudio-cli dependency:build-classpath -Dmdep.outputFile=target/classpath.txt -Dmdep.includeScope=runtime

cli *ARGS: cli-setup
    java -cp "$(cat {{cli-classpath}}):java/icstudio-cli/target/classes" dev.icstudio.cli.IcStudioCli {{ARGS}}

validate: cli-setup
    java -cp "$(cat {{cli-classpath}}):java/icstudio-cli/target/classes" dev.icstudio.cli.IcStudioCli validate

truth: cli-setup
    java -cp "$(cat {{cli-classpath}}):java/icstudio-cli/target/classes" dev.icstudio.cli.IcStudioCli truth

capability-report: cli-setup
    java -cp "$(cat {{cli-classpath}}):java/icstudio-cli/target/classes" dev.icstudio.cli.IcStudioCli capabilities --output artifacts/capability-report.md

# CycloneDX aggregate SBOM covering the Java reactor, archived under artifacts/.
sbom:
    {{maven}} -DskipTests verify
    mkdir -p artifacts
    cp target/icstudio.cdx.json artifacts/icstudio.cdx.json

license-check: sbom
    {{maven}} -pl java/icstudio-conformance -am test -Dtest=SbomLicencePolicyTest -Dsurefire.failIfNoSpecifiedTests=false

checkpoint name: cli-setup
    java -cp "$(cat {{cli-classpath}}):java/icstudio-cli/target/classes" dev.icstudio.cli.IcStudioCli checkpoint --name {{name}}

resume-check name: cli-setup
    java -cp "$(cat {{cli-classpath}}):java/icstudio-cli/target/classes" dev.icstudio.cli.IcStudioCli resume-check --checkpoint {{name}}

ci: verify validate license-check truth
