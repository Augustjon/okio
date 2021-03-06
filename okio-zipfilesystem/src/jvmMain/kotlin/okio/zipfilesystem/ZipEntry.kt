/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package okio.zipfilesystem

import okio.ByteString
import okio.ExperimentalFileSystem
import okio.Path
import java.util.Calendar
import java.util.GregorianCalendar

/**
 * An entry within a zip file.
 *
 * An entry has attributes such as its name (which is actually a path) and the uncompressed size
 * of the corresponding data. An entry does not contain the data itself, but can be used as a key
 * with [ZipFileSystem.source].
 */
@ExperimentalFileSystem
internal class ZipEntry(
  /**
   * The name is actually a path and may contain `/` characters.
   *
   * *Security note:* Entry names can represent relative paths. `foo/../bar` or
   * `../bar/baz`, for example. If the entry name is being used to construct a filename
   * or as a path component, it must be validated or sanitized to ensure that files are not
   * written outside of the intended destination directory.
   *
   * At most 0xffff bytes when encoded.
   */
  val canonicalPath: Path,

  /**
   * Determine whether or not this `ZipEntry` is a directory.
   *
   * @return `true` when this `ZipEntry` is a directory, `false` otherwise.
   */
  val isDirectory: Boolean = false,

  /**
   * Returns the comment for this `ZipEntry`, or `null` if there is no comment.
   * If we're reading a zip file using `ZipInputStream`, the comment is not available.
   */
  val comment: String = "",

  /**
   * Gets the checksum for this `ZipEntry`.
   *
   * Needs to be a long to distinguish -1 ("not set") from the 0xffffffff CRC32.
   *
   * @return the checksum, or -1 if the checksum has not been set.
   *
   * @throws IllegalArgumentException if `value` is < 0 or > 0xFFFFFFFFL.
   */
  val crc: Long = -1L,

  /**
   * Gets the compressed size of this `ZipEntry`.
   *
   * @return the compressed size, or -1 if the compressed size has not been
   * set.
   */
  val compressedSize: Long = -1L,

  /**
   * Gets the uncompressed size of this `ZipEntry`.
   *
   * @return the uncompressed size, or `-1` if the size has not been
   * set.
   */
  val size: Long = -1L,

  /**
   * Gets the compression method for this `ZipEntry`.
   *
   * @return the compression method, either `DEFLATED`, `STORED`
   * or -1 if the compression method has not been set.
   */
  val compressionMethod: Int = -1,

  val time: Int = -1,

  val modDate: Int = -1,

  /**
   * Gets the extra information for this `ZipEntry`.
   *
   * @return a byte array containing the extra information, or `null` if
   * there is none.
   */
  val extra: ByteString = ByteString.EMPTY,

  val localHeaderRelOffset: Long = -1L
) {
  val children = mutableListOf<Path>()

  /**
   * Gets the last modification time of this `ZipEntry`.
   *
   * @return the last modification time as the number of milliseconds since
   * Jan. 1, 1970.
   */
  fun getTime(): Long {
    if (time != -1) {
      // Note that this inherits the local time zone.
      val cal = GregorianCalendar()
      cal.set(Calendar.MILLISECOND, 0)
      val year = 1980 + (modDate shr 9 and 0x7f)
      val month = modDate shr 5 and 0xf
      val day = modDate and 0x1f
      val hour = time shr 11 and 0x1f
      val minute = time shr 5 and 0x3f
      val second = time and 0x1f shl 1
      cal.set(year, month - 1, day, hour, minute, second)
      return cal.time.time
    }
    return -1
  }

  companion object {
    /** Zip entry state: Deflated. */
    const val DEFLATED = 8

    /** Zip entry state: Stored. */
    const val STORED = 0
  }
}
