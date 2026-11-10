package com.luckyprospector.app.inference

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class Yuv420ConverterTest {
    @Test fun `neutral chroma preserves grayscale and honors row stride`() {
        val y = YuvPlane(byteArrayOf(16, 235.toByte(), 99, 99, 81, 145.toByte(), 99, 99), 4, 1)
        val chroma = YuvPlane(byteArrayOf(128.toByte()), 1, 1)
        val rgb = Yuv420Converter.toRgb(2, 2, y, chroma, chroma)
        assertArrayEquals(
            byteArrayOf(0, 0, 0, 255.toByte(), 255.toByte(), 255.toByte(), 76, 76, 76, 150.toByte(), 150.toByte(), 150.toByte()),
            rgb,
        )
    }

    @Test fun `sha256 is stable for approved bundle verification`() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256("abc".toByteArray()))
    }
}
