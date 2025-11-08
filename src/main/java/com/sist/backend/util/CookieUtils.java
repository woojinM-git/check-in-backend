package com.sist.backend.util;

import java.net.URI;
import java.net.URISyntaxException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CookieUtils {

    private CookieUtils() {
    }

    public static String buildDomainAttribute(String frontendUrl) {
        if (frontendUrl == null || frontendUrl.isEmpty()) {
            return "";
        }

        try {
            URI uri = new URI(frontendUrl);
            String host = uri.getHost();

            if (host == null || host.isEmpty()) {
                host = extractHost(frontendUrl);
            }

            if (host == null || host.isEmpty() || isLocalHost(host)) {
                return "";
            }

            return "; Domain=" + host;
        } catch (URISyntaxException exception) {
            log.warn("프론트엔드 URL 파싱 실패: {}", frontendUrl, exception);
            return "";
        }
    }

    private static String extractHost(String frontendUrl) {
        String candidate = frontendUrl;

        int schemeIndex = candidate.indexOf("//");
        if (schemeIndex >= 0) {
            candidate = candidate.substring(schemeIndex + 2);
        }

        int pathIndex = candidate.indexOf('/');
        if (pathIndex >= 0) {
            candidate = candidate.substring(0, pathIndex);
        }

        int portIndex = candidate.indexOf(':');
        if (portIndex >= 0) {
            candidate = candidate.substring(0, portIndex);
        }

        return candidate;
    }

    private static boolean isLocalHost(String host) {
        return "localhost".equalsIgnoreCase(host)
            || "127.0.0.1".equals(host)
            || host.startsWith("127.")
            || "0.0.0.0".equals(host);
    }
}
