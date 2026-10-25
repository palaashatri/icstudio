package dev.icstudio.ui.scene;

import dev.icstudio.project.Cell;
import dev.icstudio.project.Library;
import dev.icstudio.project.ProjectSnapshot;
import dev.icstudio.ui.theme.StudioTokens;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.types.Rect;
import io.github.humbleui.types.RRect;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Pixmap;

/**
 * Immutable scene model for the project hierarchy, painted with Skia.
 *
 * <p>The scene reads an immutable {@link ProjectSnapshot} and never edits authoritative state. The
 * engineering canvas is always a controlled solid surface.</p>
 */
public final class StudioScene {
    static final int CELL_WIDTH = 160;
    static final int CELL_HEIGHT = 56;
    static final int GAP = 16;
    static final int MARGIN = 24;
    static final int ROW_HEIGHT = 18;

    /** One drawn cell: its library column, row, and view count. */
    public record CellBox(String library, String cell, int column, int row, int views, Rect bounds) {
    }

    private final ProjectSnapshot snapshot;
    private final List<CellBox> boxes;

    public StudioScene(ProjectSnapshot snapshot) {
        this.snapshot = snapshot;
        this.boxes = layout(snapshot);
    }

    public ProjectSnapshot snapshot() {
        return snapshot;
    }

    public List<CellBox> boxes() {
        return boxes;
    }

    /** Preferred pixel size that keeps every cell visible without scrolling. */
    public int preferredWidth() {
        int columns = Math.max(1, snapshot.libraries().size());
        return MARGIN * 2 + columns * CELL_WIDTH + (columns - 1) * GAP;
    }

    public int preferredHeight() {
        int rows = 0;
        for (Library library : snapshot.libraries().values()) {
            rows = Math.max(rows, library.cells().size());
        }
        return MARGIN * 2 + Math.max(1, rows) * (CELL_HEIGHT + GAP);
    }

    /** Paints the scene onto a Skia canvas of the given size, clearing it to the canvas material first. */
    public void paint(Canvas canvas, float width, float height) {
        canvas.clear(argb(StudioTokens.canvasBackground()));
        try (var grid = new Paint().setColor(argb(StudioTokens.separator())).setStroke(true).setStrokeWidth(1f);
                var cellFill = new Paint().setColor(argb(StudioTokens.elevatedBackground())).setAntiAlias(true);
                var cellEdge = new Paint().setColor(argb(StudioTokens.accent())).setStroke(true)
                        .setStrokeWidth(1.5f).setAntiAlias(true)) {
            for (int column = 0; column * (CELL_WIDTH + GAP) + MARGIN < width; column++) {
                float x = MARGIN + column * (CELL_WIDTH + GAP) - GAP / 2f;
                if (x > 0 && x < width) {
                    canvas.drawLine(x, 0, x, height, grid);
                }
            }
            for (CellBox box : boxes) {
                var rounded = RRect.makeXYWH(box.bounds().getLeft(), box.bounds().getTop(),
                        box.bounds().getWidth(), box.bounds().getHeight(), StudioTokens.CORNER_RADIUS);
                canvas.drawRRect(rounded, cellFill);
                canvas.drawRRect(rounded, cellEdge);
            }
        }
    }

    /** Renders the scene into an opaque, premultiplied image using the native raster backend. */
    public BufferedImage render(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("scene size must be positive");
        }
        if (ByteOrder.nativeOrder() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalStateException("Skia scene readback requires a little-endian host");
        }
        var info = ImageInfo.makeN32Premul(width, height);
        try (Surface surface = Surface.makeRaster(info)) {
            paint(surface.getCanvas(), width, height);
            try (Pixmap pixmap = new Pixmap()) {
                if (!surface.peekPixels(pixmap)) {
                    throw new IllegalStateException("Skia surface pixels are unavailable");
                }
                return toBufferedImage(pixmap, width, height);
            }
        }
    }

    private static BufferedImage toBufferedImage(Pixmap pixmap, int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        int[] destination = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        ByteBuffer bytes = pixmap.getBuffer().duplicate().order(ByteOrder.LITTLE_ENDIAN);
        IntBuffer pixels = bytes.asIntBuffer();
        for (int row = 0; row < height; row++) {
            pixels.position(row * pixmap.getRowBytesAsPixels());
            pixels.get(destination, row * width, width);
        }
        return image;
    }

    static int argb(Color color) {
        return (color.getAlpha() << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    private static List<CellBox> layout(ProjectSnapshot snapshot) {
        var boxes = new ArrayList<CellBox>();
        int column = 0;
        for (Library library : snapshot.libraries().values()) {
            int row = 0;
            for (Cell cell : library.cells().values()) {
                int left = MARGIN + column * (CELL_WIDTH + GAP);
                int top = MARGIN + row * (CELL_HEIGHT + GAP);
                var bounds = Rect.makeXYWH(left, top, CELL_WIDTH, CELL_HEIGHT);
                boxes.add(new CellBox(library.name(), cell.name(), column, row, cell.views().size(), bounds));
                row++;
            }
            column++;
        }
        return List.copyOf(boxes);
    }
}
