package com.nguyendevs.stkh.util

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.nguyendevs.stkh.R

/** Custom Snackbar (Toast replacement) */
fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    val context = this.context
    Snackbar.make(this, message, duration)
        .setBackgroundTint(ContextCompat.getColor(context, R.color.colorSurface))
        .setTextColor(ContextCompat.getColor(context, R.color.colorOnSurface))
        .setActionTextColor(ContextCompat.getColor(context, R.color.colorAccent))
        .show()
}

/** Extension to show premium message from Activity */
fun Activity.showPremiumMessage(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
    val root = this.findViewById<View>(android.R.id.content)
    root?.showSnackbar(message, duration)
}

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    if (this is Activity) {
        this.showPremiumMessage(message, if (duration == Toast.LENGTH_LONG) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT)
    } else {
        Toast.makeText(this, message, duration).show()
    }
}

fun Context.showToastLong(message: String) {
    showToast(message, Toast.LENGTH_LONG)
}

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }
fun View.isVisible(): Boolean = visibility == View.VISIBLE

fun View.startAnimation(context: Context, animResId: Int) {
    startAnimation(AnimationUtils.loadAnimation(context, animResId))
}
