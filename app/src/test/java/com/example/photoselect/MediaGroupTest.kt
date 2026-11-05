package com.example.photoselect

import com.example.photoselect.data.model.MediaFile
import com.example.photoselect.data.model.MediaGroup
import com.example.photoselect.data.settings.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaGroupTest {

    @Test
    fun defaultExtensions_areCorrect() {
        val primaryExts = SettingsRepository.DEFAULT_PRIMARY_EXTENSIONS
        val rawExts = SettingsRepository.DEFAULT_RAW_EXTENSIONS

        assertTrue(primaryExts.contains("JPG"))
        assertTrue(primaryExts.contains("JPEG"))
        assertTrue(primaryExts.contains("HIF"))
        assertTrue(primaryExts.contains("HEIC"))

        assertTrue(rawExts.contains("RW2"))
        assertTrue(rawExts.contains("ARW"))
        assertTrue(rawExts.contains("CR3"))
        assertTrue(rawExts.contains("NEF"))
        assertTrue(rawExts.contains("RAF"))
        assertTrue(rawExts.contains("ORF"))
        assertTrue(rawExts.contains("DNG"))
    }

    @Test
    fun mediaGroup_pairedRaw_hasRaw_isTrue() {
        val primaryFile = MediaFile(
            uri = null,
            name = "DSC0001.JPG",
            extension = "JPG",
            size = 1000L,
            lastModified = 100L
        )

        val rawFile = MediaFile(
            uri = null,
            name = "DSC0001.RW2",
            extension = "RW2",
            size = 5000L,
            lastModified = 100L
        )

        val group = MediaGroup(
            baseName = "DSC0001",
            primaryFile = primaryFile,
            rawFile = rawFile
        )

        assertTrue(group.hasRaw)
        assertFalse(group.isVideo)
        assertEquals("DSC0001.JPG", group.displayName)
    }

    @Test
    fun mediaGroup_video_isVideo_isTrue() {
        val videoFile = MediaFile(
            uri = null,
            name = "DSC0002.MP4",
            extension = "MP4",
            size = 10000L,
            lastModified = 200L
        )

        val group = MediaGroup(
            baseName = "DSC0002",
            videoFile = videoFile
        )

        assertFalse(group.hasRaw)
        assertTrue(group.isVideo)
        assertEquals("DSC0002.MP4", group.displayName)
    }
}
