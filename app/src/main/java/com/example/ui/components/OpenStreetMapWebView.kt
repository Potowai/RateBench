package com.example.ui.components

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.BenchItem
import org.json.JSONArray
import org.json.JSONObject

fun benchesToJson(benches: List<BenchItem>): String {
  val array = JSONArray()
  for (b in benches) {
    val obj = JSONObject()
    obj.put("id", b.id)
    obj.put("title", b.title)
    obj.put("latitude", b.latitude)
    obj.put("longitude", b.longitude)
    obj.put("rating", b.rating.toDouble())
    obj.put("reviewCount", b.reviewCount)
    array.put(obj)
  }
  return array.toString()
}

class RateBenchJSBridge(
  private val onBenchClickedAction: (String) -> Unit,
  private val onMapClickedAction: (Double, Double) -> Unit,
  private val onMapReadyAction: () -> Unit
) {
  @JavascriptInterface
  fun onBenchClicked(benchId: String) {
    onBenchClickedAction(benchId)
  }

  @JavascriptInterface
  fun onMapClicked(lat: Double, lng: Double) {
    onMapClickedAction(lat, lng)
  }

  @JavascriptInterface
  fun onMapReady() {
    onMapReadyAction()
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun OpenStreetMapWebView(
  onWebViewCreated: (WebView) -> Unit,
  onMapReady: () -> Unit,
  onBenchClicked: (String) -> Unit,
  onMapClicked: (Double, Double) -> Unit
) {
  AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { ctx ->
      WebView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(android.graphics.Color.parseColor("#F1F5F9"))

        settings.apply {
          javaScriptEnabled = true
          domStorageEnabled = true
          allowFileAccess = true
          allowContentAccess = true
          databaseEnabled = true
          useWideViewPort = true
          loadWithOverviewMode = true
          cacheMode = WebSettings.LOAD_DEFAULT
          userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0"
        }

        addJavascriptInterface(
          RateBenchJSBridge(
            onBenchClickedAction = { benchId ->
              post { onBenchClicked(benchId) }
            },
            onMapClickedAction = { lat, lng ->
              post { onMapClicked(lat, lng) }
            },
            onMapReadyAction = {
              post { onMapReady() }
            }
          ),
          "Android"
        )

        webChromeClient = object : WebChromeClient() {
          override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            Log.d("RateBenchWebMap", "${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()}")
            return true
          }
        }

        webViewClient = object : WebViewClient() {
          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            post { onMapReady() }
          }

          override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            Log.e("RateBenchWebMap", "WebView Error: $description on $failingUrl")
          }
        }

        loadUrl("file:///android_asset/map.html")
        onWebViewCreated(this)
      }
    }
  )
}
