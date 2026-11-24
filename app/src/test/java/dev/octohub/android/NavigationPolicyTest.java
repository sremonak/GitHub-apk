package dev.octohub.android;

import org.junit.Test;
import static org.junit.Assert.*;

public class NavigationPolicyTest {
    @Test public void allowsGitHubPages() {
        assertTrue(NavigationPolicy.isInternal("https://github.com/"));
        assertTrue(NavigationPolicy.isInternal("https://github.com/login?return_to=%2Fsettings"));
        assertTrue(NavigationPolicy.isInternal("https://gist.github.com/user/123"));
        assertTrue(NavigationPolicy.isInternal("https://GITHUB.com:443/user/repo"));
    }
    @Test public void blocksSpoofedAndExternalOrigins() {
        String[] urls = {"https://github.com.attacker.test/", "https://attacker.test/github.com",
            "https://github.com@attacker.test/", "https://attacker@github.com/",
            "https://github.com:8443/", "https://example.github.io/", "https://api.github.com/",
            "https://github.com\\@attacker.test/", "https://github.com./", "//github.com/"};
        for (String url : urls) assertFalse(url, NavigationPolicy.isInternal(url));
    }
    @Test public void blocksUnsafeSchemesAndInvalidInput() {
        String[] urls = {null, "", "not a URL", "http://github.com", "javascript:alert(1)",
            "file:///data/data/dev.octohub.android/", "content://github.com/a", "intent://github.com/", "data:text/html,test"};
        for (String url : urls) {
            assertFalse(url, NavigationPolicy.isInternal(url));
            assertFalse(url, NavigationPolicy.isDownloadAllowed(url));
        }
    }
    @Test public void allowsOnlyGitHubDownloadHosts() {
        assertTrue(NavigationPolicy.isDownloadAllowed("https://github.com/user/repo/releases/download/v1/app.apk"));
        assertTrue(NavigationPolicy.isDownloadAllowed("https://codeload.github.com/user/repo/zip/main"));
        assertTrue(NavigationPolicy.isDownloadAllowed("https://release-assets.githubusercontent.com/asset"));
        assertTrue(NavigationPolicy.isDownloadAllowed("https://raw.githubusercontent.com/user/repo/main/file"));
        assertFalse(NavigationPolicy.isDownloadAllowed("https://githubusercontent.com.attacker.test/asset"));
        assertFalse(NavigationPolicy.isDownloadAllowed("https://attacker.test/asset"));
        assertFalse(NavigationPolicy.isDownloadAllowed("blob:https://github.com/some-id"));
        assertFalse(NavigationPolicy.isDownloadAllowed("http://codeload.github.com/user/repo/zip/main"));
    }
}
