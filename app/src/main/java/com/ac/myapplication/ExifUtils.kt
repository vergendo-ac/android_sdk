package com.ac.myapplication

import android.location.Location
import androidx.exifinterface.media.ExifInterface
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.common.RationalNumber
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.tiff.TiffField
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants
import org.apache.commons.imaging.formats.tiff.constants.TiffDirectoryType
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoRational
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoShort
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


fun TiffOutputSet.writeOrientation(orientation: Int) {
    getOrCreateRootDirectory().apply {
        removeField(TiffTagConstants.TIFF_TAG_ORIENTATION)
        add(TiffTagConstants.TIFF_TAG_ORIENTATION, getRotationTag(orientation))
    }
}

fun TiffOutputSet.writeGravityVector(gravityVector: Array<Float>) {
    val vec = gravityVector.joinToString(separator = " ")

    getOrCreateRootDirectory().add(
        TiffTagConstants.TIFF_TAG_IMAGE_DESCRIPTION, vec
    )
}

fun TiffOutputSet.writeGps(location: Location) {
    apply {
        setGPSInDegrees(location.longitude, location.latitude)
        getOrCreateGPSDirectory().apply {
            // make sure to remove old value if present (this method will
            // not fail if the tag does not exist).

            updateRationalField(GpsTagConstants.GPS_TAG_GPS_ALTITUDE, location.altitude)
            updateRationalField(GpsTagConstants.GPS_TAG_GPS_DOP, location.accuracy.toDouble())
            updateRationalField(
                GpsTagConstants.GPS_TAG_GPS_IMG_DIRECTION,
                location.bearing.toDouble()
            )

            removeField(GpsTagConstants.GPS_TAG_GPS_DATE_STAMP)
            val dateStamp = SimpleDateFormat()
                .apply { applyLocalizedPattern("yyyy:MM:dd") }
                .format(location.time)
            add(GpsTagConstants.GPS_TAG_GPS_DATE_STAMP, dateStamp)

            removeField(GpsTagConstants.GPS_TAG_GPS_TIME_STAMP)


            val (hourRational, minuteRational, secondRational) = prepareRationalTime(location.time)

            add(
                GpsTagConstants.GPS_TAG_GPS_TIME_STAMP,
                hourRational,
                minuteRational,
                secondRational
            )
        }
    }
}

fun prepareRationalTime(locationTime: Long): List<RationalNumber> {
    val time = Calendar.getInstance().apply {
        timeInMillis = locationTime
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return with(time) {
        arrayOf(
            get(Calendar.HOUR_OF_DAY),
            get(Calendar.MINUTE),
            get(Calendar.SECOND)
        )
    }.map {
        RationalNumber.valueOf(it.toDouble())
    }
}


fun editExif(byteArray: ByteArray, command: TiffOutputSet.() -> Unit): ByteArray {
    val outputSet = byteArray.getExif()?.outputSet ?: TiffOutputSet()
    outputSet.command()

    val outputStream = ByteArrayOutputStream()
    ExifRewriter().updateExifMetadataLossless(byteArray, outputStream, outputSet)
    return outputStream.toByteArray()
}

fun TiffOutputDirectory.updateRationalField(tag: TagInfoRational, newValue: Double) {
    updateRationalField(tag, RationalNumber.valueOf(newValue)!!)
}

fun TiffOutputDirectory.updateRationalField(tag: TagInfoRational, newValue: RationalNumber) {
    removeField(tag)
    add(tag, newValue)
}


fun getLocation(byteArray: ByteArray): Option<Location> {
    val exif = byteArray.getExif()
    return exif?.let {
        Some(Location("ExifUtils").apply {
            latitude = exif.gps.latitudeAsDegreesNorth
            longitude = exif.gps.longitudeAsDegreesEast
        })
    } ?: None
}

fun ByteArray.getExif(): TiffImageMetadata? {
    // note that exif might be null if no Exif metadata is found.
    return Imaging.getMetadata(this)?.let { it as JpegImageMetadata }?.exif
}

fun ByteArray.getExifOrientation(): Int {
    try {
        val metadata = Imaging.getMetadata(this)
        val tiffImageMetadata = when (metadata) {
            is JpegImageMetadata -> metadata.exif
            is TiffImageMetadata -> metadata
            else -> return ExifInterface.ORIENTATION_UNDEFINED
        }

        var field: TiffField? = tiffImageMetadata.findField(TiffTagConstants.TIFF_TAG_ORIENTATION)
        return if (field != null) {
            field.intValue
        } else {
            val tagInfo = TagInfoShort(
                "Orientation",
                274,
                TiffDirectoryType.TIFF_DIRECTORY_IFD0
            ) // MAGIC_NUMBER
            field = tiffImageMetadata.findField(tagInfo)
            field?.intValue ?: ExifInterface.ORIENTATION_UNDEFINED
        }
    } catch (e: Exception) {
        return ExifInterface.ORIENTATION_UNDEFINED
    }
}

fun getRotationDegrees(rotation: Int): Float {
    return when (rotation) {
        8 -> -90f
        3 -> 180f
        6 -> 90f
        else -> 0f
    }
}

fun getRotationTag(rotation: Int): Short {
    return when (rotation) {
        270 -> 8
        180 -> 1
        90 -> 6
        else -> 3
    }
}

