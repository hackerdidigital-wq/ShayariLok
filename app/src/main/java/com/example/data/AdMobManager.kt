package com.example.data

import android.app.Activity
import android.content.Context
import android.os.Build
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

object AdMobManager {
  private val initialized = AtomicBoolean(false)
  private var cachedInterstitial: InterstitialAd? = null
  private var loadedInterstitialAdUnitId: String = ""
  private var isLoadingInterstitial: Boolean = false
  private var lastShownAtMillis: Long = 0L
  private const val INTERSTITIAL_COOLDOWN_MS = 90_000L

  private val VALID_AD_UNIT_REGEX = Regex("^ca-app-pub-\\d+/\\d+$")

  fun isRobolectricUnitTest(): Boolean =
    Build.FINGERPRINT.equals("robolectric", ignoreCase = true)

  fun isValidAdUnitId(adUnitId: String): Boolean {
    val clean = adUnitId.trim()
    if (clean.isEmpty() || clean.contains('~')) return false
    return VALID_AD_UNIT_REGEX.matches(clean)
  }

  fun initialize(context: Context, adsEnabled: Boolean) {
    if (!adsEnabled || isRobolectricUnitTest()) return
    if (!initialized.compareAndSet(false, true)) return
    runCatching {
      MobileAds.initialize(context.applicationContext) {}
    }.onFailure {
      initialized.set(false)
    }
  }

  fun preloadInterstitial(context: Context, adsEnabled: Boolean, interstitialAdId: String) {
    val cleanId = interstitialAdId.trim()
    if (!adsEnabled || !isValidAdUnitId(cleanId) || isRobolectricUnitTest()) {
      cachedInterstitial = null
      loadedInterstitialAdUnitId = ""
      return
    }
    initialize(context, adsEnabled)
    if (isLoadingInterstitial) return
    if (cachedInterstitial != null && loadedInterstitialAdUnitId == cleanId) return

    isLoadingInterstitial = true
    runCatching {
      InterstitialAd.load(
        context.applicationContext,
        cleanId,
        AdRequest.Builder().build(),
        object : InterstitialAdLoadCallback() {
          override fun onAdLoaded(ad: InterstitialAd) {
            isLoadingInterstitial = false
            cachedInterstitial = ad
            loadedInterstitialAdUnitId = cleanId
          }

          override fun onAdFailedToLoad(error: LoadAdError) {
            isLoadingInterstitial = false
            cachedInterstitial = null
          }
        },
      )
    }.onFailure {
      isLoadingInterstitial = false
    }
  }

  fun maybeShowInterstitial(
    activity: Activity?,
    adsEnabled: Boolean,
    interstitialAdId: String,
  ): Boolean {
    val cleanId = interstitialAdId.trim()
    if (activity == null || activity.isFinishing || activity.isDestroyed) return false
    if (!adsEnabled || !isValidAdUnitId(cleanId) || isRobolectricUnitTest()) return false

    val now = System.currentTimeMillis()
    if (now - lastShownAtMillis < INTERSTITIAL_COOLDOWN_MS) {
      preloadInterstitial(activity, adsEnabled, cleanId)
      return false
    }

    val ad = cachedInterstitial
    if (ad == null || loadedInterstitialAdUnitId != cleanId) {
      preloadInterstitial(activity, adsEnabled, cleanId)
      return false
    }

    lastShownAtMillis = now
    cachedInterstitial = null
    ad.fullScreenContentCallback =
      object : FullScreenContentCallback() {
        override fun onAdDismissedFullScreenContent() {
          preloadInterstitial(activity, adsEnabled, cleanId)
        }

        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
          preloadInterstitial(activity, adsEnabled, cleanId)
        }
      }
    return runCatching {
      ad.show(activity)
      true
    }.getOrElse {
      preloadInterstitial(activity, adsEnabled, cleanId)
      false
    }
  }
}
