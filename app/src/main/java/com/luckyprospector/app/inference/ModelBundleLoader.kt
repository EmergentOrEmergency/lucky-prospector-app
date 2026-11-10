package com.luckyprospector.app.inference

import android.content.Context
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.security.MessageDigest

class ModelBundleLoader(private val context: Context) {
    fun load(assetDirectory: String = "models/lucky-prospector", useGpu: Boolean = true): Detector {
        val metadata = context.assets.open("$assetDirectory/model-metadata.json").bufferedReader().use { ModelMetadata.parse(it.readText()) }
        val provenanceBytes = context.assets.open("$assetDirectory/dataset-provenance.json").bufferedReader().use {
            it.readText().replace("\r\n", "\n").replace('\r', '\n').toByteArray(Charsets.UTF_8)
        }
        require(sha256(provenanceBytes) == metadata.datasetManifestSha256) { "Dataset provenance SHA-256 does not match approved metadata" }
        // The model is stored uncompressed (noCompress in build.gradle.kts), so it can be
        // memory-mapped: no heap copy of several MB, and the interpreter reads the same pages.
        val buffer = context.assets.openFd("$assetDirectory/model.tflite").use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                channel.map(FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.declaredLength)
            }
        }
        require(sha256(buffer.duplicate()) == metadata.artifactSha256) { "Model artifact SHA-256 does not match approved metadata" }
        buffer.order(ByteOrder.nativeOrder())
        return when (metadata.outputDecoder) {
            ModelMetadata.YOLO26_DECODER -> TfliteDetector(metadata, LiteRtModelRunner(buffer))
            ModelMetadata.YOLOX_DECODER -> YoloxTfliteDetector(metadata, LiteRtYoloxModelRunner(buffer, metadata, context.codeCacheDir.takeIf { useGpu }))
            else -> error("Metadata validation accepted an unsupported decoder")
        }
    }
}

internal fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

internal fun sha256(buffer: ByteBuffer): String =
    MessageDigest.getInstance("SHA-256").apply { update(buffer) }.digest().joinToString("") { "%02x".format(it) }
