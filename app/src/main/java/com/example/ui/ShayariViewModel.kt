package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ShayariRepository
import com.example.data.ShayariTextSize
import com.example.model.ShayariConfig
import com.example.model.ShayariItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
  EXPLORE,
  FAVORITES,
}

sealed interface ShayariScreenState {
  data object Loading : ShayariScreenState

  data class Maintenance(
    val appName: String,
    val message: String,
    val socialLink: String,
  ) : ShayariScreenState

  data class Ready(
    val config: ShayariConfig,
    val selectedCategory: String,
    val activeTab: MainTab,
    val favoriteIds: Set<Int>,
    val searchQuery: String,
  ) : ShayariScreenState {
    val visibleShayaris: List<ShayariItem>
      get() {
        val base =
          if (activeTab == MainTab.FAVORITES) {
            config.shayariList.filter { it.id in favoriteIds }
          } else {
            config.shayariList
          }
        val byCategory =
          if (
            selectedCategory.equals("Popular", ignoreCase = true) ||
              selectedCategory.equals("All", ignoreCase = true)
          ) {
            base
          } else {
            base.filter { it.category.equals(selectedCategory, ignoreCase = true) }
          }
        val query = searchQuery.trim()
        return if (query.isEmpty()) {
          byCategory
        } else {
          byCategory.filter { item ->
            item.text.contains(query, ignoreCase = true) ||
              item.category.contains(query, ignoreCase = true) ||
              item.author.contains(query, ignoreCase = true)
          }
        }
      }
  }
}

data class ShayariUiState(
  val screenState: ShayariScreenState = ShayariScreenState.Loading,
  val isRefreshing: Boolean = false,
  val isUsingCachedData: Boolean = false,
  val lastSyncedAtMillis: Long = 0L,
  val textSize: ShayariTextSize = ShayariTextSize.MEDIUM,
  val isSearchVisible: Boolean = false,
  val searchQuery: String = "",
  val syncStatusMessage: String? = null,
  val showUpdateDialog: Boolean = false,
  val unreadNotificationCount: Int = 0,
)

class ShayariViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = ShayariRepository(application)

  private val _uiState = MutableStateFlow(ShayariUiState())
  val uiState: StateFlow<ShayariUiState> = _uiState.asStateFlow()

  private var currentConfig: ShayariConfig = repository.loadCachedConfig()
  private var selectedCategory: String =
    currentConfig.categories.firstOrNull() ?: "Popular"
  private var activeTab: MainTab = MainTab.EXPLORE
  private var favoriteIds: Set<Int> = repository.getFavoriteIds()
  private var textSize: ShayariTextSize = repository.getShayariTextSize()
  private var searchQuery: String = ""
  private var updateDismissedThisSession: Boolean = false

  init {
    publishCurrentState(
      isUsingCached = !repository.hasCachedRemoteConfig(),
      syncMessage = null,
    )
    refreshConfig(isInitialAutoSync = true)
  }

  fun reloadFromCache() {
    currentConfig = repository.loadCachedConfig()
    favoriteIds = repository.getFavoriteIds()
    textSize = repository.getShayariTextSize()
    if (
      !selectedCategory.equals("Popular", ignoreCase = true) &&
        !selectedCategory.equals("All", ignoreCase = true) &&
        currentConfig.categories.none { it.equals(selectedCategory, ignoreCase = true) }
    ) {
      selectedCategory = currentConfig.categories.firstOrNull() ?: "Popular"
    }
    publishCurrentState(
      isUsingCached = _uiState.value.isUsingCachedData,
      syncMessage = null,
    )
  }

  fun refreshConfig(isInitialAutoSync: Boolean = false) {
    if (_uiState.value.isRefreshing) return
    viewModelScope.launch {
      _uiState.update { it.copy(isRefreshing = true, syncStatusMessage = null) }
      val result = repository.fetchRemoteConfig()
      result.fold(
        onSuccess = { remoteConfig ->
          currentConfig = remoteConfig
          if (
            !selectedCategory.equals("Popular", ignoreCase = true) &&
              !selectedCategory.equals("All", ignoreCase = true) &&
              remoteConfig.categories.none { it.equals(selectedCategory, ignoreCase = true) }
          ) {
            selectedCategory = remoteConfig.categories.firstOrNull() ?: "Popular"
          }
          publishCurrentState(
            isUsingCached = false,
            syncMessage = if (isInitialAutoSync) null else "Synced latest Shayari",
          )
        },
        onFailure = {
          publishCurrentState(
            isUsingCached = true,
            syncMessage =
              if (isInitialAutoSync) {
                null
              } else {
                "Offline mode: showing saved Shayari"
              },
          )
        },
      )
      _uiState.update { it.copy(isRefreshing = false) }
    }
  }

  fun selectCategory(category: String) {
    selectedCategory = category
    publishCurrentState(
      isUsingCached = _uiState.value.isUsingCachedData,
      syncMessage = null,
    )
  }

  fun selectTab(tab: MainTab) {
    activeTab = tab
    if (tab == MainTab.FAVORITES) {
      selectedCategory = currentConfig.categories.firstOrNull() ?: "Popular"
    }
    publishCurrentState(
      isUsingCached = _uiState.value.isUsingCachedData,
      syncMessage = null,
    )
  }

  fun toggleSearchVisibility() {
    val nextVisible = !_uiState.value.isSearchVisible
    if (!nextVisible) {
      searchQuery = ""
    }
    _uiState.update { it.copy(isSearchVisible = nextVisible, searchQuery = searchQuery) }
    publishCurrentState(
      isUsingCached = _uiState.value.isUsingCachedData,
      syncMessage = null,
    )
  }

  fun updateSearchQuery(query: String) {
    searchQuery = query
    _uiState.update { it.copy(searchQuery = query) }
    publishCurrentState(
      isUsingCached = _uiState.value.isUsingCachedData,
      syncMessage = null,
    )
  }

  fun toggleFavorite(shayariId: Int) {
    favoriteIds = repository.toggleFavorite(shayariId)
    publishCurrentState(
      isUsingCached = _uiState.value.isUsingCachedData,
      syncMessage = null,
    )
  }

  fun dismissUpdateDialog() {
    updateDismissedThisSession = true
    _uiState.update { it.copy(showUpdateDialog = false) }
  }

  fun clearSyncStatusMessage() {
    _uiState.update { it.copy(syncStatusMessage = null) }
  }

  private fun publishCurrentState(
    isUsingCached: Boolean,
    syncMessage: String?,
  ) {
    val screenState =
      if (currentConfig.maintenanceMode) {
        ShayariScreenState.Maintenance(
          appName = currentConfig.appName,
          message = currentConfig.maintenanceMessage,
          socialLink = currentConfig.socialLink,
        )
      } else {
        ShayariScreenState.Ready(
          config = currentConfig,
          selectedCategory = selectedCategory,
          activeTab = activeTab,
          favoriteIds = favoriteIds,
          searchQuery = searchQuery,
        )
      }

    val needsUpdate =
      !updateDismissedThisSession &&
        currentConfig.latestVersionCode > BuildConfig.VERSION_CODE &&
        currentConfig.forceUpdateUrl.isNotBlank()

    val unreadCount =
      if (repository.hasUnreadNotifications(currentConfig)) {
        currentConfig.notifications.size
      } else {
        0
      }

    _uiState.update { previous ->
      previous.copy(
        screenState = screenState,
        isUsingCachedData = isUsingCached,
        lastSyncedAtMillis = repository.getLastSyncedTimeMillis(),
        textSize = textSize,
        searchQuery = searchQuery,
        syncStatusMessage = syncMessage,
        showUpdateDialog = needsUpdate,
        unreadNotificationCount = unreadCount,
      )
    }
  }
}
