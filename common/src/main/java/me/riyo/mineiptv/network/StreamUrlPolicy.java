package me.riyo.mineiptv.network;

import java.net.URI;

public final class StreamUrlPolicy {
    private StreamUrlPolicy() {}

    public static boolean allowed(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 4096) return false;
        try {
            URI uri = URI.create(raw.trim());
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null
                    && uri.getUserInfo() == null;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
