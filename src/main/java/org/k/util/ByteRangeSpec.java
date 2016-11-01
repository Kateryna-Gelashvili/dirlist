package org.k.util;

import com.google.common.base.Preconditions;

import org.k.exception.RangeNotSatisfiableException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ByteRangeSpec {
    private static final Pattern headerPattern = Pattern.compile("bytes=([0-9]*)-([0-9]*)");

    private final long start;
    private final long end;
    private final long contentSizeInBytes;

    private ByteRangeSpec(long start, long end, long contentSizeInBytes) {
        Preconditions.checkArgument(start >= 0);
        Preconditions.checkArgument(end >= start);
        Preconditions.checkArgument(contentSizeInBytes >= 0);

        if (end >= contentSizeInBytes) {
            throw new RangeNotSatisfiableException(start + "-" + end);
        }

        this.start = start;
        this.end = end;
        this.contentSizeInBytes = contentSizeInBytes;
    }

    public static ByteRangeSpec fromHeader(String header, long contentSizeInBytes) {
        Preconditions.checkNotNull(header);

        Matcher matcher = headerPattern.matcher(header);
        if (matcher.matches()) {
            String startStr = matcher.group(1);
            Long start = startStr != null && !"".equals(startStr) ?
                    Long.parseLong(startStr) : 0L;
            String endStr = matcher.group(2);
            Long end = endStr != null && !"".equals(endStr) ?
                    Long.parseLong(endStr) : contentSizeInBytes - 1;
            return new ByteRangeSpec(start, end, contentSizeInBytes);
        }
        throw new IllegalArgumentException("Invalid range header: " + header);
    }

    public long getStart() {
        return start;
    }

    public String buildContentRangeHeader() {
        return "bytes " + start + "-" + end + "/" + contentSizeInBytes;
    }

    public long buildContentLengthHeader() {
        return end - start + 1;
    }
}
