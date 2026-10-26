package com.qwerty.homenet.video;

public enum VideoFormat {
    MONOCHROME,
    COLOR;

    public static boolean colorAllowed(VideoFormat source, VideoFormat display) {
        return source == COLOR && display == COLOR;
    }

    public static int displayRgb(int rgb, VideoFormat source, VideoFormat display) {
        if (colorAllowed(source, display)) {
            return rgb & 16777215;
        } else {
            int gray = ((rgb >> 16 & 0xFF) * 299 + (rgb >> 8 & 0xFF) * 587 + (rgb & 0xFF) * 114) / 1000;
            return gray << 16 | gray << 8 | gray;
        }
    }
}
