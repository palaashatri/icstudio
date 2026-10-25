package dev.icstudio.platform.evidence;

import dev.icstudio.platform.json.Json;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Evidence services for the programme: truth score, capability report, MCP lock, checkpoints,
 * and the repository licence posture.
 *
 * <p>The truth score is conservative by construction. It is the sum of milestone weight times
 * completion, and milestone completion is only raised by reviewed, accepted checkpoints.</p>
 */
public final class ProgrammeEvidence {
    public static final String TRUTH_PATH = ".project/truth.json";
    public static final String CAPABILITIES_PATH = ".project/capabilities.json";
    public static final String MCP_PROTOCOL_VERSION = "2025-11-25";
    public static final String MCP_LOCK_PATH = "toolchains/mcp.lock";
    public static final String CHECKPOINT_DIRECTORY = ".project/checkpoints";

    static final List<String> REQUIRED_CAPABILITIES = List.of(
            "CAP-INFRA-BOOT",
            "CAP-PROJ-DB",
            "CAP-MCP-BASE",
            "CAP-SIM-DC",
            "CAP-LAY-EDIT",
            "CAP-DRC",
            "CAP-LVS",
            "CAP-PEX-RC",
            "CAP-EM-3D",
            "CAP-PHOTONICS",
            "CAP-FLOW-OPENPDK",
            "CAP-RELEASE");

    static final List<String> REQUIRED_FILES = List.of(
            "AGENTS.md",
            "LICENSE",
            "pom.xml",
            "mvnw",
            "mvnw.cmd",
            ".mvn/wrapper/maven-wrapper.properties",
            CAPABILITIES_PATH,
            TRUTH_PATH,
            MCP_LOCK_PATH);

    private static final Pattern CHECKPOINT_NAME = Pattern.compile("CP-[A-Za-z0-9_-]{0,93}");

    private ProgrammeEvidence() {
    }

    public static double truthScore(Path root) throws IOException {
        var truth = readObject(root.resolve(TRUTH_PATH));
        if (!(truth.get("milestones") instanceof List<?> milestones)) {
            throw new IllegalStateException("truth file contains no milestone records");
        }
        double score = 0.0;
        int count = 0;
        for (Object item : milestones) {
            var milestone = asObject(item);
            double weight = number(milestone, "weight", "truth milestone is missing weight");
            double completion = number(milestone, "completion", "truth milestone is missing completion");
            if (!(completion >= 0.0 && completion <= 1.0)) {
                throw new IllegalStateException(
                        "milestone completion must be between 0 and 1, got " + completion);
            }
            score += weight * completion;
            count++;
        }
        if (count == 0) {
            throw new IllegalStateException("truth file contains no milestone records");
        }
        return score;
    }

    public static double reportedTruthScore(Path root) throws IOException {
        var truth = readObject(root.resolve(TRUTH_PATH));
        if (!(truth.get("reported_score") instanceof BigDecimal value)) {
            throw new IllegalStateException("truth file is missing reported_score");
        }
        return value.doubleValue();
    }

    public static void validateProjectState(Path root) throws IOException {
        for (String relative : REQUIRED_FILES) {
            if (!Files.isRegularFile(root.resolve(relative))) {
                throw new IllegalStateException(
                        "required project file is missing: " + root.resolve(relative));
            }
        }
        String capabilities = Files.readString(root.resolve(CAPABILITIES_PATH), StandardCharsets.UTF_8);
        for (String capability : REQUIRED_CAPABILITIES) {
            if (!capabilities.contains(capability)) {
                throw new IllegalStateException("capability state is missing " + capability);
            }
        }
        String lock = Files.readString(root.resolve(MCP_LOCK_PATH), StandardCharsets.UTF_8).strip();
        if (!lock.equals(MCP_PROTOCOL_VERSION)) {
            throw new IllegalStateException(
                    "MCP lock mismatch: expected " + MCP_PROTOCOL_VERSION + ", found " + lock);
        }
        double computed = truthScore(root);
        double reported = reportedTruthScore(root);
        if (Math.abs(computed - reported) > 1e-9) {
            throw new IllegalStateException(String.format(
                    Locale.ROOT, "truth score mismatch: computed %.2f, reported %.2f", computed, reported));
        }
        staticLicenceCheck(root);
    }

    /** Verifies the repository licence and that every reactor module inherits the MIT parent. */
    public static void staticLicenceCheck(Path root) throws IOException {
        String rootPom = Files.readString(root.resolve("pom.xml"), StandardCharsets.UTF_8);
        if (!rootPom.contains("<name>MIT License</name>")) {
            throw new IllegalStateException("workspace licence is not MIT");
        }
        for (String module : moduleDirectories(rootPom)) {
            String pom = Files.readString(root.resolve(module).resolve("pom.xml"), StandardCharsets.UTF_8);
            if (!pom.contains("<artifactId>icstudio-parent</artifactId>")) {
                throw new IllegalStateException(module + " does not inherit the workspace MIT parent");
            }
        }
    }

