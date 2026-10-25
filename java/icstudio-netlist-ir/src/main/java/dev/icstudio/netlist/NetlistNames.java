package dev.icstudio.netlist;

import dev.icstudio.core.TextLines;
import dev.icstudio.core.Utf8Order;

final class NetlistNames {
    private NetlistNames() {
    }

    static void validate(String kind, String value) {
        if (value == null || value.isEmpty()
                || Utf8Order.utf8Length(value) > 128
                || value.codePoints().anyMatch(code -> TextLines.isWhitespace(code) || Character.isISOControl(code))) {
            throw new IllegalArgumentException(kind + " name '" + value + "' is invalid");
        }
    }
}
