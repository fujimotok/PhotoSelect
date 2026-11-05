package com.example.photoselect

import com.example.photoselect.data.model.MediaFile
import com.example.photoselect.data.model.MediaGroup
import com.example.photoselect.ui.gallery.GalleryUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryTrashTest {

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
    fun galleryUiState_trashScreenState_correctlyToggles() {
        var state = GalleryUiState()
        assertFalse(state.showTrashScreen)
        assertTrue(state.trashGroups.isEmpty())

        val trashedItem = createMediaGroup("DSC0001")
        state = state.copy(showTrashScreen = true, trashGroups = listOf(trashedItem))

        assertTrue(state.showTrashScreen)
        assertEquals(1, state.trashGroups.size)
        assertEquals("DSC0001", state.trashGroups[0].baseName)
    }

    @Test
    fun detailScreen_deletionIndexCalculation_middleItemRemoved() {
        val groups = mutableListOf(
            createMediaGroup("A"),
            createMediaGroup("B"),
            createMediaGroup("C")
        )

        // User deletes item at index 1 ("B")
        val deletedIndex = 1
        groups.removeAt(deletedIndex)

        assertEquals(2, groups.size)
        assertEquals("A", groups[0].baseName)
        assertEquals("C", groups[1].baseName)

        // Next target index should be coerced within 0..size-1
        val nextIndex = deletedIndex.coerceIn(0, groups.size - 1)
        assertEquals(1, nextIndex)
        assertEquals("C", groups[nextIndex].baseName)
    }

    @Test
    fun detailScreen_deletionIndexCalculation_lastItemRemoved() {
        val groups = mutableListOf(
            createMediaGroup("A"),
            createMediaGroup("B")
        )

        // User deletes item at index 1 ("B", last item)
        val deletedIndex = 1
        groups.removeAt(deletedIndex)

        assertEquals(1, groups.size)
        assertEquals("A", groups[0].baseName)

        val nextIndex = deletedIndex.coerceIn(0, groups.size - 1)
        assertEquals(0, nextIndex)
        assertEquals("A", groups[nextIndex].baseName)
    }

    @Test
    fun detailScreen_deletionIndexCalculation_allAllItemsRemoved() {
        val groups = mutableListOf(
            createMediaGroup("A")
        )

        // User deletes the only item
        groups.removeAt(0)

        assertTrue(groups.isEmpty())
    }

    @Test
    fun galleryUiState_trashLoadingProgress_updatesProgressAndMessage() {
        val initial = GalleryUiState(
            isLoading = false,
            loadingProgress = 1f,
            loadingMessage = "Complete"
        )

        val item1 = createMediaGroup("DSC0001")
        val item2 = createMediaGroup("DSC0002")
        val items = listOf(item1, item2)

        // Simulate starting trash move
        var state = initial.copy(
            isLoading = true,
            loadingProgress = 0f,
            loadingMessage = "Moving to Trash... (0/${items.size})"
        )

        assertTrue(state.isLoading)
        assertEquals(0f, state.loadingProgress, 0.001f)
        assertEquals("Moving to Trash... (0/2)", state.loadingMessage)

        // Progress for item 1
        state = state.copy(
            loadingProgress = 0.5f,
            loadingMessage = "Moving to Trash... (1/2)"
        )
        assertEquals(0.5f, state.loadingProgress, 0.001f)
        assertEquals("Moving to Trash... (1/2)", state.loadingMessage)

        // Progress for item 2
        state = state.copy(
            loadingProgress = 1f,
            loadingMessage = "Moving to Trash... (2/2)"
        )
        assertEquals(1f, state.loadingProgress, 0.001f)
        assertEquals("Moving to Trash... (2/2)", state.loadingMessage)
    }

    @Test
    fun galleryUiState_batchCopyMoveProgress_updatesProgressAndMessage() {
        val initial = GalleryUiState(
            isLoading = false,
            loadingProgress = 1f,
            loadingMessage = "Complete"
        )

        // Copy simulation
        var state = initial.copy(
            isLoading = true,
            loadingProgress = 0f,
            loadingMessage = "Copying items... (0/3)"
        )

        assertTrue(state.isLoading)
        assertEquals(0f, state.loadingProgress, 0.001f)
        assertEquals("Copying items... (0/3)", state.loadingMessage)

        state = state.copy(
            loadingProgress = 0.33f,
            loadingMessage = "Copying items... (1/3)"
        )
        assertEquals(0.33f, state.loadingProgress, 0.001f)
        assertEquals("Copying items... (1/3)", state.loadingMessage)

        // Move simulation
        state = initial.copy(
            isLoading = true,
            loadingProgress = 0f,
            loadingMessage = "Moving items... (0/3)"
        )

        assertTrue(state.isLoading)
        assertEquals(0f, state.loadingProgress, 0.001f)
        assertEquals("Moving items... (0/3)", state.loadingMessage)
    }
}
