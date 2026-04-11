package com.nguyendevs.stkh.util

import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) =
    Toast.makeText(this, message, duration).show()

fun Context.showToastLong(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }
fun View.isVisible(): Boolean = visibility == View.VISIBLE

fun View.startAnimation(context: Context, animResId: Int) {
    startAnimation(AnimationUtils.loadAnimation(context, animResId))
}
