package me.riyo.mineiptv.tv;

public enum TvSize {
    TV_1X1("tv_1x1", 1, 1),
    TV_2X1("tv_2x1", 2, 1),
    TV_2X2("tv_2x2", 2, 2),
    TV_3X2("tv_3x2", 3, 2),
    TV_4X3("tv_4x3", 4, 3);

    private final String id;
    private final int width;
    private final int height;

    TvSize(String id, int width, int height) {
        this.id = id;
        this.width = width;
        this.height = height;
    }

    public String id() { return id; }
    public int width() { return width; }
    public int height() { return height; }

    public static TvSize from(int width, int height) {
        for (TvSize size : values()) {
            if (size.width == width && size.height == height) return size;
        }
        return TV_1X1;
    }
}
