package com.luckyprospector.app.domain

object FindingValidator {
    const val MAX_WISH_LENGTH = 500

    fun validate(finding: Finding): List<String> = buildList {
        if (finding.privateImageUri.isBlank()) add("privateImageUri is required")
        if (finding.wish != null && finding.wish.length > MAX_WISH_LENGTH) {
            add("wish exceeds $MAX_WISH_LENGTH characters")
        }
        finding.location?.let { location ->
            if (location.latitude !in -90.0..90.0) add("latitude is out of range")
            if (location.longitude !in -180.0..180.0) add("longitude is out of range")
            if (location.accuracyMeters != null && location.accuracyMeters < 0f) {
                add("location accuracy cannot be negative")
            }
        }
        finding.prediction?.let { prediction ->
            if (prediction.confidence !in 0f..1f) add("confidence is out of range")
            if (prediction.modelVersion.isBlank()) add("modelVersion is required")
        }
    }
}

