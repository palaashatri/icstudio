package dev.icstudio.ui.scene;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.command.AddCell;
import dev.icstudio.command.AddLibrary;
import dev.icstudio.command.AddView;
import dev.icstudio.command.TransactionalProjectService;
import dev.icstudio.core.Revision;
import dev.icstudio.project.ProjectSnapshot;
import dev.icstudio.project.ProjectStore;
import dev.icstudio.ui.theme.StudioTokens;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StudioSceneRenderTest {
    @TempDir
    Path temporary;

    @Test
    void renderedCanvasIsOpaqueAndUsesTheCanvasMaterial() throws Exception {
        var scene = new StudioScene(sampleSnapshot());
        BufferedImage image = scene.render(64, 48);

        assertEquals(64, image.getWidth());
        assertEquals(48, image.getHeight());
        assertEquals(StudioScene.argb(StudioTokens.canvasBackground()) | 0xFF000000, image.getRGB(0, 0));
        assertEquals(0xFF, image.getRGB(0, 0) >>> 24);
    }

    @Test
    void sceneLaysOutOneBoxPerCellAndReadsOnlyTheSnapshot() throws Exception {
        var snapshot = sampleSnapshot();
        var scene = new StudioScene(snapshot);

        assertEquals(2, scene.boxes().size());
        assertEquals(List.of("buffer", "inverter"), scene.boxes().stream().map(StudioScene.CellBox::cell).toList());
        assertTrue(scene.preferredWidth() > 0 && scene.preferredHeight() > 0);
        assertEquals(snapshot, scene.snapshot());
    }

    private ProjectSnapshot sampleSnapshot() throws Exception {
        ProjectStore.create(temporary, "scene");
        var service = TransactionalProjectService.open(temporary);
        service.commit(Revision.ZERO, "scene-1", "test", List.of(new AddLibrary("analog")));
        service.commit(new Revision(1), "scene-2", "test", List.of(new AddCell("analog", "inverter")));
        service.commit(new Revision(2), "scene-3", "test", List.of(new AddCell("analog", "buffer")));
        service.commit(new Revision(3), "scene-4", "test", List.of(new AddView("analog", "inverter", "schematic", "schematic")));
        return service.snapshot();
    }
}
