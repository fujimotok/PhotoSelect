package com.example.photoselect

import com.example.photoselect.data.model.MediaFile
import com.example.photoselect.data.model.MediaGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryFilterSortTest {

    private fun createMediaGroup(
        name: String,
        isVid: Boolean = false,
        hasRawFile: Boolean = false,
        modified: Long = 100L,
        fileSize: Long = 1000L
    ): MediaGroup {
        return MediaGroup(
            baseName = name,
            primaryFile = if (isVid) null else MediaFile(
                uri = null,
                name = "$name.JPG",
                extension = "JPG",
                size = fileSize,
                lastModified = modified
            ),
            videoFile = if (!isVid) null else MediaFile(
                uri = null,
                name = "$name.MP4",
                extension = "MP4",
                size = fileSize * 10,
                lastModified = modified
            ),
            rawFile = if (!hasRawFile) null else MediaFile(
                uri = null,
                name = "$name.ARW",
                extension = "ARW",
                size = fileSize * 20,
                lastModified = modified
            )
        )
    }

    @Test
    fun testFiltering_photosOnly() {
        val groups = listOf(
            createMediaGroup("photo1", isVid = false),
            createMediaGroup("video1", isVid = true),
            createMediaGroup("photo2", isVid = false)
        )

        val filtered = groups.filter { !it.isVideo }
        assertEquals(2, filtered.size)
        assertEquals("photo1", filtered[0].baseName)
        assertEquals("photo2", filtered[1].baseName)
    }

    @Test
    fun testFiltering_videosOnly() {
        val groups = listOf(
            createMediaGroup("photo1", isVid = false),
            createMediaGroup("video1", isVid = true)
        )
        val filtered = groups.filter { it.isVideo }
        assertEquals(1, filtered.size)
        assertEquals("video1", filtered[0].baseName)
    }

    @Test
    fun testFiltering_rawAvailable() {
        val groups = listOf(
            createMediaGroup("photo1", hasRawFile = false),
            createMediaGroup("photo2", hasRawFile = true)
        )
        val filtered = groups.filter { it.hasRaw }
        assertEquals(1, filtered.size)
        assertEquals("photo2", filtered[0].baseName)
    }

    @Test
    fun testSorting_dateDesc() {
        val groups = listOf(
            createMediaGroup("old", modified = 10L),
            createMediaGroup("new", modified = 200L),
            createMediaGroup("mid", modified = 100L)
        )
        val sorted = groups.sortedByDescending { it.lastModified }
        assertEquals("new", sorted[0].baseName)
        assertEquals("mid", sorted[1].baseName)
        assertEquals("old", sorted[2].baseName)
    }

    @Test
    fun testSorting_nameAsc() {
        val groups = listOf(
            createMediaGroup("B"),
            createMediaGroup("A"),
            createMediaGroup("C")
        )
        val sorted = groups.sortedBy { it.baseName.lowercase() }
        assertEquals("A", sorted[0].baseName)
        assertEquals("B", sorted[1].baseName)
        assertEquals("C", sorted[2].baseName)
    }
}
