package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateParserTest {

    @Test
    fun parsesRawManifestJson() {
        val info = AppUpdateParser.parse(
            """
            {
              "versionCode": 4,
              "versionName": "1.3.0",
              "title": "نسخة جديدة",
              "notes": "تحسينات مهمة",
              "apkUrl": "https://example.com/app-release.apk"
            }
            """.trimIndent(),
            "https://raw.githubusercontent.com/3bud-ZC/Mohamy/main/update/latest.json"
        )

        assertEquals(4, info.versionCode)
        assertEquals("1.3.0", info.versionName)
        assertEquals("https://example.com/app-release.apk", info.apkUrl)
        assertEquals("تحسينات مهمة", info.releaseNotes)
        assertEquals("نسخة جديدة", info.releaseTitle)
    }

    @Test
    fun parsesGitHubReleaseStyleJson() {
        val info = AppUpdateParser.parse(
            """
            {
              "tag_name": "v5",
              "name": "1.4.0",
              "body": "versionCode: 5\n- Bug fixes",
              "assets": [
                { "name": "readme.txt", "browser_download_url": "https://example.com/readme.txt" },
                { "name": "app-release.apk", "browser_download_url": "https://example.com/app-release.apk" }
              ]
            }
            """.trimIndent(),
            "https://api.github.com/repos/3bud-ZC/Mohamy/releases/latest"
        )

        assertEquals(5, info.versionCode)
        assertEquals("1.4.0", info.versionName)
        assertEquals("https://example.com/app-release.apk", info.apkUrl)
        assertTrue(info.releaseNotes.contains("Bug fixes"))
    }

    @Test
    fun parsesReleasePageUrl() {
        val info = AppUpdateParser.parse(
            """
            {
              "versionCode": 6,
              "versionName": "1.5.0",
              "apkUrl": "https://example.com/app-release.apk",
              "releasePageUrl": "https://github.com/3bud-ZC/Mohamy/releases/tag/v1.5.0"
            }
            """.trimIndent(),
            "https://raw.githubusercontent.com/3bud-ZC/Mohamy/main/update/latest.json"
        )

        assertEquals(6, info.versionCode)
        assertEquals("https://github.com/3bud-ZC/Mohamy/releases/tag/v1.5.0", info.releasePageUrl)
    }
}
