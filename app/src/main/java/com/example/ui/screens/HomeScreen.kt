package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.data.ShayariTextSize
import com.example.data.rememberShayariImageBitmap
import com.example.model.PublicProfile
import com.example.model.ShayariBanner
import com.example.model.ShayariItem
import com.example.ui.MainTab
import com.example.ui.ShayariScreenState
import com.example.ui.components.AdMobBannerCard
import com.example.ui.components.DailyStreakAndLocaleBar
import com.example.ui.components.DynamicBannerCarousel
import com.example.ui.components.LanguageCountrySelectorDialog
import com.example.ui.components.LivePollCard
import com.example.ui.components.ShayariCard
import com.example.ui.components.UserAvatar
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BadgePurple
import com.example.ui.theme.CrownGold
import com.example.ui.theme.LocalShayariColors
import kotlinx.coroutines.launch
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.parseHexColorOrNull
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  readyState: ShayariScreenState.Ready,
  textSize: ShayariTextSize,
  isRefreshing: Boolean,
  isSearchVisible: Boolean,
  unreadNotificationCount: Int,
  snackbarHostState: SnackbarHostState,
  selectedLanguageCode: String = "hi",
  selectedCountryCode: String = "IN",
  dailyStreakDays: Int = 1,
  votedPollOptions: Map<String, String> = emptyMap(),
  myProfile: PublicProfile? = null,
  userEmail: String = "",
  isDarkTheme: Boolean = true,
  onToggleTheme: () -> Unit = {},
  onSignOutClick: () -> Unit = {},
  onSelectCategory: (String) -> Unit,
  onSelectTab: (MainTab) -> Unit,
  onToggleSearch: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onRefreshClick: () -> Unit,
  onNotificationsClick: () -> Unit,
  onOpenFaqClick: () -> Unit = {},
  onOpenPrivacyPolicyClick: () -> Unit = {},
  onSettingsClick: () -> Unit,
  onShareAppClick: () -> Unit,
  onOpenCommunityClick: (String) -> Unit,
  onCopyShayari: (ShayariItem) -> Unit,
  onShareShayari: (ShayariItem) -> Unit,
  onWhatsAppShareShayari: (ShayariItem) -> Unit = onShareShayari,
  onFavoriteToggle: (Int) -> Unit,
  onSelectLanguage: (String) -> Unit = {},
  onSelectCountry: (String) -> Unit = {},
  onToggleAdultContent: (Boolean) -> Unit = {},
  onVotePoll: (pollId: String, optionId: String) -> Unit = { _, _ -> },
  onOpenBannerClick: (ShayariBanner) -> Unit = {},
  socialTabContent: @Composable (tab: MainTab, openLocalePicker: () -> Unit) -> Unit,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val coroutineScope = rememberCoroutineScope()
  var showLocaleDialog by remember { mutableStateOf(false) }
  var showAgeConfirmDialog by remember { mutableStateOf(false) }
  val customLogoBitmap = rememberShayariImageBitmap(readyState.config.appLogoUrl)

  BackHandler(enabled = drawerState.isOpen) {
    coroutineScope.launch { drawerState.close() }
  }
  BackHandler(enabled = !drawerState.isOpen && isSearchVisible) {
    onToggleSearch()
  }
  BackHandler(
    enabled = !drawerState.isOpen && !isSearchVisible && readyState.activeTab != MainTab.EXPLORE
  ) {
    onSelectTab(MainTab.EXPLORE)
  }

  val ambientVfx = rememberInfiniteTransition(label = "homeAmbientVfx")
  val driftPhase by
    ambientVfx.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 7000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart,
        ),
      label = "driftPhase",
    )
  val crownGlowAlpha by
    ambientVfx.animateFloat(
      initialValue = 0.20f,
      targetValue = 0.45f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1500, easing = EaseInOutSine),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "crownGlowAlpha",
    )

  ModalNavigationDrawer(
    drawerState = drawerState,
    gesturesEnabled = true,
    scrimColor = Color.Black.copy(alpha = 0.65f),
    drawerContent = {
      ShayariSideDrawerSheet(
        appName = readyState.config.appName,
        appLogoUrl = readyState.config.appLogoUrl,
        socialLink = readyState.config.socialLink,
        myProfile = myProfile,
        userEmail = userEmail,
        unreadNotificationCount = unreadNotificationCount,
        isRefreshing = isRefreshing,
        isDarkTheme = isDarkTheme,
        selectedLanguageCode = selectedLanguageCode,
        selectedCountryCode = selectedCountryCode,
        onCloseDrawer = { coroutineScope.launch { drawerState.close() } },
        onOpenProfileClick = {
          coroutineScope.launch { drawerState.close() }
          onSelectTab(MainTab.PROFILE)
        },
        onNotificationsClick = {
          coroutineScope.launch { drawerState.close() }
          onNotificationsClick()
        },
        onToggleTheme = onToggleTheme,
        onRefreshClick = {
          coroutineScope.launch { drawerState.close() }
          onRefreshClick()
        },
        onOpenLocaleClick = {
          coroutineScope.launch { drawerState.close() }
          showLocaleDialog = true
        },
        onOpenFaqClick = {
          coroutineScope.launch { drawerState.close() }
          onOpenFaqClick()
        },
        onOpenPrivacyPolicyClick = {
          coroutineScope.launch { drawerState.close() }
          onOpenPrivacyPolicyClick()
        },
        onSettingsClick = {
          coroutineScope.launch { drawerState.close() }
          onSettingsClick()
        },
        onShareAppClick = {
          coroutineScope.launch { drawerState.close() }
          onShareAppClick()
        },
        onOpenCommunityClick = { url ->
          coroutineScope.launch { drawerState.close() }
          onOpenCommunityClick(url)
        },
        onSignOutClick = {
          coroutineScope.launch { drawerState.close() }
          onSignOutClick()
        },
      )
    },
  ) {
  BoxWithConstraints(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(palette.bgTop, palette.bgMid, palette.bgBottom)
          )
        )
  ) {
    // Subtle animated poster bokeh aura behind the feed
    Canvas(modifier = Modifier.fillMaxSize()) {
      if (size.minDimension <= 1f) return@Canvas
      val phaseAngle = driftPhase * 2f * PI.toFloat()
      val glowShiftX = cos(phaseAngle) * 28f
      val glowShiftY = sin(phaseAngle) * 24f
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(NeonPink.copy(alpha = 0.14f), Color.Transparent),
            center = Offset(size.width * 0.85f + glowShiftX, size.height * 0.30f + glowShiftY),
            radius = size.minDimension * 0.55f,
          ),
        center = Offset(size.width * 0.85f + glowShiftX, size.height * 0.30f + glowShiftY),
        radius = size.minDimension * 0.55f,
      )
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(NeonPurple.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(size.width * 0.18f - glowShiftX, size.height * 0.72f - glowShiftY),
            radius = size.minDimension * 0.65f,
          ),
        center = Offset(size.width * 0.18f - glowShiftX, size.height * 0.72f - glowShiftY),
        radius = size.minDimension * 0.65f,
      )
    }

    val isExpandedWidth = maxWidth >= 600.dp
    val gridColumns = if (maxWidth >= 840.dp) 2 else 1

    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.navigationBarsPadding().padding(horizontal = 14.dp, vertical = 6.dp),
        ) { data ->
          Snackbar(
            shape = RoundedCornerShape(14.dp),
            containerColor = palette.snackBg,
            contentColor = palette.snackText,
            modifier = Modifier.padding(bottom = 4.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                painter = painterResource(id = R.drawable.ic_check),
                contentDescription = null,
                tint = CrownGold,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.labelLarge,
                color = palette.snackText,
              )
            }
          }
        }
      },
      topBar = {
        val dynamicPrimary = parseHexColorOrNull(readyState.config.dynamicTheme.primaryHex)
        val headerStart = dynamicPrimary ?: palette.headerStart
        val headerEnd = dynamicPrimary?.copy(alpha = 0.88f) ?: palette.headerEnd
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                Brush.verticalGradient(colors = listOf(headerStart, headerEnd))
              )
              .statusBarsPadding()
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            HeaderIconButton(
              iconRes = R.drawable.ic_menu,
              contentDescription = "Open navigation drawer",
              tint = Color.White,
              testTag = "menu_button",
              onClick = { coroutineScope.launch { drawerState.open() } },
            )

            // Center Crown / Custom Admin Logo + "Live Shayari / App Name" branding
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Box(
                    modifier =
                      Modifier.size(34.dp)
                        .background(
                          Brush.radialGradient(
                            colors =
                              listOf(CrownGold.copy(alpha = crownGlowAlpha), Color.Transparent)
                          ),
                          CircleShape,
                        )
                  )
                  if (customLogoBitmap != null) {
                    Image(
                      bitmap = customLogoBitmap,
                      contentDescription = readyState.config.appName,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.size(30.dp).clip(CircleShape),
                    )
                  } else {
                    Image(
                      painter = painterResource(id = R.drawable.ic_crown),
                      contentDescription = null,
                      modifier = Modifier.size(28.dp),
                    )
                  }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Live ",
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.ExtraBold,
                  fontStyle = FontStyle.Italic,
                  fontSize = 20.sp,
                  color = CrownGold,
                )
                Text(
                  text = "Shayari",
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.ExtraBold,
                  fontStyle = FontStyle.Italic,
                  fontSize = 20.sp,
                  color = Color.White,
                )
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Box(
                  modifier =
                    Modifier.width(24.dp).height(1.dp).background(CrownGold.copy(alpha = 0.7f))
                )
                Text(
                  text = readyState.config.appName,
                  style = MaterialTheme.typography.labelLarge,
                  fontStyle = FontStyle.Italic,
                  color = Color.White.copy(alpha = 0.94f),
                )
                Box(
                  modifier =
                    Modifier.width(24.dp).height(1.dp).background(CrownGold.copy(alpha = 0.7f))
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              HeaderBellButton(
                unreadCount = unreadNotificationCount,
                onClick = onNotificationsClick,
              )
              if (readyState.activeTab == MainTab.EXPLORE || readyState.activeTab == MainTab.FAVORITES) {
                HeaderIconButton(
                  iconRes = R.drawable.ic_search,
                  contentDescription = strings.searchShayariHint,
                  tint = if (isSearchVisible) CrownGold else Color.White,
                  testTag = "search_button",
                  onClick = onToggleSearch,
                )
              }
            }
          }

          AnimatedVisibility(
            visible =
              isSearchVisible &&
                (readyState.activeTab == MainTab.EXPLORE || readyState.activeTab == MainTab.FAVORITES)
          ) {
            OutlinedTextField(
              value = readyState.searchQuery,
              onValueChange = onSearchQueryChange,
              singleLine = true,
              placeholder = {
                Text(
                  text = strings.searchShayariHint,
                  color = Color.White.copy(alpha = 0.65f),
                )
              },
              leadingIcon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_search),
                  contentDescription = null,
                  tint = CrownGold,
                  modifier = Modifier.size(20.dp),
                )
              },
              shape = RoundedCornerShape(14.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = CrownGold,
                  unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White,
                  focusedContainerColor = Color.Black.copy(alpha = 0.25f),
                  unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                  .testTag("search_input"),
            )
          }
        }
      },
      bottomBar = {
        if (!isExpandedWidth) {
          CleanWhiteBottomBar(
            activeTab = readyState.activeTab,
            onSelectTab = onSelectTab,
          )
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (isExpandedWidth) {
          NavigationRail(
            containerColor = palette.bottomBarBg,
            modifier = Modifier.fillMaxHeight().navigationBarsPadding(),
          ) {
            Spacer(modifier = Modifier.height(12.dp))
            NavigationRailItem(
              selected = readyState.activeTab == MainTab.EXPLORE,
              onClick = { onSelectTab(MainTab.EXPLORE) },
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_home),
                  contentDescription = strings.tabHome,
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text(strings.tabHome) },
              modifier = Modifier.testTag("nav_home"),
            )
            NavigationRailItem(
              selected = readyState.activeTab == MainTab.COMMUNITY,
              onClick = { onSelectTab(MainTab.COMMUNITY) },
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_community),
                  contentDescription = strings.tabFriends,
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text(strings.tabFriends) },
              modifier = Modifier.testTag("nav_community"),
            )
            NavigationRailItem(
              selected = readyState.activeTab == MainTab.FAVORITES,
              onClick = { onSelectTab(MainTab.FAVORITES) },
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_heart_filled),
                  contentDescription = strings.tabFavorites,
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text(strings.tabFavorites) },
              modifier = Modifier.testTag("nav_favorites"),
            )
            NavigationRailItem(
              selected = readyState.activeTab == MainTab.PROFILE,
              onClick = { onSelectTab(MainTab.PROFILE) },
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_person),
                  contentDescription = strings.tabProfile,
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text(strings.tabProfile) },
              modifier = Modifier.testTag("nav_profile"),
            )
            NavigationRailItem(
              selected = false,
              onClick = onSettingsClick,
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_settings),
                  contentDescription = strings.tabSettings,
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text(strings.tabSettings) },
              modifier = Modifier.testTag("nav_settings"),
            )
          }
        }

        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          if (readyState.activeTab == MainTab.COMMUNITY || readyState.activeTab == MainTab.PROFILE) {
            socialTabContent(readyState.activeTab) { showLocaleDialog = true }
          } else {
          if (readyState.activeTab == MainTab.EXPLORE) {
            LazyRow(
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth(),
            ) {
              items(readyState.visibleCategories, key = { it }) { category ->
                val isSelected = category.equals(readyState.selectedCategory, ignoreCase = true)
                val chipInteraction = remember { MutableInteractionSource() }
                val chipBg by
                  animateColorAsState(
                    targetValue = if (isSelected) palette.chipSelectedBg else palette.chipUnselectedBg,
                    animationSpec = tween(durationMillis = 200),
                    label = "chipBg",
                  )
                val chipTextColor by
                  animateColorAsState(
                    targetValue = if (isSelected) palette.chipSelectedText else palette.chipUnselectedText,
                    animationSpec = tween(durationMillis = 200),
                    label = "chipTextColor",
                  )
                val chipScale by
                  animateFloatAsState(
                    targetValue = if (isSelected) 1.04f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "chipScale",
                  )
                Surface(
                  shape = RoundedCornerShape(50),
                  color = chipBg,
                  border =
                    if (isSelected) {
                      BorderStroke(1.2.dp, palette.accentViolet.copy(alpha = 0.85f))
                    } else {
                      BorderStroke(0.8.dp, palette.outline.copy(alpha = 0.55f))
                    },
                  shadowElevation = if (isSelected) 6.dp else 1.dp,
                  modifier =
                    Modifier.graphicsLayer {
                        scaleX = chipScale
                        scaleY = chipScale
                      }
                      .clip(RoundedCornerShape(50))
                      .clickable(
                        interactionSource = chipInteraction,
                        indication = null,
                        onClick = { onSelectCategory(category) },
                      )
                      .minimumInteractiveComponentSize()
                      .testTag("category_chip_${category.lowercase()}"),
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                  ) {
                    Text(
                      text = strings.categoryLabel(category),
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                      color = chipTextColor,
                    )
                  }
                }
              }
            }
          }

          val items = readyState.visibleShayaris
          val selectedLangObj =
            readyState.config.supportedLanguages.firstOrNull {
              it.code.equals(selectedLanguageCode, ignoreCase = true)
            }
          val selectedCountryObj =
            readyState.config.supportedCountries.firstOrNull {
              it.code.equals(selectedCountryCode, ignoreCase = true)
            }
          val engagement = readyState.config.engagement

          PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefreshClick,
            modifier = Modifier.fillMaxSize().testTag("shayari_pull_to_refresh"),
          ) {
            if (items.isEmpty()) {
              EmptyShayariState(
                isFavoritesTab = readyState.activeTab == MainTab.FAVORITES,
                modifier =
                  Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
              )
            } else if (gridColumns > 1) {
              LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                  Modifier.widthIn(max = 960.dp)
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
              ) {
                if (readyState.activeTab == MainTab.EXPLORE) {
                  item(
                    key = "streak_locale_grid_bar",
                    span = { GridItemSpan(gridColumns) },
                  ) {
                    DailyStreakAndLocaleBar(
                      showStreak = engagement.dailyStreakEnabled,
                      streakDays = dailyStreakDays,
                      selectedLanguage = selectedLangObj,
                      selectedCountry = selectedCountryObj,
                      onOpenLocalePicker = { showLocaleDialog = true },
                    )
                  }
                }
                if (readyState.activeTab == MainTab.EXPLORE && readyState.hiddenAdultCount > 0) {
                  item(
                    key = "safe_mode_grid_banner",
                    span = { GridItemSpan(gridColumns) },
                  ) {
                    SafeModeNoticeBanner(
                      hiddenCount = readyState.hiddenAdultCount,
                      onUnlockClick = { showAgeConfirmDialog = true },
                    )
                  }
                }
                if (
                  readyState.activeTab == MainTab.EXPLORE &&
                    readyState.config.banners.isNotEmpty()
                ) {
                  item(
                    key = "dynamic_banners_grid",
                    span = { GridItemSpan(gridColumns) },
                  ) {
                    DynamicBannerCarousel(
                      banners = readyState.config.banners,
                      onBannerClick = onOpenBannerClick,
                    )
                  }
                }
                if (
                  readyState.activeTab == MainTab.EXPLORE &&
                    readyState.config.adsEnabled &&
                    readyState.config.bannerAdId.isNotBlank()
                ) {
                  item(
                    key = "admob_banner_grid",
                    span = { GridItemSpan(gridColumns) },
                  ) {
                    AdMobBannerCard(
                      adsEnabled = readyState.config.adsEnabled,
                      bannerAdId = readyState.config.bannerAdId,
                    )
                  }
                }
                if (readyState.activeTab == MainTab.EXPLORE && engagement.polls.isNotEmpty()) {
                  items(
                    items = engagement.polls,
                    key = { "poll_grid_${it.id}" },
                    span = { GridItemSpan(gridColumns) },
                  ) { poll ->
                    LivePollCard(
                      poll = poll,
                      selectedOptionId = votedPollOptions[poll.id],
                      onVote = onVotePoll,
                    )
                  }
                }
                items(items, key = { it.id }) { item ->
                  ShayariCard(
                    item = item,
                    isFavorite = item.id in readyState.favoriteIds,
                    textSize = textSize,
                    whatsappShareEnabled = engagement.whatsappShareEnabled,
                    onCopyClick = onCopyShayari,
                    onShareClick = onShareShayari,
                    onWhatsAppClick = onWhatsAppShareShayari,
                    onFavoriteToggle = onFavoriteToggle,
                    modifier = Modifier.animateItem(),
                  )
                }
              }
            } else {
              LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                  Modifier.widthIn(max = 600.dp)
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
              ) {
                if (readyState.activeTab == MainTab.EXPLORE) {
                  item(key = "streak_locale_bar") {
                    DailyStreakAndLocaleBar(
                      showStreak = engagement.dailyStreakEnabled,
                      streakDays = dailyStreakDays,
                      selectedLanguage = selectedLangObj,
                      selectedCountry = selectedCountryObj,
                      onOpenLocalePicker = { showLocaleDialog = true },
                    )
                  }
                  if (readyState.hiddenAdultCount > 0) {
                    item(key = "safe_mode_list_banner") {
                      SafeModeNoticeBanner(
                        hiddenCount = readyState.hiddenAdultCount,
                        onUnlockClick = { showAgeConfirmDialog = true },
                      )
                    }
                  }
                  if (readyState.config.banners.isNotEmpty()) {
                    item(key = "dynamic_banners_list") {
                      DynamicBannerCarousel(
                        banners = readyState.config.banners,
                        onBannerClick = onOpenBannerClick,
                      )
                    }
                  }
                  if (readyState.config.adsEnabled && readyState.config.bannerAdId.isNotBlank()) {
                    item(key = "admob_banner_top") {
                      AdMobBannerCard(
                        adsEnabled = readyState.config.adsEnabled,
                        bannerAdId = readyState.config.bannerAdId,
                      )
                    }
                  }
                  if (engagement.polls.isNotEmpty()) {
                    items(engagement.polls, key = { "poll_${it.id}" }) { poll ->
                      LivePollCard(
                        poll = poll,
                        selectedOptionId = votedPollOptions[poll.id],
                        onVote = onVotePoll,
                      )
                    }
                  }
                }
                items(items, key = { it.id }) { item ->
                  ShayariCard(
                    item = item,
                    isFavorite = item.id in readyState.favoriteIds,
                    textSize = textSize,
                    whatsappShareEnabled = engagement.whatsappShareEnabled,
                    onCopyClick = onCopyShayari,
                    onShareClick = onShareShayari,
                    onWhatsAppClick = onWhatsAppShareShayari,
                    onFavoriteToggle = onFavoriteToggle,
                    modifier = Modifier.animateItem(),
                  )
                }
                item { Spacer(modifier = Modifier.height(10.dp)) }
              }
            }
          }
          }
        }
      }
    }

    if (showLocaleDialog) {
      LanguageCountrySelectorDialog(
        languages = readyState.config.supportedLanguages,
        countries = readyState.config.supportedCountries,
        selectedLanguageCode = selectedLanguageCode,
        selectedCountryCode = selectedCountryCode,
        onSelectLanguage = onSelectLanguage,
        onSelectCountry = onSelectCountry,
        onDismiss = { showLocaleDialog = false },
      )
    }

    if (showAgeConfirmDialog) {
      AgeConfirmationDialog(
        onConfirm18Plus = {
          showAgeConfirmDialog = false
          onToggleAdultContent(true)
        },
        onDismiss = { showAgeConfirmDialog = false },
      )
    }
  }
  }
}

