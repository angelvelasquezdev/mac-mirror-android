package com.angelsoft.macmirror

import com.angelsoft.macmirror.ui.whatsnew.WhatsNewCatalog
import org.junit.Assert.*
import org.junit.Test

class WhatsNewCatalogTest {

    @Test
    fun testCatalogHasHighlightsForSupportedVersions() {
        assertTrue(WhatsNewCatalog.hasHighlights("1.2"))
        assertTrue(WhatsNewCatalog.hasHighlights("1.2.0"))
        assertTrue(WhatsNewCatalog.hasHighlights("1.2-beta.1"))

        assertFalse(WhatsNewCatalog.hasHighlights("1.0.0"))
        assertFalse(WhatsNewCatalog.hasHighlights("1.1.0"))
        assertFalse(WhatsNewCatalog.hasHighlights("2.0.0"))
    }

    @Test
    fun testCatalogHighlightsContent() {
        val release = WhatsNewCatalog.highlights("1.2.0")
        assertNotNull(release)
        assertEquals("1.2", release?.version)
        assertFalse(release!!.items.isEmpty())

        for (item in release.items) {
            assertTrue(item.id.isNotEmpty())
            assertNotNull(item.icon)
            assertTrue(item.titleRes != 0)
            assertTrue(item.descRes != 0)
        }
    }

    @Test
    fun testLatestRelease() {
        val latest = WhatsNewCatalog.latestRelease()
        assertNotNull(latest)
        assertEquals("1.2", latest.version)
        assertFalse(latest.items.isEmpty())
    }
}
