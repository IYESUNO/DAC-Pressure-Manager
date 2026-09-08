package com.iyes.dacpressuremanager.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubReleaseUpdateCheckerTest {
    private val checker = GitHubReleaseUpdateChecker()

    @Test
    fun comparesReleaseTagsWithAndroidVersionNames() {
        assertTrue(checker.compareVersions("android-v2.2.0", "2.1.2") > 0)
        assertTrue(checker.compareVersions("2.1.1", "2.1.2-debug") < 0)
        assertEquals(0, checker.compareVersions("android-v2.1.2", "2.1.2-debug"))
    }

    @Test
    fun parsesLatestReleaseWithoutRequestingAnApkDownload() {
        val result = checker.parseRelease(
            json = """{"tag_name":"android-v2.2.0","html_url":"https://example.test/release"}""",
            currentVersion = "2.1.2",
        )

        assertEquals(
            UpdateCheckResult.Available("2.2.0", "https://example.test/release"),
            result,
        )
    }
}
