package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AdMobManager
import com.example.ui.theme.LocalShayariColors
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobBannerCard(
  adsEnabled: Boolean,
  bannerAdId: String,
  modifier: Modifier = Modifier,
) {
  val cleanAdId = bannerAdId.trim()
  if (!adsEnabled || cleanAdId.isBlank()) return
  val isPreview = LocalInspectionMode.current
  val isTestEnv = AdMobManager.isRobolectricUnitTest()
  if (isPreview || isTestEnv) return

  val context = LocalContext.current
  val palette = LocalShayariColors.current

  val adView =
    remember(cleanAdId) {
      AdMobManager.initialize(context, true)
      AdView(context).apply {
        setAdSize(AdSize.BANNER)
        adUnitId = cleanAdId
        loadAd(AdRequest.Builder().build())
      }
    }

  DisposableEffect(adView) {
    onDispose {
      runCatching { adView.destroy() }
    }
  }

  Surface(
    modifier = modifier.fillMaxWidth().testTag("admob_banner_card"),
    shape = RoundedCornerShape(16.dp),
    color = palette.surface.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, palette.outline.copy(alpha = 0.5f)),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = "Sponsored",
        color = palette.textMuted,
        fontSize = 10.sp,
        modifier = Modifier.padding(bottom = 2.dp),
      )
      AndroidView(
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        factory = { adView },
      )
    }
  }
}
