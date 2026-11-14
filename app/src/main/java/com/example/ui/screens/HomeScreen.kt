package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ShayariTextSize
import com.example.model.ShayariItem
import com.example.ui.MainTab
import com.example.ui.ShayariScreenState
import com.example.ui.components.ShayariCard
import com.example.ui.theme.BadgePurple
import com.example.ui.theme.CrownGold
import com.example.ui.theme.HeaderPurpleEnd
import com.example.ui.theme.HeaderPurpleStart

@Composable
fun HomeScreen(
  readyState: ShayariScreenState.Ready,
  textSize: ShayariTextSize,
  isRefreshing: Boolean,
  isSearchVisible: Boolean,
  unreadNotificationCount: Int,
  snackbarHostState: SnackbarHostState,
  onSelectCategory: (String) -> Unit,
  onSelectTab: (MainTab) -> Unit,
  onToggleSearch: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onRefreshClick: () -> Unit,
  onNotificationsClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onShareAppClick: () -> Unit,
  onOpenCommunityClick: (String) -> Unit,
  onCopyShayari: (ShayariItem) -> Unit,
  onShareShayari: (ShayariItem) -> Unit,
  onFavoriteToggle: (Int) -> Unit,
) {
  var isMenuExpanded by remember { mutableStateOf(false) }

  BackHandler(enabled = isMenuExpanded) {
    isMenuExpanded = false
  }
  BackHandler(enabled = !isMenuExpanded && isSearchVisible) {
    onToggleSearch()
  }
  BackHandler(enabled = !isMenuExpanded && !isSearchVisible && readyState.activeTab == MainTab.FAVORITES) {
    onSelectTab(MainTab.EXPLORE)
  }

  BoxWithConstraints(
    modifier = Modifier.fillMaxSize().background(Color(0xFFF6F4FA))
  ) {
    val isExpandedWidth = maxWidth >= 600.dp
    val gridColumns = if (maxWidth >= 840.dp) 2 else 1

    Scaffold(
      containerColor = Color(0xFFF6F4FA),
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.navigationBarsPadding(),
        )
      },
      topBar = {
        Column(
          modifier =
            Modifier.fillMaxWidth()
              .background(
                Brush.verticalGradient(colors = listOf(HeaderPurpleStart, HeaderPurpleEnd))
              )
              .statusBarsPadding()
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            // Compact App Menu with ONLY non-duplicated useful actions
            Box {
              HeaderIconButton(
                iconRes = R.drawable.ic_menu,
                contentDescription = "Open menu",
                tint = Color.White,
                testTag = "menu_button",
                onClick = { isMenuExpanded = true },
              )

              DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false },
                containerColor = Color.White,
              ) {
                DropdownMenuItem(
                  text = {
                    Text(
                      text = if (isRefreshing) "Syncing Shayari..." else "Sync Latest Shayari",
                      color = Color(0xFF1D0D36),
                    )
                  },
                  leadingIcon = {
                    Icon(
                      painter = painterResource(id = R.drawable.ic_refresh),
                      contentDescription = null,
                      tint = BadgePurple,
                      modifier = Modifier.size(20.dp),
                    )
                  },
                  enabled = !isRefreshing,
                  onClick = {
                    isMenuExpanded = false
                    onRefreshClick()
                  },
                  modifier = Modifier.testTag("menu_sync_item"),
                )
                DropdownMenuItem(
                  text = { Text("Share App", color = Color(0xFF1D0D36)) },
                  leadingIcon = {
                    Icon(
                      painter = painterResource(id = R.drawable.ic_share),
                      contentDescription = null,
                      tint = BadgePurple,
                      modifier = Modifier.size(20.dp),
                    )
                  },
                  onClick = {
                    isMenuExpanded = false
                    onShareAppClick()
                  },
                  modifier = Modifier.testTag("menu_share_app_item"),
                )
                if (readyState.config.socialLink.isNotBlank()) {
                  DropdownMenuItem(
                    text = {
                      Text(
                        text = stringResource(R.string.join_community_title),
                        color = Color(0xFF1D0D36),
                      )
                    },
                    leadingIcon = {
                      Icon(
                        painter = painterResource(id = R.drawable.ic_open_in_new),
                        contentDescription = null,
                        tint = BadgePurple,
                        modifier = Modifier.size(20.dp),
                      )
                    },
                    onClick = {
                      isMenuExpanded = false
                      onOpenCommunityClick(readyState.config.socialLink)
                    },
                    modifier = Modifier.testTag("menu_community_item"),
                  )
                }
              }
            }

            // Center Crown + "Live Shayari / Shayari Lok" branding
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_crown),
                  contentDescription = null,
                  tint = Color.Unspecified,
                  modifier = Modifier.size(26.dp),
                )
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

            HeaderIconButton(
              iconRes = R.drawable.ic_search,
              contentDescription = "Search Shayari",
              tint = if (isSearchVisible) CrownGold else Color.White,
              testTag = "search_button",
              onClick = onToggleSearch,
            )
          }

          AnimatedVisibility(visible = isSearchVisible) {
            OutlinedTextField(
              value = readyState.searchQuery,
              onValueChange = onSearchQueryChange,
              singleLine = true,
              placeholder = {
                Text(
                  text = "Search Shayari...",
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
            unreadNotificationCount = unreadNotificationCount,
            onSelectTab = onSelectTab,
            onNotificationsClick = onNotificationsClick,
            onSettingsClick = onSettingsClick,
          )
        }
      },
    ) { innerPadding ->
      Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        if (isExpandedWidth) {
          NavigationRail(
            containerColor = Color.White,
            modifier = Modifier.fillMaxHeight().navigationBarsPadding(),
          ) {
            Spacer(modifier = Modifier.height(12.dp))
            NavigationRailItem(
              selected = readyState.activeTab == MainTab.EXPLORE,
              onClick = { onSelectTab(MainTab.EXPLORE) },
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_home),
                  contentDescription = "Home",
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text("Home") },
              modifier = Modifier.testTag("nav_home"),
            )
            NavigationRailItem(
              selected = readyState.activeTab == MainTab.FAVORITES,
              onClick = { onSelectTab(MainTab.FAVORITES) },
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_heart_filled),
                  contentDescription = stringResource(R.string.tab_favorites),
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text(stringResource(R.string.tab_favorites)) },
              modifier = Modifier.testTag("nav_favorites"),
            )
            NavigationRailItem(
              selected = false,
              onClick = onNotificationsClick,
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_notification),
                  contentDescription = stringResource(R.string.cd_notifications),
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text("Alerts") },
              modifier = Modifier.testTag("nav_notifications"),
            )
            NavigationRailItem(
              selected = false,
              onClick = onSettingsClick,
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_settings),
                  contentDescription = stringResource(R.string.cd_settings),
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text("Settings") },
              modifier = Modifier.testTag("nav_settings"),
            )
          }
        }

        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          if (readyState.activeTab == MainTab.EXPLORE) {
            LazyRow(
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth(),
            ) {
              items(readyState.config.categories, key = { it }) { category ->
                val isSelected = category.equals(readyState.selectedCategory, ignoreCase = true)
                val chipInteraction = remember { MutableInteractionSource() }
                Surface(
                  shape = RoundedCornerShape(50),
                  color = if (isSelected) BadgePurple else Color(0xFFEDEAF4),
                  shadowElevation = if (isSelected) 3.dp else 0.dp,
                  modifier =
                    Modifier.clip(RoundedCornerShape(50))
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
                      text = category,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      color = if (isSelected) Color.White else Color(0xFF3E3552),
                    )
                  }
                }
              }
            }
          }

          val items = readyState.visibleShayaris
          if (items.isEmpty()) {
            EmptyShayariState(
              isFavoritesTab = readyState.activeTab == MainTab.FAVORITES,
              modifier = Modifier.fillMaxSize().padding(24.dp),
            )
          } else if (gridColumns > 1) {
            LazyVerticalGrid(
              columns = GridCells.Fixed(gridColumns),
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.widthIn(max = 960.dp).fillMaxSize(),
            ) {
              items(items, key = { it.id }) { item ->
                ShayariCard(
                  item = item,
                  isFavorite = item.id in readyState.favoriteIds,
                  textSize = textSize,
                  onCopyClick = onCopyShayari,
                  onShareClick = onShareShayari,
                  onFavoriteToggle = onFavoriteToggle,
                )
              }
            }
          } else {
            LazyColumn(
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.widthIn(max = 600.dp).fillMaxSize(),
            ) {
              items(items, key = { it.id }) { item ->
                ShayariCard(
                  item = item,
                  isFavorite = item.id in readyState.favoriteIds,
                  textSize = textSize,
                  onCopyClick = onCopyShayari,
                  onShareClick = onShareShayari,
                  onFavoriteToggle = onFavoriteToggle,
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
  unreadNotificationCount: Int,
  onSelectTab: (MainTab) -> Unit,
  onNotificationsClick: () -> Unit,
  onSettingsClick: () -> Unit,
) {
  Surface(
    color = Color.White,
    shadowElevation = 8.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column {
      HorizontalDivider(color = Color(0xFFEAE6F3), thickness = 0.8.dp)
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .navigationBarsPadding()
            .height(64.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        BottomBarTabItem(
          label = "Home",
          iconRes = R.drawable.ic_home,
          selected = activeTab == MainTab.EXPLORE,
          badgeCount = 0,
          testTag = "nav_home",
          onClick = { onSelectTab(MainTab.EXPLORE) },
          modifier = Modifier.weight(1f),
        )
        BottomBarTabItem(
          label = stringResource(R.string.tab_favorites),
          iconRes = R.drawable.ic_heart_filled,
          selected = activeTab == MainTab.FAVORITES,
          badgeCount = 0,
          testTag = "nav_favorites",
          onClick = { onSelectTab(MainTab.FAVORITES) },
          modifier = Modifier.weight(1f),
        )
        BottomBarTabItem(
          label = "Notifications",
          iconRes = R.drawable.ic_notification,
          selected = false,
          badgeCount = unreadNotificationCount,
          testTag = "nav_notifications",
          onClick = onNotificationsClick,
          modifier = Modifier.weight(1f),
        )
        BottomBarTabItem(
          label = "Settings",
          iconRes = R.drawable.ic_settings,
          selected = false,
          badgeCount = 0,
          testTag = "nav_settings",
          onClick = onSettingsClick,
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
  val interactionSource = remember { MutableInteractionSource() }
  val activeColor = BadgePurple
  val inactiveColor = Color(0xFF7B758A)
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
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Surface(
      shape = CircleShape,
      color = Color(0xFFEDE4FA),
      modifier = Modifier.size(64.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          painter =
            painterResource(
              id = if (isFavoritesTab) R.drawable.ic_heart_filled else R.drawable.ic_category
            ),
          contentDescription = null,
          tint = BadgePurple,
          modifier = Modifier.size(28.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(14.dp))
    Text(
      text =
        stringResource(
          if (isFavoritesTab) R.string.empty_favorites_title else R.string.empty_category_title
        ),
      style = MaterialTheme.typography.titleLarge,
      color = Color(0xFF1D0D36),
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text =
        stringResource(
          if (isFavoritesTab) {
            R.string.empty_favorites_subtitle
          } else {
            R.string.empty_category_subtitle
          }
        ),
      style = MaterialTheme.typography.bodyMedium,
      color = Color(0xFF6B6280),
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 300.dp),
    )
  }
}
