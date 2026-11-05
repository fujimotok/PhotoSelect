package com.example.photoselect

import com.example.photoselect.data.model.MediaFile
import com.example.photoselect.data.model.MediaGroup
import com.example.photoselect.ui.gallery.GalleryUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GallerySelectionTest {

    private fun createMediaGroup(name: String): MediaGroup {
        return MediaGroup(
            baseName = name,
            primaryFile = MediaFile(
                uri = null,
                name = "$name.JPG",
                extension = "JPG",
                size = 1000L,
                lastModified = 100L
            )
        )
    }

    @Test
    fun galleryUiState_initialState_isNotSelectionMode() {
        val state = GalleryUiState()
        assertFalse(state.isSelectionMode)
        assertTrue(state.selectedGroups.isEmpty())
    }

    @Test
    fun galleryUiState_itemsSelected_isSelectionModeTrue() {
        val group1 = createMediaGroup("DSC0001")
        val state = GalleryUiState(selectedGroups = listOf(group1))

        assertTrue(state.isSelectionMode)
        assertEquals(1, state.selectedGroups.size)
    }

    @Test
    fun selectionLogic_multipleItemsSelected_supportsNItems() {
        val group1 = createMediaGroup("DSC0001")
        val group2 = createMediaGroup("DSC0002")
        val group3 = createMediaGroup("DSC0003")

        val selected = mutableListOf<MediaGroup>()

        // Add group1
        selected.add(group1)
        var state = GalleryUiState(selectedGroups = selected.toList())
        assertEquals(1, state.selectedGroups.size)

        // Add group2
        selected.add(group2)
        state = GalleryUiState(selectedGroups = selected.toList())
        assertEquals(2, state.selectedGroups.size)

        // Add group3 (N枚選択)
        selected.add(group3)
        state = GalleryUiState(selectedGroups = selected.toList())
        assertEquals(3, state.selectedGroups.size)

        // Remove group1
        selected.remove(group1)
        state = GalleryUiState(selectedGroups = selected.toList())
        assertEquals(2, state.selectedGroups.size)

        // Clear selection
        selected.clear()
        state = GalleryUiState(selectedGroups = selected.toList())
        assertFalse(state.isSelectionMode)
    }
}
