// src/main/java/corelinks/henry/util/ColorUtils.java
package corelinks.henry.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ColorUtils {

    private ColorUtils() {}

    public static Component colorize(String message) {
        if (message == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }

    public static String colorizeLegacy(String message) {
        if (message == null) return "";
        return message.replace("&", "§");
    }
}