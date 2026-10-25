package dev.icstudio.ui.scene;

import dev.icstudio.project.ProjectSnapshot;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import javax.swing.JPanel;

/**
 * Swing host for the Skia engineering canvas.
 *
 * <p>Swing owns windowing, focus, and accessibility; Skia paints only the scene surface. The panel
 * holds an immutable snapshot and reads it, so it cannot change authoritative state.</p>
 */
public final class SkiaScenePanel extends JPanel {
    private StudioScene scene;
    private BufferedImage frame;

    public SkiaScenePanel() {
        setOpaque(true);
        setFocusable(true);
        getAccessibleContext().setAccessibleName("Project hierarchy canvas");
    }

    public void bind(ProjectSnapshot snapshot) {
        this.scene = new StudioScene(Objects.requireNonNull(snapshot, "snapshot"));
        this.frame = null;
        revalidate();
        repaint();
    }

    public StudioScene scene() {
        return scene;
    }

    @Override
    public Dimension getPreferredSize() {
        if (scene == null) {
            return new Dimension(480, 320);
        }
        return new Dimension(scene.preferredWidth(), scene.preferredHeight());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (scene == null) {
            return;
        }
        int width = Math.max(1, getWidth());
        int height = Math.max(1, getHeight());
        if (frame == null || frame.getWidth() != width || frame.getHeight() != height) {
            frame = scene.render(width, height);
        }
        ((Graphics2D) graphics).drawImage(frame, 0, 0, null);
    }
}
