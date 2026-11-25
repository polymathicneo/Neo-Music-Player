package com.vibeplay.music.data.local.lyrics

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddedLyricsReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun readEmbeddedLyrics(contentUri: String, mimeType: String, filePath: String): String? {
        val uri = Uri.parse(contentUri)
        val ext = filePath.substringAfterLast('.', "").lowercase()
        val mime = mimeType.lowercase()

        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when {
                    ext == "mp3" || mime.contains("mpeg") || mime.contains("mp3") -> readId3v2Lyrics(stream)
                    ext == "flac" || mime.contains("flac") -> readFlacLyrics(stream)
                    ext in setOf("m4a", "mp4", "aac") || mime.contains("mp4") || mime.contains("m4a") -> readMp4Lyrics(stream)
                    else -> readId3v2Lyrics(stream)
                }
            }
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun readId3v2Lyrics(stream: InputStream): String? {
        val header = ByteArray(10)
        if (stream.readFully(header) < 10) return null
        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
            return null
        }

        val majorVersion = header[3].toInt() and 0xFF
        val tagSize = syncSafeInt(header, 6).coerceAtMost(8 * 1024 * 1024)
        if (tagSize <= 0) return null

        val data = ByteArray(tagSize)
        val read = stream.readFully(data)
        var pos = 0

        // Skip extended header if present
        val flags = header[5].toInt() and 0xFF
        if ((flags and 0x40) != 0 && read >= 4) {
            val extSize = if (majorVersion >= 4) syncSafeInt(data, 0) else bigEndianInt(data, 0)
            pos += extSize.coerceAtLeast(4)
        }

        while (pos + 10 <= read) {
            val frameId = String(data, pos, 4, StandardCharsets.ISO_8859_1)
            if (frameId.all { it == '\u0000' }) break

            val frameSize = if (majorVersion >= 4) {
                syncSafeInt(data, pos + 4)
            } else {
                bigEndianInt(data, pos + 4)
            }
            pos += 10
            if (frameSize <= 0 || pos + frameSize > read) break

            if (frameId == "USLT") {
                val frameBytes = data.copyOfRange(pos, pos + frameSize)
                decodeUsltFrame(frameBytes)?.let { return it }
            }
            pos += frameSize
        }
        return null
    }

    private fun decodeUsltFrame(frame: ByteArray): String? {
        if (frame.size < 5) return null
        val encodingByte = frame[0].toInt() and 0xFF
        val charset: Charset = when (encodingByte) {
            0 -> StandardCharsets.ISO_8859_1
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            3 -> StandardCharsets.UTF_8
            else -> StandardCharsets.UTF_8
        }
        // Bytes 1..3 are 3-char language code, followed by content descriptor terminated by null
        var idx = 4
        if (encodingByte == 1 || encodingByte == 2) {
            while (idx + 1 < frame.size) {
                if (frame[idx] == 0.toByte() && frame[idx + 1] == 0.toByte()) {
                    idx += 2
                    break
                }
                idx += 2
            }
        } else {
            while (idx < frame.size) {
                if (frame[idx] == 0.toByte()) {
                    idx += 1
                    break
                }
                idx++
            }
        }
        if (idx >= frame.size) return null
        return String(frame, idx, frame.size - idx, charset).trim().takeIf { it.isNotEmpty() }
    }

    private fun readFlacLyrics(stream: InputStream): String? {
        val marker = ByteArray(4)
        if (stream.readFully(marker) < 4) return null
        if (String(marker, StandardCharsets.US_ASCII) != "fLaC") return null

        var isLast = false
        while (!isLast) {
            val blockHeader = ByteArray(4)
            if (stream.readFully(blockHeader) < 4) break
            val first = blockHeader[0].toInt() and 0xFF
            isLast = (first and 0x80) != 0
            val blockType = first and 0x7F
            val length = ((blockHeader[1].toInt() and 0xFF) shl 16) or
                ((blockHeader[2].toInt() and 0xFF) shl 8) or
                (blockHeader[3].toInt() and 0xFF)

            if (length <= 0 || length > 4 * 1024 * 1024) break

            if (blockType == 4) {
                // VORBIS_COMMENT
                val block = ByteArray(length)
                val read = stream.readFully(block)
                return parseVorbisCommentLyrics(block, read)
            } else {
                stream.skipBytes(length.toLong())
            }
        }
        return null
    }

    private fun parseVorbisCommentLyrics(data: ByteArray, size: Int): String? {
        var pos = 0
        if (pos + 4 > size) return null
        val vendorLen = littleEndianInt(data, pos)
        pos += 4 + vendorLen.coerceAtLeast(0)
        if (pos + 4 > size) return null
        val commentCount = littleEndianInt(data, pos)
        pos += 4

        repeat(commentCount.coerceAtMost(512)) {
            if (pos + 4 > size) return null
            val len = littleEndianInt(data, pos)
            pos += 4
            if (len < 0 || pos + len > size) return null
            val entry = String(data, pos, len, StandardCharsets.UTF_8)
            pos += len
            val eqIdx = entry.indexOf('=')
            if (eqIdx > 0) {
                val key = entry.substring(0, eqIdx).uppercase()
                if (key == "LYRICS" || key == "UNSYNCEDLYRICS" || key == "SYNCEDLYRICS") {
                    return entry.substring(eqIdx + 1).trim()
                }
            }
        }
        return null
    }

    private fun readMp4Lyrics(stream: InputStream): String? {
        val bytes = stream.readUpTo(2 * 1024 * 1024)
        val marker = byteArrayOf(0xA9.toByte(), 'l'.code.toByte(), 'y'.code.toByte(), 'r'.code.toByte())
        val idx = indexOfBytes(bytes, marker)
        if (idx < 0 || idx + 16 >= bytes.size) return null

        // Expect `data` sub-atom right after `©lyr`
        val dataSize = bigEndianInt(bytes, idx + 4)
        val dataTag = String(bytes, idx + 8, 4, StandardCharsets.US_ASCII)
        if (dataTag != "data" || dataSize <= 16) return null
        val textStart = idx + 20
        val textLen = (dataSize - 16).coerceAtMost(bytes.size - textStart)
        if (textLen <= 0) return null
        return String(bytes, textStart, textLen, StandardCharsets.UTF_8).trim()
    }

    private fun indexOfBytes(haystack: ByteArray, needle: ByteArray): Int {
        outer@ for (i in 0..(haystack.size - needle.size)) {
            for (j in needle.indices) {
                if (haystack[i + j] != needle[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun syncSafeInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }

    private fun bigEndianInt(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun littleEndianInt(bytes: ByteArray, offset: Int): Int {
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun InputStream.readFully(buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val read = read(buffer, total, buffer.size - total)
            if (read <= 0) break
            total += read
        }
        return total
    }

    private fun InputStream.skipBytes(count: Long) {
        var remaining = count
        val scratch = ByteArray(4096)
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped > 0) {
                remaining -= skipped
            } else {
                val read = read(scratch, 0, minOf(scratch.size.toLong(), remaining).toInt())
                if (read <= 0) break
                remaining -= read
            }
        }
    }

    private fun InputStream.readUpTo(maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (total < maxBytes) {
            val toRead = minOf(buf.size, maxBytes - total)
            val read = read(buf, 0, toRead)
            if (read <= 0) break
            out.write(buf, 0, read)
            total += read
        }
        return out.toByteArray()
    }
}