@Composable
private fun SafeModeNoticeBanner(
  hiddenCount: Int,
  onUnlockClick: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = palette.surface.copy(alpha = 0.94f),
    border = BorderStroke(1.dp, palette.accentGold.copy(alpha = 0.55f)),
    modifier = Modifier.fillMaxWidth().testTag("safe_mode_banner"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f).padding(end = 8.dp),
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_shield),
          contentDescription = null,
          tint = palette.accentGold,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "${strings.safeModeActiveBanner} $hiddenCount",
          style = MaterialTheme.typography.labelMedium,
          color = palette.textPrimary,
        )
      }
      OutlinedButton(
        onClick = onUnlockClick,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, palette.accentGold.copy(alpha = 0.7f)),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("unlock_18_plus_banner_button"),
      ) {
        Text(
          text = strings.unlock18PlusCta,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = palette.accentGold,
        )
      }
    }
  }
}

@Composable
private fun HeaderBellButton(
  unreadCount: Int,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .size(48.dp)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
        .testTag("top_notifications_bell"),
  ) {
    Box(contentAlignment = Alignment.TopEnd) {
      Icon(
        painter = painterResource(id = R.drawable.ic_notification),
        contentDescription = stringResource(R.string.cd_notifications),
        tint = if (unreadCount > 0) CrownGold else Color.White,
        modifier = Modifier.size(23.dp),
      )
      if (unreadCount > 0) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.offset(x = 6.dp, y = (-4).dp)
              .size(16.dp)
              .background(Color(0xFFEF233C), CircleShape),
        ) {
          Text(
            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}

@Composable
private fun HeaderIconButton(
  iconRes: Int,
  contentDescription: String,
  tint: Color,
  testTag: String,
  onClick: () -> Unit,
) {
  val interactionSource = remember { MutableInteractionSource() }
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.minimumInteractiveComponentSize()
        .size(48.dp)
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
        .testTag(testTag),
  ) {
    Icon(
      painter = painterResource(id = iconRes),
      contentDescription = contentDescription,
      tint = tint,
      modifier = Modifier.size(24.dp),
    )
  }
}

@Composable
private fun CleanWhiteBottomBar(
  activeTab: MainTab,
  onSelectTab: (MainTab) -> Unit,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  Surface(
    color = palette.bottomBarBg,
    shadowElevation = 8.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column {
      HorizontalDivider(color = palette.bottomBarDivider, thickness = 0.8.dp)
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .height(64.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BottomBarTabItem(
          label = strings.tabHome,
          iconRes = R.drawable.ic_home,
          selected = activeTab == MainTab.EXPLORE,
          badgeCount = 0,
          testTag = "nav_home",
          onClick = { onSelectTab(MainTab.EXPLORE) },
          modifier = Modifier.weight(1f),
        )
        BottomBarTabItem(
          label = strings.tabFriends,
          iconRes = R.drawable.ic_community,
          selected = activeTab == MainTab.COMMUNITY,
          badgeCount = 0,
          testTag = "nav_community",
          onClick = { onSelectTab(MainTab.COMMUNITY) },
          modifier = Modifier.weight(1f),
        )
        BottomBarTabItem(
          label = strings.tabFavorites,
          iconRes = R.drawable.ic_heart_filled,
          selected = activeTab == MainTab.FAVORITES,
          badgeCount = 0,
          testTag = "nav_favorites",
          onClick = { onSelectTab(MainTab.FAVORITES) },
          modifier = Modifier.weight(1f),
        )
        BottomBarTabItem(
          label = strings.tabProfile,
          iconRes = R.drawable.ic_person,
          selected = activeTab == MainTab.PROFILE,
          badgeCount = 0,
          testTag = "nav_profile",
          onClick = { onSelectTab(MainTab.PROFILE) },
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

@Composable
private fun BottomBarTabItem(
  label: String,
  iconRes: Int,
  selected: Boolean,
  badgeCount: Int,
  testTag: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalShayariColors.current
  val interactionSource = remember { MutableInteractionSource() }
  val activeColor = palette.accentPurple
  val inactiveColor = palette.textMuted
  val tint = if (selected) activeColor else inactiveColor

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier =
      modifier
        .fillMaxHeight()
        .minimumInteractiveComponentSize()
        .clickable(
          interactionSource = interactionSource,
          indication = null,
          onClick = onClick,
        )
        .testTag(testTag),
  ) {
    Box(contentAlignment = Alignment.TopEnd) {
      Icon(
        painter = painterResource(id = iconRes),
        contentDescription = label,
        tint = tint,
        modifier = Modifier.size(23.dp),
      )
      if (badgeCount > 0) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.offset(x = 7.dp, y = (-4).dp)
              .size(16.dp)
              .background(Color(0xFFEF233C), CircleShape),
        ) {
          Text(
            text = if (badgeCount > 9) "9+" else badgeCount.toString(),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
      color = tint,
      maxLines = 1,
    )
  }
}

@Composable
private fun EmptyShayariState(
  isFavoritesTab: Boolean,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      shape = CircleShape,
      color = palette.accentPurple.copy(alpha = 0.2f),
      border = BorderStroke(1.dp, palette.accentViolet.copy(alpha = 0.45f)),
      modifier = Modifier.size(64.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          painter =
            painterResource(
              id = if (isFavoritesTab) R.drawable.ic_heart_filled else R.drawable.ic_category
            ),
          contentDescription = null,
          tint = palette.accentGold,
          modifier = Modifier.size(28.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(14.dp))
    Text(
      text =
        if (isFavoritesTab) strings.emptyFavoritesTitle else strings.emptyCategoryTitle,
      style = MaterialTheme.typography.titleLarge,
      color = palette.textPrimary,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text =
        if (isFavoritesTab) {
          strings.emptyFavoritesSubtitle
        } else {
          strings.emptyCategorySubtitle
        },
      style = MaterialTheme.typography.bodyMedium,
      color = palette.textSecondary,
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 300.dp),
    )
  }
}

@Composable
private fun ShayariSideDrawerSheet(
  appName: String,
  appLogoUrl: String,
  socialLink: String,
  myProfile: PublicProfile?,
  userEmail: String,
  unreadNotificationCount: Int,
  isRefreshing: Boolean,
  isDarkTheme: Boolean,
  selectedLanguageCode: String,
  selectedCountryCode: String,
  onCloseDrawer: () -> Unit,
  onOpenProfileClick: () -> Unit,
  onNotificationsClick: () -> Unit,
  onToggleTheme: () -> Unit,
  onRefreshClick: () -> Unit,
  onOpenLocaleClick: () -> Unit,
  onOpenFaqClick: () -> Unit,
  onOpenPrivacyPolicyClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onShareAppClick: () -> Unit,
  onOpenCommunityClick: (String) -> Unit,
  onSignOutClick: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  val customLogoBitmap = rememberShayariImageBitmap(appLogoUrl)
  val drawerItemColors =
    NavigationDrawerItemDefaults.colors(
      selectedContainerColor = palette.accentPurple.copy(alpha = 0.22f),
      unselectedContainerColor = Color.Transparent,
      selectedIconColor = palette.accentGold,
      unselectedIconColor = palette.textSecondary,
      selectedTextColor = palette.textPrimary,
      unselectedTextColor = palette.textPrimary,
    )

  ModalDrawerSheet(
    drawerContainerColor = palette.bgTop,
    drawerContentColor = palette.textPrimary,
    windowInsets = WindowInsets(0, 0, 0, 0),
    modifier = Modifier.widthIn(min = 290.dp, max = 340.dp).fillMaxWidth(0.82f),
  ) {
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(palette.bgTop, palette.bgMid, palette.bgBottom)
          )
        )
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      if (size.minDimension <= 1f) return@Canvas
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(NeonPink.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(size.width * 0.9f, size.height * 0.12f),
            radius = size.minDimension * 0.7f,
          ),
        center = Offset(size.width * 0.9f, size.height * 0.12f),
        radius = size.minDimension * 0.7f,
      )
    }

    Column(
      modifier =
        Modifier.fillMaxSize()
          .navigationBarsPadding(),
    ) {
      // Brand & Live Profile Drawer Header
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .background(
              Brush.verticalGradient(
                colors = listOf(palette.drawerHeaderStart, palette.drawerHeaderEnd)
              )
            )
            .statusBarsPadding()
            .clickable(onClick = onOpenProfileClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
      ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              if (customLogoBitmap != null) {
                Image(
                  bitmap = customLogoBitmap,
                  contentDescription = appName,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier.size(26.dp).clip(CircleShape),
                )
              } else {
                Image(
                  painter = painterResource(id = R.drawable.ic_crown),
                  contentDescription = null,
                  modifier = Modifier.size(24.dp),
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = appName,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.ExtraBold,
                fontStyle = FontStyle.Italic,
                fontSize = 18.sp,
                color = CrownGold,
              )
            }

            Box(
              contentAlignment = Alignment.Center,
              modifier =
                Modifier.minimumInteractiveComponentSize()
                  .size(40.dp)
                  .clip(CircleShape)
                  .clickable(onClick = onCloseDrawer)
                  .testTag("drawer_close_button"),
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = stringResource(R.string.cd_back),
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(
              photoUrl = myProfile?.photoUrl.orEmpty(),
              displayName = myProfile?.displayName?.ifBlank { appName } ?: appName,
              size = 56.dp,
              showOnlineDot = true,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = myProfile?.displayName?.ifBlank { "Shayari Lover" } ?: "Shayari Lover",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              val handleText = myProfile?.effectiveHandle?.takeIf { it.isNotBlank() } ?: ""
              if (handleText.isNotBlank()) {
                Text(
                  text = handleText,
                  style = MaterialTheme.typography.labelMedium,
                  color = CrownGold,
                  fontWeight = FontWeight.SemiBold,
                )
              }
              if (userEmail.isNotBlank()) {
                Text(
                  text = userEmail,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFFE7DAF8),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        HorizontalDivider(color = palette.outline.copy(alpha = 0.4f))

        Column(
          modifier =
            Modifier.weight(1f)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          NavigationDrawerItem(
            selected = false,
            onClick = onNotificationsClick,
            icon = {
              Icon(
                painter = painterResource(id = R.drawable.ic_notification),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
              )
            },
            label = { Text(strings.liveAlertsTitle, fontWeight = FontWeight.SemiBold) },
            badge = {
              if (unreadNotificationCount > 0) {
                Surface(shape = CircleShape, color = NeonPink) {
                  Text(
                    text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  )
                }
              }
            },
            colors = drawerItemColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("drawer_nav_alerts"),
          )

          NavigationDrawerItem(
            selected = false,
            onClick = onToggleTheme,
            icon = {
              Icon(
                painter =
                  painterResource(
                    id = if (isDarkTheme) R.drawable.ic_dark_mode else R.drawable.ic_light_mode
                  ),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
              )
            },
            label = {
              Text(
                text = if (isDarkTheme) strings.darkModeLabel else strings.lightModeLabel,
                fontWeight = FontWeight.Medium,
              )
            },
            badge = {
              Switch(
                checked = isDarkTheme,
                onCheckedChange = { onToggleTheme() },
                colors =
                  SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = palette.accentPurple,
                    uncheckedThumbColor = palette.accentGold,
                    uncheckedTrackColor = palette.surfaceVariant,
                  ),
              )
            },
            colors = drawerItemColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("drawer_theme_toggle"),
          )

          HorizontalDivider(
            color = palette.outline.copy(alpha = 0.35f),
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
          )

          NavigationDrawerItem(
            selected = false,
            onClick = { if (!isRefreshing) onRefreshClick() },
            icon = {
              Icon(
                painter = painterResource(id = R.drawable.ic_refresh),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
              )
            },
            label = {
              Text(
                text = if (isRefreshing) strings.menuSyncing else strings.menuSync,
                fontWeight = FontWeight.Medium,
              )
            },
            colors = drawerItemColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("menu_sync_item"),
          )
          NavigationDrawerItem(
            selected = false,
            onClick = onOpenLocaleClick,
            icon = {
              Icon(
                painter = painterResource(id = R.drawable.ic_language),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
              )
            },
            label = { Text(strings.menuLanguageRegion, fontWeight = FontWeight.Medium) },
            badge = {
              Text(
                text = "${selectedLanguageCode.uppercase()} • ${selectedCountryCode.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.accentGold,
                fontWeight = FontWeight.Bold,
              )
            },
            colors = drawerItemColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("menu_locale_item"),
          )
          NavigationDrawerItem(
            selected = false,
            onClick = onOpenFaqClick,
            icon = {
              Icon(
                painter = painterResource(id = R.drawable.ic_faq),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
              )
            },
            label = { Text(strings.faqTitle, fontWeight = FontWeight.Medium) },
            colors = drawerItemColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("drawer_nav_faq"),
          )
          NavigationDrawerItem(
            selected = false,
            onClick = onOpenPrivacyPolicyClick,
            icon = {
              Icon(
                painter = painterResource(id = R.drawable.ic_shield),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
              )
            },
            label = { Text(strings.privacyPolicyTitle, fontWeight = FontWeight.Medium) },
            colors = drawerItemColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("drawer_privacy_policy_item"),
          )
          NavigationDrawerItem(
            selected = false,
            onClick = onSettingsClick,
            icon = {
              Icon(
                painter = painterResource(id = R.drawable.ic_settings),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
              )
            },
            label = { Text(strings.menuSettings, fontWeight = FontWeight.Medium) },
            colors = drawerItemColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("menu_settings_item"),
          )
          NavigationDrawerItem(
            selected = false,
            onClick = onShareAppClick,
            icon = {
              Icon(
                painter = painterResource(id = R.drawable.ic_share),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
              )
            },
            label = { Text(strings.menuShareApp, fontWeight = FontWeight.Medium) },
            colors = drawerItemColors,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("menu_share_app_item"),
          )
          if (socialLink.isNotBlank()) {
            NavigationDrawerItem(
              selected = false,
              onClick = { onOpenCommunityClick(socialLink) },
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_open_in_new),
                  contentDescription = null,
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text(strings.menuJoinCommunity, fontWeight = FontWeight.Medium) },
              colors = drawerItemColors,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("menu_community_item"),
            )
          }
        }

        HorizontalDivider(color = palette.outline.copy(alpha = 0.4f))

        Row(
          modifier =
            Modifier.fillMaxWidth()
              .clickable(onClick = onSignOutClick)
              .minimumInteractiveComponentSize()
              .padding(horizontal = 22.dp, vertical = 14.dp)
              .testTag("drawer_sign_out_button"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              painter = painterResource(id = R.drawable.ic_logout),
              contentDescription = null,
              tint = NeonPink,
              modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = strings.signOut,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = NeonPink,
            )
          }
          Text(
            text = "v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textMuted,
          )
        }
      }
    }
  }
}
