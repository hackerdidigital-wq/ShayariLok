package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ShayariRepository
import com.example.data.ShayariTextSize
import com.example.model.ShayariConfig
import com.example.model.ShayariItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
  EXPLORE,
  COMMUNITY,
  FAVORITES,
  PROFILE,
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
  val isSplashVisible: Boolean = true,
  val splashAppName: String = "Shayari Lok",
  val isRefreshing: Boolean = false,
  val isNetworkConnected: Boolean = true,
  val showOfflineDialog: Boolean = false,
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
    currentConfig.categories.firstOrNull() ?: "All"
  private var activeTab: MainTab = MainTab.EXPLORE
  private var favoriteIds: Set<Int> = repository.getFavoriteIds()
  private var textSize: ShayariTextSize = repository.getShayariTextSize()
  private var searchQuery: String = ""
  private var updateDismissedThisSession: Boolean = false
  private var hasObservedInitialNetworkState: Boolean = false
  private var hasPendingRemoteSync: Boolean = false
  private var disconnectDebounceJob: Job? = null
  private var silentRetryJob: Job? = null

  init {
    val initialOnline = repository.isNetworkAvailable()
    _uiState.update {
      it.copy(
        isNetworkConnected = initialOnline,
        showOfflineDialog = !initialOnline,
        splashAppName = currentConfig.appName,
      )
    }
    publishCurrentState(syncMessage = null)
    observeConnectivity()
    refreshConfig(isSilentSync = true)
    viewModelScope.launch {
      delay(1150L)
      val waitStart = System.currentTimeMillis()
      while (_uiState.value.isRefreshing && System.currentTimeMillis() - waitStart < 650L) {
        delay(80L)
      }
      _uiState.update { it.copy(isSplashVisible = false) }
    }
  }

  private fun observeConnectivity() {
    viewModelScope.launch {
      repository.observeNetworkConnectivity().collect { isConnected ->
        val wasConnected = _uiState.value.isNetworkConnected

        if (isConnected) {
          disconnectDebounceJob?.cancel()
          _uiState.update { current ->
            current.copy(
              isNetworkConnected = true,
              showOfflineDialog = false,
            )
          }
          if ((hasObservedInitialNetworkState && !wasConnected) || hasPendingRemoteSync) {
            // Internet is active/restored: silently auto-sync latest server data
            refreshConfig(isSilentSync = true)
          }
        } else {
          // Debounce briefly so Wi-Fi <-> Mobile Data handoff never triggers false pop-ups
          disconnectDebounceJob?.cancel()
          disconnectDebounceJob =
            viewModelScope.launch {
              delay(600L)
              val stillOffline = !repository.isNetworkAvailable()
              _uiState.update { current ->
                current.copy(
                  isNetworkConnected = !stillOffline,
                  showOfflineDialog = stillOffline,
                )
              }
            }
        }
        hasObservedInitialNetworkState = true
      }
    }
  }

  fun onAppResumed() {
    currentConfig = repository.loadCachedConfig()
    favoriteIds = repository.getFavoriteIds()
    textSize = repository.getShayariTextSize()
    if (
      !selectedCategory.equals("Popular", ignoreCase = true) &&
        !selectedCategory.equals("All", ignoreCase = true) &&
        currentConfig.categories.none { it.equals(selectedCategory, ignoreCase = true) }
    ) {
      selectedCategory = currentConfig.categories.firstOrNull() ?: "All"
    }
    val onlineNow = repository.isNetworkAvailable()
    _uiState.update {
      it.copy(
        isNetworkConnected = onlineNow,
        showOfflineDialog = !onlineNow,
      )
    }
    publishCurrentState(syncMessage = null)

    if (onlineNow) {
      refreshConfig(isSilentSync = true)
    }
  }

  fun retryConnection() {
    val currentlyOnline = repository.isNetworkAvailable()
    _uiState.update {
      it.copy(
        isNetworkConnected = currentlyOnline,
        showOfflineDialog = !currentlyOnline,
      )
    }
    if (currentlyOnline) {
      refreshConfig(isSilentSync = true)
    }
  }

  fun dismissOfflineDialog() {
    _uiState.update { it.copy(showOfflineDialog = false) }
  }

  fun refreshConfig(isSilentSync: Boolean = false) {
    if (_uiState.value.isRefreshing) return
    viewModelScope.launch {
      val currentlyOnline = repository.isNetworkAvailable()
      _uiState.update {
        it.copy(
          isRefreshing = true,
          isNetworkConnected = currentlyOnline,
          showOfflineDialog = !currentlyOnline,
          syncStatusMessage = null,
        )
      }

      if (!currentlyOnline) {
        hasPendingRemoteSync = true
        _uiState.update { it.copy(isRefreshing = false) }
        return@launch
      }

      val result = repository.fetchRemoteConfig()
      result.fold(
        onSuccess = { remoteConfig ->
          hasPendingRemoteSync = false
          silentRetryJob?.cancel()
          currentConfig = remoteConfig
          if (
            !selectedCategory.equals("Popular", ignoreCase = true) &&
              !selectedCategory.equals("All", ignoreCase = true) &&
              remoteConfig.categories.none { it.equals(selectedCategory, ignoreCase = true) }
          ) {
            selectedCategory = remoteConfig.categories.firstOrNull() ?: "All"
          }
          _uiState.update {
            it.copy(
              isNetworkConnected = true,
              showOfflineDialog = false,
            )
          }
          publishCurrentState(
            syncMessage = if (isSilentSync) null else "नई शायरियाँ अपडेट हो गईं",
          )
        },
        onFailure = {
          // Silent Fallback: if mobile internet is ON, NEVER show offline pop-up;
          // keep showing cached/local data quietly and auto-retry in background.
          val onlineAfterAttempt = repository.isNetworkAvailable()
          hasPendingRemoteSync = true
          _uiState.update {
            it.copy(
              isNetworkConnected = onlineAfterAttempt,
              showOfflineDialog = !onlineAfterAttempt,
            )
          }
          publishCurrentState(syncMessage = null)
          if (onlineAfterAttempt) {
            scheduleSilentBackgroundRetry()
          }
        },
      )
      _uiState.update { it.copy(isRefreshing = false) }
    }
  }

  private fun scheduleSilentBackgroundRetry() {
    if (silentRetryJob?.isActive == true) return
    silentRetryJob =
      viewModelScope.launch {
        delay(15_000L)
        if (hasPendingRemoteSync && repository.isNetworkAvailable() && !_uiState.value.isRefreshing) {
          refreshConfig(isSilentSync = true)
        }
      }
  }

  fun selectCategory(category: String) {
    selectedCategory = category
    publishCurrentState(syncMessage = null)
  }

  fun selectTab(tab: MainTab) {
    activeTab = tab
    if (tab == MainTab.FAVORITES) {
      selectedCategory = currentConfig.categories.firstOrNull() ?: "All"
    }
    publishCurrentState(syncMessage = null)
  }

  fun toggleSearchVisibility() {
    val nextVisible = !_uiState.value.isSearchVisible
    if (!nextVisible) {
      searchQuery = ""
    }
    _uiState.update { it.copy(isSearchVisible = nextVisible, searchQuery = searchQuery) }
    publishCurrentState(syncMessage = null)
  }

  fun updateSearchQuery(query: String) {
    searchQuery = query
    _uiState.update { it.copy(searchQuery = query) }
    publishCurrentState(syncMessage = null)
  }

  fun toggleFavorite(shayariId: Int) {
    favoriteIds = repository.toggleFavorite(shayariId)
    publishCurrentState(syncMessage = null)
  }

  fun dismissUpdateDialog() {
    updateDismissedThisSession = true
    _uiState.update { it.copy(showUpdateDialog = false) }
  }

  fun markAdminNotificationsRead() {
    repository.markNotificationsRead(currentConfig)
    publishCurrentState(syncMessage = null)
  }

  fun clearSyncStatusMessage() {
    _uiState.update { it.copy(syncStatusMessage = null) }
  }

  private fun publishCurrentState(syncMessage: String?) {
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
        splashAppName = currentConfig.appName,
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
