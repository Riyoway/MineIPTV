package me.riyo.mineiptv;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class M3uParser {
    private static final Pattern GROUP = Pattern.compile("group-title=\\\"([^\\\"]*)\\\"", Pattern.CASE_INSENSITIVE);

    private M3uParser() {}

    public static List<Channel> parse(String text, String sourceUrl) {
        String normalized = text == null ? "" : text.replace("\r", "");

        // An HLS media/master manifest is itself a stream, not an IPTV channel list.
        if (normalized.contains("#EXT-X-TARGETDURATION") || normalized.contains("#EXT-X-STREAM-INF")) {
            return List.of(new Channel("Direct HLS stream", sourceUrl, ""));
        }

        List<Channel> channels = new ArrayList<>();
        String pendingName = null;
        String pendingGroup = "";

        for (String raw : normalized.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("#EXTINF:")) {
                int comma = line.indexOf(',');
                pendingName = comma >= 0 && comma + 1 < line.length()
                        ? line.substring(comma + 1).trim()
                        : "Untitled";
                Matcher matcher = GROUP.matcher(line);
                pendingGroup = matcher.find() ? matcher.group(1).trim() : "";
                continue;
            }

            if (!line.startsWith("#")) {
                if (pendingName != null) {
                    channels.add(new Channel(pendingName, resolve(sourceUrl, line), pendingGroup));
                    pendingName = null;
                    pendingGroup = "";
                }
            }
        }

        return channels;
    }

    private static String resolve(String sourceUrl, String value) {
        try {
            URI valueUri = URI.create(value);
            if (valueUri.isAbsolute()) return value;
            if (sourceUrl != null && (sourceUrl.startsWith("http://") || sourceUrl.startsWith("https://") || sourceUrl.startsWith("file:"))) {
                return URI.create(sourceUrl).resolve(valueUri).toString();
            }
        } catch (IllegalArgumentException ignored) {
        }
        return value;
    }
}
