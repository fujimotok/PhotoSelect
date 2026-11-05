package com.example.photoselect

import com.example.photoselect.data.exif.ExifThumbnailFetcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExifThumbnailTest {

    @Test
    fun isSupportedScheme_returnsTrueForContentAndFileSchemes() {
        assertTrue(ExifThumbnailFetcher.isSupportedScheme("content"))
        assertTrue(ExifThumbnailFetcher.isSupportedScheme("file"))
        assertFalse(ExifThumbnailFetcher.isSupportedScheme("http"))
        assertFalse(ExifThumbnailFetcher.isSupportedScheme("https"))
        assertFalse(ExifThumbnailFetcher.isSupportedScheme(null))
    }
}
