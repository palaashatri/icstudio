package dev.icstudio.project;

final class ProjectNames {
    private ProjectNames() {
    }

    static String component(String kind, String value) {
        if (value == null || value.isEmpty() || value.length() > 128) {
            throw new IllegalArgumentException(kind + " name must contain 1 to 128 characters");
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            boolean supported = character >= 'a' && character <= 'z'
                    || character >= 'A' && character <= 'Z'
                    || character >= '0' && character <= '9'
                    || character == '_'
                    || character == '-'
                    || character == '.';
            if (!supported) {
                throw new IllegalArgumentException(
                        kind + " name '" + value + "' contains unsupported characters");
            }
        }
        return value;
    }
}
