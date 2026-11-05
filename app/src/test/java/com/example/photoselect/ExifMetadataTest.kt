package com.example.photoselect

import com.example.photoselect.data.exif.ExifMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class ExifMetadataTest {

    @Test
    fun cameraDisplay_formatCorrectly() {
        val meta1 = ExifMetadata(make = "SONY", model = "ILCE-7M4")
        assertEquals("SONY ILCE-7M4", meta1.cameraDisplay)

        val meta2 = ExifMetadata(make = "Panasonic", model = "Panasonic DC-GH5")
        assertEquals("Panasonic DC-GH5", meta2.cameraDisplay)

        val meta3 = ExifMetadata()
        assertEquals("Unknown Camera", meta3.cameraDisplay)
    }

    @Test
    fun exposureDisplay_formatCorrectly() {
        val meta = ExifMetadata(
            iso = "100",
            shutterSpeed = "1/1000s",
            aperture = "f/2.8",
            focalLength = "50mm"
        )
        assertEquals("1/1000s  |  f/2.8  |  ISO 100  |  50mm", meta.exposureDisplay)
    }

    @Test
    fun resolutionDisplay_formatCorrectly() {
        val meta1 = ExifMetadata(width = 6000, height = 4000)
        assertEquals("6000 x 4000", meta1.resolutionDisplay)

        val meta2 = ExifMetadata()
        assertEquals("Unknown resolution", meta2.resolutionDisplay)
    }
}
