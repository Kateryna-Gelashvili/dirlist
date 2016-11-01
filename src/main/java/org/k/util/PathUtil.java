package org.k.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PathUtil {
    private static final Logger logger = LoggerFactory.getLogger(PathUtil.class);

    public static String extractPath(String prefix, String requestUri) {
        String dirPath;
        try {
            dirPath = URLDecoder.decode(StringUtils.replaceOnce(
                    requestUri, prefix, ""), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            logger.debug("Unsupported encoding exception while loading " + StringUtils.replaceOnce(
                    requestUri, prefix, ""));
            throw new RuntimeException(e);
        }
        return dirPath;
    }
}