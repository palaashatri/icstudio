package dev.icstudio.app;

import dev.icstudio.command.TransactionalProjectService;
import dev.icstudio.project.ProjectSnapshot;
import dev.icstudio.ui.material.MaterialResolver;
import dev.icstudio.ui.material.StudioMaterial;
import dev.icstudio.ui.theme.StudioLookAndFeel;
import dev.icstudio.ui.workspace.ProjectWorkbench;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Desktop entry point. Usage: {@code icstudio-app [--project PATH]}.
 *
 * <p>Opens the project read-only through the same transactional service the CLI uses. In a headless
 * environment the application prints the authoritative summary instead of opening a window.</p>
 */
public final class ICStudio {
    private ICStudio() {
    }

    public static void main(String[] args) throws IOException {
        Path project = parseProject(args);
        ProjectSnapshot snapshot = project == null ? null : TransactionalProjectService.open(project).snapshot();

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println(snapshot == null ? "ICStudio: no project opened" : snapshot.summaryJson());
            return;
        }
        SwingUtilities.invokeLater(() -> showWindow(snapshot));
    }

    static Path parseProject(String[] args) {
        for (int index = 0; index < args.length; index++) {
            if ("--project".equals(args[index])) {
                if (index + 1 >= args.length) {
                    throw new IllegalArgumentException("--project requires a value");
                }
                return Path.of(args[index + 1]);
            }
        }
        return null;
    }

    private static void showWindow(ProjectSnapshot snapshot) {
        StudioLookAndFeel.install(StudioLookAndFeel.Appearance.LIGHT);
        var workbench = new ProjectWorkbench();
        if (snapshot != null) {
            workbench.bind(snapshot);
        }
        var resolution = MaterialResolver.resolve(StudioMaterial.CANVAS, false, false);
        if (resolution.opacity() != 1.0f) {
            throw new IllegalStateException("engineering canvas must remain opaque");
        }
        var frame = new JFrame("ICStudio");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(workbench);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
