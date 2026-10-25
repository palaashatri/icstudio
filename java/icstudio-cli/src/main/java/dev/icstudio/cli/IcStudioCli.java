package dev.icstudio.cli;

import dev.icstudio.command.AddCell;
import dev.icstudio.command.AddLibrary;
import dev.icstudio.command.AddView;
import dev.icstudio.command.ProjectCommand;
import dev.icstudio.command.TransactionalProjectService;
import dev.icstudio.core.Revision;
import dev.icstudio.platform.evidence.LicencePolicy;
import dev.icstudio.platform.evidence.ProgrammeEvidence;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Headless ICStudio command-line surface.
 *
 * <p>Every project read and mutation goes through the same transactional services used by the MCP
 * gateway and the desktop application. Errors are reported on standard error with exit status 1.</p>
 */
public final class IcStudioCli {
    static final String USAGE = "Usage: icstudio [--project-root PATH] <command> [options]\n\n"
            + "Commands:\n"
            + "  project create --path PATH --name NAME\n"
            + "  project show --path PATH\n"
            + "  project add-library --path PATH --name NAME --expected-revision N [--request-id ID] [--actor ACTOR]\n"
            + "  project add-cell --path PATH --library LIB --name NAME --expected-revision N [--request-id ID] [--actor ACTOR]\n"
            + "  project add-view --path PATH --library LIB --cell CELL --name NAME --kind KIND --expected-revision N [--request-id ID] [--actor ACTOR]\n"
            + "  validate\n"
            + "  truth\n"
            + "  capabilities [--output PATH]\n"
            + "  checkpoint --name CP-...\n"
            + "  resume-check --checkpoint CP-...\n"
            + "  license-check [--sbom PATH]\n";

    private IcStudioCli() {
    }

    public static void main(String[] args) {
        int status = run(args, System.out, System.err);
        System.exit(status);
    }

    /** Runs one command. Returns the process exit status; never calls {@link System#exit}. */
    public static int run(String[] arguments, PrintStream out, PrintStream err) {
        try {
            execute(new ArrayList<>(List.of(arguments)), out);
            return 0;
        } catch (CliUsageException error) {
            err.print("icstudio: " + error.getMessage() + "\n\n" + USAGE + "\n");
            return 1;
        } catch (IOException | RuntimeException error) {
            err.print("icstudio: " + error.getMessage() + "\n");
            return 1;
        }
    }

    private static void execute(List<String> args, PrintStream out) throws IOException {
        Path root = takeOption(args, "--project-root").map(Path::of).orElse(Path.of("."));
        if (args.isEmpty()) {
            throw usage("missing command");
        }
        String command = args.removeFirst();
        switch (command) {
            case "project" -> projectCommand(root, args, out);
            case "validate" -> {
                requireEmpty(args);
                ProgrammeEvidence.validateProjectState(root);
                out.print("project state: valid\n");
            }
            case "truth" -> {
                requireEmpty(args);
                double computed = ProgrammeEvidence.truthScore(root);
                double reported = ProgrammeEvidence.reportedTruthScore(root);
                out.print(String.format(Locale.ROOT, "ICStudio truth score: %.2f/100\n", computed));
                out.print(String.format(Locale.ROOT, "reported score: %.2f/100\n", reported));
                out.print("assessment: score counts only accepted checkpoints\n");
            }
            case "capabilities" -> {
                Path output = takeOption(args, "--output").map(Path::of).orElse(null);
                requireEmpty(args);
                String report = ProgrammeEvidence.capabilityReportMarkdown(root);
                if (output != null) {
                    Path destination = root.resolve(output);
                    Files.createDirectories(destination.toAbsolutePath().getParent());
                    Files.writeString(destination, report, StandardCharsets.UTF_8);
                } else {
                    out.print(report);
                }
            }
            case "checkpoint" -> {
                String name = takeOption(args, "--name").orElseThrow(() -> usage("checkpoint requires --name"));
                requireEmpty(args);
                out.print("created checkpoint " + ProgrammeEvidence.createCheckpoint(root, name) + "\n");
            }
            case "resume-check" -> {
                String checkpoint = takeOption(args, "--checkpoint")
                        .orElseThrow(() -> usage("resume-check requires --checkpoint"));
                requireEmpty(args);
                ProgrammeEvidence.resumeCheck(root, checkpoint);
                out.print("checkpoint " + checkpoint + ": compatible\n");
            }
            case "license-check" -> licenceCheck(root, args, out);
            case "help", "--help", "-h" -> out.print(USAGE + "\n");
            default -> throw usage("unknown command '" + command + "'");
        }
    }

