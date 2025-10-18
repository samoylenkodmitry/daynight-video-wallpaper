package com.archstarter.core.common.wallpaper

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import com.archstarter.core.common.R

/**
 * Provides the built-in wallpaper clips that ship with the application.
 */
fun defaultSlotVideos(context: Context): Map<DaySlot, Uri> = mapOf(
    DaySlot.MORNING to context.resourceUri(R.raw.wallpaper_morning),
    DaySlot.DAY to context.resourceUri(R.raw.wallpaper_day),
    DaySlot.EVENING to context.resourceUri(R.raw.wallpaper_evening),
    DaySlot.NIGHT to context.resourceUri(R.raw.wallpaper_night),
)

private fun Context.resourceUri(@RawRes resId: Int): Uri = Uri.Builder()
    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
    .authority(resources.getResourcePackageName(resId))
    .appendPath(resources.getResourceTypeName(resId))
    .appendPath(resources.getResourceEntryName(resId))
    .build()
