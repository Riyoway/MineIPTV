package me.riyo.mineiptv;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class FfmpegPlayer implements AutoCloseable {
    public static final int FRAME_WIDTH = 512;
    public static final int FRAME_HEIGHT = 288;
    private static final int FRAME_BYTES = FRAME_WIDTH * FRAME_HEIGHT * 4;

    //? if >=26.1 {
    private final DynamicTexture texture = new DynamicTexture("MineIPTV stream", FRAME_WIDTH, FRAME_HEIGHT, false);
    //?} else {
    /*private final DynamicTexture texture = new DynamicTexture(new NativeImage(FRAME_WIDTH, FRAME_HEIGHT, false));
    *///?}
    private final Identifier textureId = MineIptv.id("stream");
    private final AtomicReference<byte[]> latestFrame = new AtomicReference<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Process process;
    private Thread videoThread;
    private Thread audioThread;
    private volatile String status = "Idle";
    private volatile int volume = 80;

    public FfmpegPlayer() {
        //? if >=26.1 {
        Minecraft.getInstance().getTextureManager().register(textureId, texture);
        //?} else {
        /*Minecraft.getInstance().getTextureManager().register(textureId, texture);
        *///?}
    }

    public Identifier textureId() {
        return textureId;
    }

    public String status() {
        return status;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(100, volume));
    }

    public synchronized void play(String ffmpegPath, String url) throws IOException {
        stop();
        if (url == null || url.isBlank()) throw new IOException("Stream URL is empty");

        String executable = ffmpegPath == null || ffmpegPath.isBlank() ? "ffmpeg" : ffmpegPath;
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-hide_banner");
        command.add("-loglevel");
        command.add("quiet");
        command.add("-nostdin");
        command.add("-i");
        command.add(url.trim());
        command.add("-map");
        command.add("0:v:0");
        command.add("-vf");
        command.add("scale=" + FRAME_WIDTH + ":" + FRAME_HEIGHT + ":force_original_aspect_ratio=decrease,pad=" + FRAME_WIDTH + ":" + FRAME_HEIGHT + ":(ow-iw)/2:(oh-ih)/2:black");
        command.add("-r");
        command.add("20");
        command.add("-f");
        command.add("rawvideo");
        command.add("-pix_fmt");
        command.add("rgba");
        command.add("pipe:1");
        command.add("-map");
        command.add("0:a:0?");
        command.add("-f");
        command.add("s16le");
        command.add("-ac");
        command.add("2");
        command.add("-ar");
        command.add("48000");
        command.add("pipe:2");

        process = new ProcessBuilder(command).start();
        running.set(true);
        status = "Buffering...";

        videoThread = Thread.ofVirtual().name("MineIPTV-video").start(() -> videoLoop(process.getInputStream()));
        audioThread = Thread.ofVirtual().name("MineIPTV-audio").start(() -> audioLoop(process.getErrorStream()));
    }

    private void videoLoop(InputStream input) {
        try (BufferedInputStream in = new BufferedInputStream(input, FRAME_BYTES * 2)) {
            while (running.get()) {
                byte[] frame = readExactly(in, FRAME_BYTES);
                latestFrame.set(frame);
                status = "Playing";
            }
        } catch (EOFException ignored) {
            if (running.get()) status = "Stream ended";
        } catch (IOException e) {
            if (running.get()) status = "Video error: " + shortMessage(e);
        } finally {
            running.set(false);
        }
    }

    private void audioLoop(InputStream input) {
        AudioFormat format = new AudioFormat(48_000f, 16, 2, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

        try (BufferedInputStream in = new BufferedInputStream(input, 64 * 1024);
             SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info)) {
            line.open(format, 48_000 * 4 / 4);
            line.start();
            byte[] buffer = new byte[8192];
            int n;
            while (running.get() && (n = in.read(buffer)) >= 0) {
                applyVolume(buffer, n, volume / 100.0f);
                line.write(buffer, 0, n);
            }
            line.drain();
        } catch (Exception e) {
            if (running.get()) status = "Audio unavailable: " + shortMessage(e);
        }
    }

    public void uploadLatestFrame() {
        byte[] frame = latestFrame.getAndSet(null);
        if (frame == null) return;

        NativeImage image = texture.getPixels();
        if (image == null) return;

        int i = 0;
        for (int y = 0; y < FRAME_HEIGHT; y++) {
            for (int x = 0; x < FRAME_WIDTH; x++) {
                int r = frame[i] & 0xFF;
                int g = frame[i + 1] & 0xFF;
                int b = frame[i + 2] & 0xFF;
                int a = frame[i + 3] & 0xFF;
                //? if >=26.1 {
                image.setPixel(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                //?} else {
                /*image.setPixelRGBA(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                *///?}
                i += 4;
            }
        }
        texture.upload();
    }

    public synchronized void stop() {
        running.set(false);
        latestFrame.set(null);
        if (process != null) {
            process.destroy();
            if (process.isAlive()) process.destroyForcibly();
            process = null;
        }
        status = "Idle";
    }

    @Override
    public void close() {
        stop();
        Minecraft.getInstance().getTextureManager().release(textureId);
    }

    private static byte[] readExactly(InputStream in, int length) throws IOException {
        byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            int n = in.read(data, offset, length - offset);
            if (n < 0) throw new EOFException();
            offset += n;
        }
        return data;
    }

    private static void applyVolume(byte[] pcm, int length, float gain) {
        if (gain >= 0.999f) return;
        for (int i = 0; i + 1 < length; i += 2) {
            int sample = (short) ((pcm[i] & 0xFF) | (pcm[i + 1] << 8));
            int scaled = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(sample * gain)));
            pcm[i] = (byte) (scaled & 0xFF);
            pcm[i + 1] = (byte) ((scaled >>> 8) & 0xFF);
        }
    }

    private static String shortMessage(Exception e) {
        String m = e.getMessage();
        return m == null || m.isBlank() ? e.getClass().getSimpleName() : m;
    }
}
