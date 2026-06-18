package edu.cmu.androidstuco.clongdict.util

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference
import java.util.concurrent.Callable

interface Toaster {
    fun logToast(message: String) {
        showToastSync(message)
    }

    public enum class ToasterConfig {
        SHORT_TOAST,
        LONG_TOAST,
        SHORT_SNACKBAR,
        LONG_SNACKBAR,
        INDEFINITE_SNACKBAR,
        LONG_FULL_MESSAGE,
    }

    companion object {
        private val mainHandler = Handler(Looper.getMainLooper())

        private var toastApplicationContext: Context? = null
        private var snackbarActivityRef = WeakReference<Activity?>(null)

        /**
         * Call once from [android.app.Application.onCreate] so [showToastSync] can show Toasts
         * from any thread.
         */
        @JvmStatic
        fun bindToastContext(context: Context) {
            toastApplicationContext = context.applicationContext
        }

        /**
         * Prefer registering [android.app.Application.ActivityLifecycleCallbacks] instead of calling
         * this directly (see `ClongDictApplication`).
         */
        @JvmStatic
        fun bindSnackbarActivity(activity: Activity) {
            snackbarActivityRef = WeakReference(activity)
        }

        @JvmStatic
        fun clearSnackbarActivity(activity: Activity) {
            if (snackbarActivityRef.get() === activity) {
                snackbarActivityRef.clear()
            }
        }

        private fun Activity.canShowSnackbar(): Boolean {
            if (isFinishing) return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) !isDestroyed else true
        }

        private fun showFullMessageDialog(activity: Activity, message: String) {
            if (!activity.canShowSnackbar()) return
            val scroll = ScrollView(activity)
            val tv = TextView(activity).apply {
                text = message
                val pad = (24 * resources.displayMetrics.density).toInt()
                setPadding(pad, pad, pad, pad)
                textSize = 14f
                setTextIsSelectable(true)
            }
            scroll.addView(
                tv,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ),
            )
            AlertDialog.Builder(activity)
                .setTitle("Details")
                .setView(scroll)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }

        private fun showSnackbar(message: String, length: Int, ctx: Context): Snackbar? {
            val activity = snackbarActivityRef.get()
            if (activity != null && activity.canShowSnackbar()) {
                val root = activity.findViewById<View>(android.R.id.content)
                if (root != null) {
                    val s = Snackbar.make(root, message, length)
                    return s
                } else {
                    Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                    return null
                }
            } else {
                Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                return null
            }
        }

        /**
         * Show a Toast from any thread (background executor, JNI attach thread, etc.).
         * No-op if [bindToastContext] has not run yet.
         *
         * [ToasterConfig.LONG_FULL_MESSAGE] shows a long Snackbar with a **FULL** action when an
         * activity is registered via lifecycle callbacks; otherwise falls back to a long Toast.
         */
        @JvmStatic
        fun showToastCallable(message: String, config: ToasterConfig = ToasterConfig.SHORT_TOAST): Callable<Snackbar?>? {
            val ctx = toastApplicationContext ?: return null
            return Callable {
                when (config) {
                    ToasterConfig.LONG_FULL_MESSAGE -> {
                        val s = showSnackbar(message, Snackbar.LENGTH_LONG, ctx)
                            // Append a "FULL" action to the Snackbar
                            ?.setAction("FULL") {
                                showFullMessageDialog(snackbarActivityRef.get()!!, message)
                            }
                        s?.show()
                        s
                    }
                    ToasterConfig.LONG_SNACKBAR -> {
                        val s = showSnackbar(message, Snackbar.LENGTH_LONG, ctx)
                        s?.show()
                        s
                    }
                    ToasterConfig.INDEFINITE_SNACKBAR -> {
                        val s = showSnackbar(message, Snackbar.LENGTH_INDEFINITE, ctx)
                        s?.show()
                        s
                    }
                    ToasterConfig.SHORT_SNACKBAR -> {
                        val s = showSnackbar(message, Snackbar.LENGTH_SHORT, ctx)
                        s?.show()
                        s
                    }
                    ToasterConfig.LONG_TOAST -> {
                        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
                        null
                    }
                    ToasterConfig.SHORT_TOAST -> {
                        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                        null
                    }
                }
            }
        }

        @JvmStatic
        fun showToastSync(message: String, config: ToasterConfig = ToasterConfig.SHORT_TOAST): Snackbar? {
            val callable = showToastCallable(message, config) ?: return null
            if (Looper.myLooper() == Looper.getMainLooper()) {
                return callable.call()
            } else {
                mainHandler.post { callable.call() }
                return null
            }
        }

        /**
         * Maps Android [Toast] length constants to [ToasterConfig]: [Toast.LENGTH_SHORT],
         * [Toast.LENGTH_LONG], and values **greater than** [Toast.LENGTH_LONG] (legacy “long +
         * snackbar with FULL”) to [ToasterConfig.LONG_FULL_MESSAGE].
         */
        @JvmStatic
        fun showToastSync(message: String, length: Int): Snackbar? {
            val config = when {
                length < 0 -> throw IllegalArgumentException("Invalid Toast length: $length")
                length <= Toast.LENGTH_SHORT -> ToasterConfig.SHORT_TOAST
                length == Toast.LENGTH_LONG -> ToasterConfig.LONG_TOAST
                else -> ToasterConfig.LONG_FULL_MESSAGE
            }
            return showToastSync(message, config)
        }
    }
}
