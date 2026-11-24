package dev.octohub.android;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/** Explicit host boundaries: a URL containing the word 'github' is not necessarily GitHub. */
public final class NavigationPolicy {
    private NavigationPolicy() {}

    private static String secureHost(String url) {
        if (url == null) return "";
        try {
            URI uri = new URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getHost() == null) return "";
            return uri.getHost().toLowerCase(Locale.ROOT);
        } catch (URISyntaxException e) { return ""; }
    }

    public static boolean isInternal(String url) {
        String host = secureHost(url);
        return host.equals("github.com") || host.equals("www.github.com") || host.equals("gist.github.com");
    }

    public static boolean isDownloadAllowed(String url) {
        String host = secureHost(url);
        return isInternal(url) || host.equals("codeload.github.com")
                || host.equals("githubusercontent.com") || host.endsWith(".githubusercontent.com");
    }
}
