package co.onmind.hex4w.domain.models;

import java.time.LocalDateTime;

public class StoreItem {
    private final String key;
    private final Long size;
    private final LocalDateTime lastModified;
    private final String eTag;

    public StoreItem(String key, Long size, LocalDateTime lastModified, String eTag) {
        this.key = key;
        this.size = size;
        this.lastModified = lastModified;
        this.eTag = eTag;
    }

    public String key() { return key; }
    public Long size() { return size; }
    public LocalDateTime lastModified() { return lastModified; }
    public String eTag() { return eTag; }
}