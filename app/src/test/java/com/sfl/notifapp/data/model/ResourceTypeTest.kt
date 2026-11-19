package com.sfl.notifapp.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies the resource sub-category mapping used to filter Resources
 * notifications per-resource (wood / stone / iron / gold / crimstone /
 * sunstone / oil).
 */
class ResourceTypeTest {

    @Test
    fun `maps all seven resource timer titles`() {
        assertEquals(ResourceType.WOOD, ResourceType.fromTitle("Wood (Tree)"))
        assertEquals(ResourceType.STONE, ResourceType.fromTitle("Stone"))
        assertEquals(ResourceType.IRON, ResourceType.fromTitle("Iron"))
        assertEquals(ResourceType.GOLD, ResourceType.fromTitle("Gold"))
        assertEquals(ResourceType.CRIMSTONE, ResourceType.fromTitle("Crimstone"))
        assertEquals(ResourceType.SUNSTONE, ResourceType.fromTitle("Sunstone"))
        assertEquals(ResourceType.OIL, ResourceType.fromTitle("Oil Reserve"))
    }

    @Test
    fun `crimstone and sunstone never collapse into stone`() {
        assertEquals(ResourceType.CRIMSTONE, ResourceType.fromTitle("Crimstone"))
        assertEquals(ResourceType.SUNSTONE, ResourceType.fromTitle("Sunstone"))
    }

    @Test
    fun `non-resource titles return null so they fall back to category gate`() {
        assertNull(ResourceType.fromTitle("Wheat"))
        assertNull(ResourceType.fromTitle("Apple"))
        assertNull(ResourceType.fromTitle("Vegetable Medley"))
    }

    @Test
    fun `matching is case insensitive`() {
        assertEquals(ResourceType.WOOD, ResourceType.fromTitle("wood (tree)"))
        assertEquals(ResourceType.OIL, ResourceType.fromTitle("OIL RESERVE"))
    }

    @Test
    fun `all seven entries have unique keys`() {
        val keys = ResourceType.entries.map { it.key }
        assertEquals(keys.size, keys.distinct().size)
    }
}
