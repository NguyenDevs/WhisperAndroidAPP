package com.nguyendevs.stkh.util

import android.content.Context
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast

/**
 * Extension functions tiện ích dùng chung toàn app.
 */

// Toast shorthand
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showToastLong(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

// View visibility helpers
fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.INVISIBLE
}

fun View.isVisible(): Boolean = visibility == View.VISIBLE

// Animation helpers
fun View.startAnimation(context: Context, animResId: Int) {
    val anim: Animation = AnimationUtils.loadAnimation(context, animResId)
    startAnimation(anim)
}

fun View.clearAnimation() {
    animation?.cancel()
    clearAnimation()
}
