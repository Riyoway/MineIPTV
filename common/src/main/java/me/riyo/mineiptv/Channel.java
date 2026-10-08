package me.riyo.mineiptv;

public record Channel(String name, String url, String group) {
    public Channel {
        if (name == null || name.isBlank()) name = "Untitled";
        if (url == null) url = "";
        if (group == null) group = "";
    }
}
