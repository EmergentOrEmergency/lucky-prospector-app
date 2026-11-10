package com.luckyprospector.app.inference

internal data class YuvPlane(val bytes: ByteArray, val rowStride: Int, val pixelStride: Int)

internal object Yuv420Converter {
    fun toRgb(width: Int, height: Int, y: YuvPlane, u: YuvPlane, v: YuvPlane): ByteArray {
        require(width > 0 && height > 0)
        val rgb = ByteArray(width * height * 3)
        var destination = 0
        for (py in 0 until height) for (px in 0 until width) {
            val yy = y.sample(px, py).coerceAtLeast(16) - 16
            val uu = u.sample(px / 2, py / 2) - 128
            val vv = v.sample(px / 2, py / 2) - 128
            rgb[destination++] = ((298 * yy + 409 * vv + 128) shr 8).coerceIn(0, 255).toByte()
            rgb[destination++] = ((298 * yy - 100 * uu - 208 * vv + 128) shr 8).coerceIn(0, 255).toByte()
            rgb[destination++] = ((298 * yy + 516 * uu + 128) shr 8).coerceIn(0, 255).toByte()
        }
        return rgb
    }

    private fun YuvPlane.sample(x: Int, y: Int): Int {
        val index = y * rowStride + x * pixelStride
        require(index in bytes.indices) { "YUV plane is shorter than its stride metadata" }
        return bytes[index].toInt() and 0xff
    }
}
