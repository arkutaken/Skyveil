package name.skyveil.client.update;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class GitHubUpdateManagerTest {
    private static final String DIGEST="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test void parsesOnlyTheVersionedRepositoryJarAndChangeNotes(){
        var release=GitHubUpdateManager.parseRelease(JsonParser.parseString("""
            {"tag_name":"v1.10.4","html_url":"https://github.com/arkutaken/Skyveil/releases/tag/v1.10.4","draft":false,"prerelease":false,
             "body":"## Changes\\n- First change\\n- Second `change`\\n### Requirements\\n- Java 25",
             "assets":[{"name":"sources.zip","size":12,"digest":"sha256:%s","browser_download_url":"https://github.com/arkutaken/Skyveil/releases/download/v1.10.4/sources.zip"},
                       {"name":"Skyveil-1.10.4.jar","size":2721000,"digest":"sha256:%s","browser_download_url":"https://github.com/arkutaken/Skyveil/releases/download/v1.10.4/Skyveil-1.10.4.jar"}]}
            """.formatted(DIGEST,DIGEST)).getAsJsonObject());
        assertEquals("1.10.4",release.version());assertEquals(java.util.List.of("First change","Second change"),release.notes());assertEquals("Skyveil-1.10.4.jar",release.asset().name());
    }

    @Test void comparesSemanticVersionsNumerically(){
        assertEquals(1,GitHubUpdateManager.compareVersions("1.10.4","1.9.99"));assertEquals(0,GitHubUpdateManager.compareVersions("v1.10.4","1.10.4"));assertEquals(-1,GitHubUpdateManager.compareVersions("1.10.3","1.10.4"));
    }

    @Test void rejectsAssetsOutsideTheOwnedRepository(){
        assertThrows(IllegalArgumentException.class,()->GitHubUpdateManager.parseRelease(JsonParser.parseString("""
            {"tag_name":"v9.0.0","html_url":"https://github.com/arkutaken/Skyveil/releases/tag/v9.0.0","draft":false,"prerelease":false,
             "assets":[{"name":"Skyveil-9.0.0.jar","size":100,"digest":"sha256:%s","browser_download_url":"https://evil.example/Skyveil-9.0.0.jar"}]}
            """.formatted(DIGEST)).getAsJsonObject()));
    }
}