    public static String capabilityReportMarkdown(Path root) throws IOException {
        var capabilities = readObject(root.resolve(CAPABILITIES_PATH));
        if (!(capabilities.get("capabilities") instanceof List<?> records) || records.isEmpty()) {
            throw new IllegalStateException("capability state contains no capability records");
        }
        double score = truthScore(root);
        var report = new StringBuilder("# ICStudio capability report\n\n");
        report.append(String.format(Locale.ROOT, "**Truth score:** %.2f/100\n\n", score));
        report.append("| Capability | Description | Status | Tier |\n");
        report.append("|---|---|---:|---:|\n");
        for (Object item : records) {
            var record = asObject(item);
            String id = string(record, "id");
            String name = record.get("name") instanceof String value ? value : id;
            String status = record.get("status") instanceof String value ? value : "unknown";
            int tier = record.get("tier") instanceof BigDecimal value ? value.intValue() : 0;
            report.append("| `").append(id).append("` | ").append(name)
                    .append(" | ").append(status).append(" | ").append(tier).append(" |\n");
        }
        return report.toString();
    }

    /** Creates an immutable checkpoint manifest that binds the capability and truth state. */
    public static Path createCheckpoint(Path root, String name) throws IOException {
        validateCheckpointName(name);
        validateProjectState(root);
        Path directory = root.resolve(CHECKPOINT_DIRECTORY);
        Files.createDirectories(directory);
        Path destination = directory.resolve(name + ".json");
        byte[] capabilities = Files.readAllBytes(root.resolve(CAPABILITIES_PATH));
        byte[] truth = Files.readAllBytes(root.resolve(TRUTH_PATH));
        String manifest = "{\n"
                + "  \"schema_version\": 1,\n"
                + "  \"name\": \"" + escapeJson(name) + "\",\n"
                + "  \"created_unix\": " + Instant.now().getEpochSecond() + ",\n"
                + "  \"git_commit\": \"" + escapeJson(currentGitCommit(root)) + "\",\n"
                + "  \"capabilities_hash\": \"" + fnv1a64(capabilities) + "\",\n"
                + "  \"truth_hash\": \"" + fnv1a64(truth) + "\",\n"
                + String.format(Locale.ROOT, "  \"truth_score\": %.2f\n", truthScore(root))
                + "}\n";
        try {
            Files.writeString(
                    destination,
                    manifest,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
        } catch (java.nio.file.FileAlreadyExistsException error) {
            throw new IllegalStateException("checkpoint already exists: " + destination, error);
        }
        return destination;
    }

    public static void resumeCheck(Path root, String name) throws IOException {
        validateCheckpointName(name);
        validateProjectState(root);
        var checkpoint = readObject(root.resolve(CHECKPOINT_DIRECTORY).resolve(name + ".json"));
        String expectedCapabilities = string(checkpoint, "capabilities_hash");
        String expectedTruth = string(checkpoint, "truth_hash");
        String actualCapabilities = fnv1a64(Files.readAllBytes(root.resolve(CAPABILITIES_PATH)));
        String actualTruth = fnv1a64(Files.readAllBytes(root.resolve(TRUTH_PATH)));
        if (!expectedCapabilities.equals(actualCapabilities)) {
            throw new IllegalStateException("capability state differs from checkpoint " + name
                    + ": expected " + expectedCapabilities + ", got " + actualCapabilities);
        }
        if (!expectedTruth.equals(actualTruth)) {
            throw new IllegalStateException("truth state differs from checkpoint " + name
                    + ": expected " + expectedTruth + ", got " + actualTruth);
        }
    }

    /** FNV-1a 64-bit digest rendered as 16 lowercase hexadecimal digits. */
    public static String fnv1a64(byte[] bytes) {
        long hash = 0xcbf29ce484222325L;
        for (byte value : bytes) {
            hash ^= Byte.toUnsignedLong(value);
            hash *= 0x100000001b3L;
        }
        return String.format(Locale.ROOT, "%016x", hash);
    }

    static void validateCheckpointName(String name) {
        if (!name.startsWith("CP-")) {
            throw new IllegalArgumentException("checkpoint names must start with CP-");
        }
        if (!CHECKPOINT_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("checkpoint name contains unsafe characters");
        }
    }

    private static String currentGitCommit(Path root) {
        try {
            var process = new ProcessBuilder("git", "-C", root.toString(), "rev-parse", "HEAD")
                    .redirectErrorStream(false)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
            return process.waitFor() == 0 && !output.isEmpty() ? output : "unknown";
        } catch (IOException error) {
            return "unknown";
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return "unknown";
        }
    }

    static List<String> moduleDirectories(String rootPom) {
        var modules = new ArrayList<String>();
        var matcher = Pattern.compile("<module>([^<]+)</module>").matcher(rootPom);
        while (matcher.find()) {
            modules.add(matcher.group(1));
        }
        return modules;
    }

    private static Map<String, Object> readObject(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("failed to read " + path + ": no such file");
        }
        return asObject(Json.parse(Files.readString(path, StandardCharsets.UTF_8)));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalStateException("JSON value is not an object");
    }

    private static double number(Map<String, Object> object, String key, String missing) {
        if (!(object.get(key) instanceof BigDecimal value)) {
            throw new IllegalStateException(missing);
        }
        return value.doubleValue();
    }

    private static String string(Map<String, Object> object, String key) {
        if (!(object.get(key) instanceof String value)) {
            throw new IllegalStateException("missing JSON string field '" + key + "'");
        }
        return value;
    }

    static String escapeJson(String value) {
        return Json.escape(value);
    }
}
