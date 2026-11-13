package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ShayariTextSize
import com.example.model.ShayariItem
import com.example.ui.MainTab
import com.example.ui.ShayariScreenState
import com.example.ui.components.ShayariCard
import com.example.ui.theme.DarkBackgroundBottom
import com.example.ui.theme.LightBackgroundBottom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  readyState: ShayariScreenState.Ready,
  textSize: ShayariTextSize,
  isRefreshing: Boolean,
  isUsingCachedData: Boolean,
  hasUnreadAnnouncement: Boolean,
  snackbarHostState: SnackbarHostState,
  onSelectCategory: (String) -> Unit,
  onSelectTab: (MainTab) -> Unit,
  onRefreshClick: () -> Unit,
  onNotificationsClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onCopyShayari: (ShayariItem) -> Unit,
  onShareShayari: (ShayariItem) -> Unit,
  onFavoriteToggle: (Int) -> Unit,
) {
  BackHandler(enabled = readyState.activeTab == MainTab.FAVORITES) {
    onSelectTab(MainTab.EXPLORE)
  }

  val isDark = isSystemInDarkTheme()
  val bgGradient =
    Brush.verticalGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.background,
          if (isDark) DarkBackgroundBottom else LightBackgroundBottom,
        )
    )

  BoxWithConstraints(modifier = Modifier.fillMaxSize().background(bgGradient)) {
    val isExpandedWidth = maxWidth >= 600.dp
    val gridColumns = if (maxWidth >= 840.dp) 2 else 1

    Scaffold(
      containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f),
      contentWindowInsets = WindowInsets.statusBars,
      snackbarHost = {
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.navigationBarsPadding(),
        )
      },
      topBar = {
        Column {
          TopAppBar(
            colors =
              TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                titleContentColor = MaterialTheme.colorScheme.onBackground,
              ),
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.size(34.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      painter = painterResource(id = R.drawable.ic_category),
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
                Column {
                  Text(
                    text = readyState.config.appName,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text =
                      if (isUsingCachedData) {
                        "Offline Ready • ${readyState.config.shayariList.size} Shayari"
                      } else {
                        "Synced • ${readyState.config.shayariList.size} Shayari"
                      },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            },
            actions = {
              IconButton(
                onClick = onRefreshClick,
                enabled = !isRefreshing,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("refresh_button"),
              ) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_refresh),
                  contentDescription = stringResource(R.string.cd_refresh),
                  tint = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.size(22.dp),
                )
              }

              IconButton(
                onClick = onNotificationsClick,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("notifications_button"),
              ) {
                Box {
                  Icon(
                    painter = painterResource(id = R.drawable.ic_notification),
                    contentDescription = stringResource(R.string.cd_notifications),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(22.dp),
                  )
                  if (hasUnreadAnnouncement) {
                    Box(
                      modifier =
                        Modifier.size(8.dp)
                          .align(Alignment.TopEnd)
                          .background(MaterialTheme.colorScheme.secondary, CircleShape)
                    )
                  }
                }
              }

              IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("settings_button"),
              ) {
                Icon(
                  painter = painterResource(id = R.drawable.ic_settings),
                  contentDescription = stringResource(R.string.cd_settings),
                  tint = MaterialTheme.colorScheme.onBackground,
                  modifier = Modifier.size(22.dp),
                )
              }
            },
          )

          if (isRefreshing) {
            LinearProgressIndicator(
              modifier = Modifier.fillMaxWidth().height(2.dp),
              color = MaterialTheme.colorScheme.secondary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
          }
        }
      },
      bottomBar = {
        if (!isExpandedWidth) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            tonalElevation = 3.dp,
          ) {
            NavigationBarItem(
              selected = readyState.activeTab == MainTab.EXPLORE,
              onClick = { onSelectTab(MainTab.EXPLORE) },
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_category),
                  contentDescription = stringResource(R.string.tab_explore),
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text(stringResource(R.string.tab_explore)) },
              modifier = Modifier.testTag("nav_explore"),
            )
            NavigationBarItem(
              selected = readyState.activeTab == MainTab.FAVORITES,
              onClick = { onSelectTab(MainTab.FAVORITES) },
              icon = {
                Icon(
                  painter =
                    painterResource(
                      id =
                        if (readyState.activeTab == MainTab.FAVORITES) {
                          R.drawable.ic_heart_filled
                        } else {
                          R.drawable.ic_heart
                        }
                    ),
                  contentDescription = stringResource(R.string.tab_favorites),
                  modifier = Modifier.size(22.dp),
                )
              },
              label = {
                val favCount = readyState.favoriteIds.size
                Text(
                  if (favCount > 0) {
                    "${stringResource(R.string.tab_favorites)} ($favCount)"
                  } else {
                    stringResource(R.string.tab_favorites)
                  }
                )
              },
              modifier = Modifier.testTag("nav_favorites"),
            )
          }
        }
      },
    ) { innerPadding ->
      Row(
        modifier = Modifier.fillMaxSize().padding(innerPadding)
      ) {
        if (isExpandedWidth) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            modifier = Modifier.fillMaxHeight().navigationBarsPadding(),
          ) {
            Spacer(modifier = Modifier.height(12.dp))
            NavigationRailItem(
              selected = readyState.activeTab == MainTab.EXPLORE,
              onClick = { onSelectTab(MainTab.EXPLORE) },
              icon = {
                Icon(
                  painter = painterResource(id = R.drawable.ic_category),
                  contentDescription = stringResource(R.string.tab_explore),
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text(stringResource(R.string.tab_explore)) },
              modifier = Modifier.testTag("nav_explore"),
            )
            NavigationRailItem(
              selected = readyState.activeTab == MainTab.FAVORITES,
              onClick = { onSelectTab(MainTab.FAVORITES) },
              icon = {
                Icon(
                  painter =
                    painterResource(
                      id =
                        if (readyState.activeTab == MainTab.FAVORITES) {
                          R.drawable.ic_heart_filled
                        } else {
                          R.drawable.ic_heart
                        }
                    ),
                  contentDescription = stringResource(R.string.tab_favorites),
                  modifier = Modifier.size(22.dp),
                )
              },
              label = { Text(stringResource(R.string.tab_favorites)) },
              modifier = Modifier.testTag("nav_favorites"),
            )
          }
        }

        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          // Category filter bar (shown on Explore tab)
          if (readyState.activeTab == MainTab.EXPLORE) {
            LazyRow(
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.widthIn(max = 960.dp).fillMaxWidth(),
            ) {
              items(readyState.config.categories, key = { it }) { category ->
                val isSelected = category.equals(readyState.selectedCategory, ignoreCase = true)
                FilterChip(
                  selected = isSelected,
                  onClick = { onSelectCategory(category) },
                  label = {
                    Text(
                      text = category,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                  },
                  shape = RoundedCornerShape(50),
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primary,
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                      labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("category_chip_${category.lowercase()}"),
                )
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
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
              horizontalArrangement = Arrangement.spacedBy(14.dp),
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
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
              verticalArrangement = Arrangement.spacedBy(14.dp),
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
              item { Spacer(modifier = Modifier.height(12.dp)) }
            }
          }
        }
      }
    }
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
      color = MaterialTheme.colorScheme.primaryContainer,
      modifier = Modifier.size(68.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          painter =
            painterResource(
              id = if (isFavoritesTab) R.drawable.ic_heart else R.drawable.ic_category
            ),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(30.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text =
        stringResource(
          if (isFavoritesTab) R.string.empty_favorites_title else R.string.empty_category_title
        ),
      style = MaterialTheme.typography.titleLarge,
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
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
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.widthIn(max = 320.dp),
    )
  }
}
