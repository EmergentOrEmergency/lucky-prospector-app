package com.luckyprospector.app.domain

/** Requested analysis size. CameraX negotiates the actual size with all bound use cases. */
enum class SearchResolution(val width: Int, val height: Int) {
    STANDARD(640, 480),
    HIGH(1280, 960),
    FULL_HD(1920, 1080),
    /** Retained so a previously selected 4:3 resolution still loads correctly. */
    MAX(1920, 1440),
    /** Highest compatible camera stream, rather than a fixed pixel size. */
    CAMERA_MAX(0, 0);

    companion object {
        val DEFAULT = FULL_HD

        fun fromStored(name: String?): SearchResolution =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
