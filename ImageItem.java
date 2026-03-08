package com.virtualcamera.app;

import java.util.Objects;

public class ImageItem {
    private String uri;
    private String name;

    public ImageItem() {}

    public ImageItem(String uri, String name) {
        this.uri = uri;
        this.name = name;
    }

    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImageItem)) return false;
        ImageItem item = (ImageItem) o;
        return Objects.equals(uri, item.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }
}
