package com.basecamp.turbolinks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class TurbolinksView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        FrameLayout(context, attrs, defStyleAttr) {

    private val webViewContainer: ViewGroup get() = findViewById(R.id.turbolinks_webView_container)
    private val progressContainer: ViewGroup get() = findViewById(R.id.turbolinks_progress_container)
    private val errorContainer: ViewGroup get() = findViewById(R.id.turbolinks_error_container)
    private val screenshotView: ImageView get() = findViewById(R.id.turbolinks_screenshot)

    internal val webViewRefresh: SwipeRefreshLayout? get() = webViewContainer as? SwipeRefreshLayout
    internal val errorRefresh: SwipeRefreshLayout? get() = findViewById(R.id.turbolinks_error_refresh)

    internal fun attachWebView(webView: WebView, onAttached: (Boolean) -> Unit) {
        if (webView.parent != null) {
            onAttached(false)
        }

        // Match the WebView background with its new parent
        if (background is ColorDrawable) {
            webView.setBackgroundColor((background as ColorDrawable).color)
        }

        webViewContainer.post {
            webViewContainer.addView(webView)
            onAttached(true)
        }
    }

    internal fun detachWebView(webView: WebView, onDetached: () -> Unit) {
        webViewContainer.post {
            webViewContainer.removeView(webView)
            onDetached()
        }
    }

    internal fun addProgressView(progressView: View) {
        // Don't show the progress view if a screenshot is available
        if (screenshotView.isVisible) return

        check(progressView.parent == null) { "Progress view cannot be attached to another parent" }

        removeProgressView()
        progressContainer.addView(progressView)
        progressContainer.isVisible = true
    }

    internal fun removeProgressView() {
        progressContainer.removeAllViews()
        progressContainer.isVisible = false
    }

    internal fun addScreenshot(screenshot: Bitmap?) {
        if (screenshot == null) return

        screenshotView.setImageBitmap(screenshot)
        screenshotView.isVisible = true
    }

    internal fun removeScreenshot() {
        screenshotView.setImageBitmap(null)
        screenshotView.isVisible = false
    }

    internal fun addErrorView(errorView: View) {
        check(errorView.parent == null) { "Error view cannot be attached to another parent" }

        removeErrorView()
        errorContainer.addView(errorView)
        errorContainer.isVisible = true

        errorRefresh?.let {
            it.isVisible = true
            it.isEnabled = true
            it.isRefreshing = false
        }
    }

    internal fun removeErrorView() {
        errorContainer.removeAllViews()
        errorContainer.isVisible = false

        errorRefresh?.let {
            it.isVisible = false
            it.isEnabled = false
            it.isRefreshing = false
        }
    }

    fun createScreenshot(): Bitmap? {
        if (!isLaidOut) return null
        if (!hasEnoughMemoryForScreenshot()) return null
        if (width <= 0 || height <= 0) return null

        // TODO: Catch-all approach where taking a screenshot for TL should never crash the app
        // https://sentry.io/organizations/basecamp/issues/1706905982/events/fbdca87b1f8d4bda882e946c1b890f88/?project=1861173&query=is%3Aunresolved
        return try {
            drawToBitmap()
        } catch (e: Exception) {
            // Don't ever crash when trying to make a screenshot
            null
        }
    }

    fun screenshotOrientation(): Int {
        return context.resources.configuration.orientation
    }

    private fun hasEnoughMemoryForScreenshot(): Boolean {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory().toFloat()
        val max = runtime.maxMemory().toFloat()
        val remaining = 1f - (used / max)

        // TODO: Some instances where low memory may be removing views and drawing bitmaps crashes
        // https://sentry.io/organizations/basecamp/issues/1706905982/events/latest/?project=1861173&query=is%3Aunresolved
        return remaining > .20
    }
}
