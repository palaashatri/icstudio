package dev.icstudio.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.icstudio.core.ObjectId;
import dev.icstudio.core.Revision;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

final class ProjectRoundTripTest {
    private static final String RUST_COMPATIBLE_FIXTURE = """
            ICSTUDIO_PROJECT	1
            project	00000000000000010000000000000002	7	demo
            library	00000000000000100000000000000010	analog
            cell	00000000000000200000000000000020	analog	inverter
            view	00000000000000300000000000000030	analog	inverter	schematic	schematic
            view	00000000000000400000000000000040	analog	inverter	symbol	symbol
            """;

    @Test
    void rustM1FixtureRoundTripsByteForByte() {
        var project = ProjectCodec.decode(RUST_COMPATIBLE_FIXTURE);

        assertEquals(RUST_COMPATIBLE_FIXTURE, ProjectCodec.encode(project));
        assertEquals(
                "{\"schemaVersion\":1,\"projectId\":\"00000000000000010000000000000002\","
                        + "\"name\":\"demo\",\"revision\":7,\"libraries\":1,\"cells\":1,\"views\":2}",
                project.snapshot().summaryJson());
        assertEquals(new HierarchyCounts(1, 1, 2), project.snapshot().hierarchyCounts());
    }

    @Test
    void hierarchyIdsAreDeterministicForTheSameParentAndPath() {
        var projectId = ObjectId.parseHex("0123456789abcdef0fedcba987654321");
        var first = Project.restore(projectId, "demo", Revision.ZERO, new TreeMap<>())
                .withLibraryAdded("analog")
                .withCellAdded("analog", "inverter")
                .withViewAdded("analog", "inverter", "schematic", "schematic");

        var second = Project.restore(projectId, "demo", Revision.ZERO, new TreeMap<>())
                .withLibraryAdded("analog")
                .withCellAdded("analog", "inverter")
                .withViewAdded("analog", "inverter", "schematic", "schematic");

        assertEquals(first, second);
        assertEquals(
                first.libraries().get("analog").cells().get("inverter").views().get("schematic").id(),
                second.libraries().get("analog").cells().get("inverter").views().get("schematic").id());
    }

    @Test
    void serializationUsesSortedHierarchyOrder() {
        var base = Project.restore(
                ObjectId.parseHex("00000000000000000000000000000001"),
                "demo",
                Revision.ZERO,
                new TreeMap<>());
        var project = base.withLibraryAdded("zlib").withLibraryAdded("alib");

        var encoded = ProjectCodec.encode(project);
        assertTrue(encoded.indexOf("	alib
") < encoded.indexOf("	zlib
"));
    }

    @Test
    void hierarchyCollectionsAreImmutable() {
        var project = Project.create("demo").withLibraryAdded("analog");

        assertThrows(
                UnsupportedOperationException.class,
                () -> project.libraries().put(
                        "illegal",
                        new Library(
                                ObjectId.parseHex("00000000000000000000000000000001"),
                                "illegal",
                                new TreeMap<>())));
    }

    @Test
    void malformedReferencesAndEscapesAreRejected() {
        var missingLibrary = """
                ICSTUDIO_PROJECT	1
                project	00000000000000000000000000000001	0	demo
                cell	00000000000000000000000000000002	analog	inv
                """;
        assertThrows(IllegalArgumentException.class, () -> ProjectCodec.decode(missingLibrary));
        assertThrows(IllegalArgumentException.class, () -> ProjectCodec.unescapeField("bad\\q"));
        assertThrows(IllegalArgumentException.class, () -> Project.create("bad name"));
    }
}
