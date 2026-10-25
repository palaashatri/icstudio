package dev.icstudio.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.icstudio.platform.json.Json;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PlatformAndJsonTest {
    @Test
    void operatingSystemNamesMapToPlatforms() {
        assertEquals(Platform.MACOS, Platform.fromOsName("Mac OS X"));
        assertEquals(Platform.WINDOWS, Platform.fromOsName("Windows 11"));
        assertEquals(Platform.LINUX, Platform.fromOsName("Linux"));
        assertEquals(Platform.OTHER, Platform.fromOsName("Plan9"));
        assertFalse(Platform.LINUX.nativeBackdropBlurVerified());
    }

    @Test
    void strictParserRejectsDuplicatesAndTrailingData() {
        var parsed = (Map<?, ?>) Json.parse("{\"a\":1}");
        assertEquals(new java.math.BigDecimal("1"), parsed.get("a"));
        var duplicate = assertThrows(IllegalArgumentException.class,
                () -> Json.parseTopLevelMembers("{\"id\":1,\"id\":2}"));
        assertEquals("duplicate JSON field 'id'", duplicate.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{} trailing"));
    }

    @Test
    void topLevelMembersKeepTheirRawSourceText() {
        var members = Json.parseTopLevelMembers("{\"id\":\"abc\",\"params\":{\"id\":999}}");

        assertEquals("\"abc\"", members.get("id").raw());
        assertEquals("{\"id\":999}", members.get("params").raw());
    }

    @Test
    void escapingFollowsJsonRules() {
        assertEquals("a\\\"b\\\\c\\n\\u0001", Json.escape("a\"b\\c\n\u0001"));
    }
}