    private static void projectCommand(Path root, List<String> args, PrintStream out) throws IOException {
        if (args.isEmpty()) {
            throw usage("project requires a subcommand");
        }
        String subcommand = args.removeFirst();
        Path path = takeOption(args, "--path").map(value -> resolve(root, value))
                .orElseThrow(() -> usage("project command requires --path"));

        switch (subcommand) {
            case "create" -> {
                String name = takeOption(args, "--name").orElseThrow(() -> usage("project create requires --name"));
                requireEmpty(args);
                var service = TransactionalProjectService.create(path, name);
                out.print(service.snapshot().summaryJson() + "\n");
            }
            case "show" -> {
                requireEmpty(args);
                out.print(TransactionalProjectService.open(path).snapshot().summaryJson() + "\n");
            }
            case "add-library" -> {
                String name = takeOption(args, "--name")
                        .orElseThrow(() -> usage("project add-library requires --name"));
                commitMutation(path, args, out, new AddLibrary(name));
            }
            case "add-cell" -> {
                String library = takeOption(args, "--library")
                        .orElseThrow(() -> usage("project add-cell requires --library"));
                String name = takeOption(args, "--name").orElseThrow(() -> usage("project add-cell requires --name"));
                commitMutation(path, args, out, new AddCell(library, name));
            }
            case "add-view" -> {
                String library = takeOption(args, "--library")
                        .orElseThrow(() -> usage("project add-view requires --library"));
                String cell = takeOption(args, "--cell").orElseThrow(() -> usage("project add-view requires --cell"));
                String name = takeOption(args, "--name").orElseThrow(() -> usage("project add-view requires --name"));
                String kind = takeOption(args, "--kind").orElseThrow(() -> usage("project add-view requires --kind"));
                commitMutation(path, args, out, new AddView(library, cell, name, kind));
            }
            default -> throw usage("unknown project command '" + subcommand + "'");
        }
    }

    private static void commitMutation(Path path, List<String> args, PrintStream out, ProjectCommand command)
            throws IOException {
        Revision expectedRevision = requiredRevision(args);
        String requestId = takeOption(args, "--request-id")
                .orElse("cli-" + ProcessHandle.current().pid() + "-" + expectedRevision.toUnsignedString());
        String actor = takeOption(args, "--actor").orElse("cli");
        requireEmpty(args);
        var service = TransactionalProjectService.open(path);
        service.commit(expectedRevision, requestId, actor, List.of(command));
        out.print(service.snapshot().summaryJson() + "\n");
    }

    private static void licenceCheck(Path root, List<String> args, PrintStream out) throws IOException {
        Path sbom = takeOption(args, "--sbom").map(value -> resolve(root, value)).orElse(null);
        requireEmpty(args);
        ProgrammeEvidence.staticLicenceCheck(root);
        if (sbom == null) {
            out.print("licence policy: MIT project code with a permissive dependency allowlist\n");
            return;
        }
        for (String line : LicencePolicy.check(sbom)) {
            out.print(line + "\n");
        }
    }

    private static Revision requiredRevision(List<String> args) {
        String value = takeOption(args, "--expected-revision")
                .orElseThrow(() -> usage("mutating project command requires --expected-revision"));
        try {
            return Revision.parseUnsigned(value);
        } catch (IllegalArgumentException error) {
            throw usage("invalid expected revision '" + value + "': " + error.getMessage());
        }
    }

    /**
     * Removes {@code name value} from the arguments. A value that is missing or begins with {@code --}
     * is an error, so an option can never consume the next option as its value.
     */
    static java.util.Optional<String> takeOption(List<String> args, String name) {
        int index = args.indexOf(name);
        if (index < 0) {
            return java.util.Optional.empty();
        }
        args.remove(index);
        if (index >= args.size() || args.get(index).startsWith("--")) {
            throw usage(name + " requires a value");
        }
        return java.util.Optional.of(args.remove(index));
    }

    private static void requireEmpty(List<String> args) {
        if (!args.isEmpty()) {
            throw usage("unexpected arguments: " + String.join(" ", args));
        }
    }

    private static Path resolve(Path root, String value) {
        Path path = Path.of(value);
        return path.isAbsolute() ? path : root.resolve(path);
    }

    private static CliUsageException usage(String reason) {
        return new CliUsageException(reason);
    }

    /** A malformed invocation; reported with the full usage text. */
    static final class CliUsageException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        CliUsageException(String message) {
            super(message);
        }
    }
}
