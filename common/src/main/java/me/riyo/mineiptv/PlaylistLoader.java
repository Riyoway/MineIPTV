package me.riyo.mineiptv;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public final class PlaylistLoader {
    private static final int MAX_PLAYLIST_BYTES = 8 * 1024 * 1024;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(12))
            .build();

    private PlaylistLoader() {}

    public static List<Channel> load(String input) throws IOException, InterruptedException {
        String source = input == null ? "" : input.trim();
        if (source.isEmpty()) throw new IOException("Playlist URL/path is empty");

        String text;
        if (source.startsWith("http://") || source.startsWith("https://")) {
            final URI uri;
            try {
                uri = URI.create(source);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid playlist URL", e);
            }

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("User-Agent", "MineIPTV/0.2")
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = HTTP.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("HTTP " + response.statusCode());
                }
                text = readLimitedUtf8(body);
            }
        } else {
            final Path path;
            try {
                path = source.startsWith("file:") ? Path.of(URI.create(source)) : Path.of(source);
            } catch (IllegalArgumentException e) {
                throw new IOException("Invalid playlist path", e);
            }
            try (InputStream in = Files.newInputStream(path)) {
                text = readLimitedUtf8(in);
            }
        }

        List<Channel> channels = M3uParser.parse(text, source);
        if (channels.isEmpty()) throw new IOException("No channels found in playlist");
        return channels;
    }

    private static String readLimitedUtf8(InputStream in) throws IOException {
        byte[] bytes = in.readNBytes(MAX_PLAYLIST_BYTES + 1);
        if (bytes.length > MAX_PLAYLIST_BYTES) {
            throw new IOException("Playlist exceeds 8 MiB limit");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
