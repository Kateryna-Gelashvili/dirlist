package org.k.data;

import com.google.common.base.Preconditions;

public class ExtractionProgress {
    private final String id;
    private final long totalSize;
    private final long extractedSize;

    public ExtractionProgress(String id, long totalSize, long extractedSize) {
        this.id = Preconditions.checkNotNull(id);
        Preconditions.checkArgument(totalSize > 0);
        Preconditions.checkArgument(extractedSize >= 0);
        Preconditions.checkArgument(extractedSize <= totalSize);
        this.totalSize = totalSize;
        this.extractedSize = extractedSize;
    }

    public String getId() {
        return id;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public long getExtractedSize() {
        return extractedSize;
    }
}
