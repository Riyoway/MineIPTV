package me.riyo.mineiptv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class MineIptvConfig {
    private static final Path PATH = Path.of("config", "mineiptv.properties");

    public String playlist = "";
    public String ffmpeg = "ffmpeg";
    public int volume = 80;

    public static MineIptvConfig load() {
        MineIptvConfig config = new MineIptvConfig();
        if (!Files.isRegularFile(PATH)) return config;

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(PATH)) {
            props.load(in);
            config.playlist = props.getProperty("playlist", "");
            config.ffmpeg = props.getProperty("ffmpeg", "ffmpeg");
            config.volume = clamp(parseInt(props.getProperty("volume", "80"), 80), 0, 100);
        } catch (IOException ignored) {
        }
        return config;
    }

    public void save() {
        Properties props = new Properties();
        props.setProperty("playlist", playlist == null ? "" : playlist);
        props.setProperty("ffmpeg", ffmpeg == null || ffmpeg.isBlank() ? "ffmpeg" : ffmpeg);
        props.setProperty("volume", Integer.toString(clamp(volume, 0, 100)));
        try {
            Files.createDirectories(PATH.getParent());
            try (OutputStream out = Files.newOutputStream(PATH)) {
                props.store(out, "MineIPTV settings");
            }
        } catch (IOException ignored) {
        }
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (NumberFormatException e) { return fallback; }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
