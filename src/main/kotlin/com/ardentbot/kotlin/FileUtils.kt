package com.ardentbot.kotlin

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

@Throws(ZipException::class, IOException::class)
fun extractFolder(zipFile: String) {
    val BUFFER = 2048
    val file = File(zipFile)

    val zip = ZipFile(file)
    val newPath = zipFile.substring(0, zipFile.length - 4)

    File(newPath).mkdir()
    val zipFileEntries = zip.entries()

    // Process each entry
    while (zipFileEntries.hasMoreElements()) {
        // grab a zip file entry
        val entry = zipFileEntries.nextElement() as ZipEntry
        val currentEntry = entry.name
        val destFile = File(newPath, currentEntry)
        //destFile = new File(newPath, destFile.getName());
        val destinationParent = destFile.parentFile

        // create the parent directory structure if needed
        destinationParent.mkdirs()

        if (!entry.isDirectory) {
            val inputStream = BufferedInputStream(zip
                    .getInputStream(entry))
            var currentByte: Int
            // establish buffer for writing file
            val data = ByteArray(BUFFER)

            // write the current file to disk
            val fos = FileOutputStream(destFile)
            val dest = BufferedOutputStream(fos,
                    BUFFER)

            // read and write until last byte is encountered
            do {
                currentByte = inputStream.read(data, 0, BUFFER)
                if (currentByte != -1) dest.write(data, 0, currentByte)
            } while (currentByte != -1)

            dest.flush()
            dest.close()
            inputStream.close()
        }

        if (currentEntry.endsWith(".zip")) {
            // found a zip file, try to open
            extractFolder(destFile.absolutePath)
        }
    }
}
