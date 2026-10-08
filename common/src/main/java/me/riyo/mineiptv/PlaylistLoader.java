package me.riyo.mineiptv;

import java.io.IOException;
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
            HttpRequest request = HttpRequest.newBuilder(URI.create(source))
                    .header("User-Agent", "MineIPTV/0.1")
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("HTTP " + response.statusCode());
            }
            text = response.body();
        } else {
            Path path = source.startsWith("file:") ? Path.of(URI.create(source)) : Path.of(source);
            text = Files.readString(path, StandardCharsets.UTF_8);
        }

        List<Channel> channels = M3uParser.parse(text, source);
        if (channels.isEmpty()) throw new IOException("No channels found in playlist");
        return channels;
    }
}
