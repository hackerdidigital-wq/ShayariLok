package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.ShayariFaq
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.LocalShayariColors

@Composable
fun FaqSheet(
  faqs: List<ShayariFaq>,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  var query by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var expandedFaqId by remember(faqs) { mutableStateOf(faqs.firstOrNull()?.id ?: "") }

  val categories =
    remember(faqs) {
      buildList {
        add("All")
        faqs.map { it.category.trim() }
          .filter { it.isNotEmpty() }
          .distinctBy { it.lowercase() }
          .forEach { add(it) }
      }
    }

  val filteredFaqs =
    remember(faqs, query, selectedCategory) {
      val cleanQuery = query.trim()
      faqs.filter { faq ->
        val matchesCategory =
          selectedCategory.equals("All", ignoreCase = true) ||
            faq.category.equals(selectedCategory, ignoreCase = true)
        val matchesQuery =
          cleanQuery.isEmpty() ||
            faq.question.contains(cleanQuery, ignoreCase = true) ||
            faq.answer.contains(cleanQuery, ignoreCase = true) ||
            faq.category.contains(cleanQuery, ignoreCase = true)
        matchesCategory && matchesQuery
      }
    }

  Dialog(
    onDismissRequest = onDismiss,
    properties =
      DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false,
      ),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors = listOf(palette.bgTop, palette.bgMid, palette.bgBottom)
            )
          )
          .windowInsetsPadding(WindowInsets.safeDrawing),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        // Header bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier
                  .size(44.dp)
                  .clip(CircleShape)
                  .background(palette.accentPurple.copy(alpha = 0.22f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_faq),
                contentDescription = strings.faqTitle,
                tint = palette.accentGold,
                modifier = Modifier.size(24.dp),
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = strings.faqTitle,
                color = palette.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = strings.faqSubtitle,
                color = palette.textMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
          IconButton(
            onClick = onDismiss,
            modifier =
              Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(palette.pillSurface)
                .testTag("faq_close_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = strings.closeAction,
              tint = palette.textPrimary,
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text(strings.faqSearchHint, color = palette.textMuted) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = palette.accentViolet,
            )
          },
          trailingIcon = {
            if (query.isNotEmpty()) {
              IconButton(onClick = { query = "" }) {
                Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = strings.closeAction,
                  tint = palette.textMuted,
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          colors =
            OutlinedTextFieldDefaults.colors(
              focusedContainerColor = palette.surface.copy(alpha = 0.9f),
              unfocusedContainerColor = palette.surface.copy(alpha = 0.78f),
              focusedBorderColor = palette.accentPurple,
              unfocusedBorderColor = palette.outline,
              focusedTextColor = palette.textPrimary,
              unfocusedTextColor = palette.textPrimary,
            ),
          modifier = Modifier.fillMaxWidth().testTag("faq_search_input"),
        )

        if (categories.size > 1) {
          Spacer(modifier = Modifier.height(10.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
          ) {
            items(categories) { category ->
              val selected = selectedCategory.equals(category, ignoreCase = true)
              FilterChip(
                selected = selected,
                onClick = { selectedCategory = category },
                label = {
                  Text(
                    text = category,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  )
                },
                colors =
                  FilterChipDefaults.filterChipColors(
                    selectedContainerColor = palette.chipSelectedBg,
                    selectedLabelColor = palette.chipSelectedText,
                    containerColor = palette.chipUnselectedBg,
                    labelColor = palette.chipUnselectedText,
                  ),
                border =
                  FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = palette.outline,
                    selectedBorderColor = palette.accentPurple,
                  ),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredFaqs.isEmpty()) {
          Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                painter = painterResource(id = R.drawable.ic_faq),
                contentDescription = null,
                tint = palette.accentGold,
                modifier = Modifier.size(48.dp),
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = strings.faqEmptyTitle,
                color = palette.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = strings.faqEmptySubtitle,
                color = palette.textMuted,
                fontSize = 13.sp,
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
          ) {
            items(items = filteredFaqs, key = { it.id }) { faq ->
              val isExpanded = expandedFaqId == faq.id
              Surface(
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable {
                      expandedFaqId = if (isExpanded) "" else faq.id
                    }
                    .testTag("faq_item_${faq.id}"),
                shape = RoundedCornerShape(18.dp),
                color = palette.surface.copy(alpha = 0.94f),
                border =
                  BorderStroke(
                    width = 1.dp,
                    color = if (isExpanded) palette.accentPurple else palette.outline.copy(alpha = 0.65f),
                  ),
              ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      if (faq.category.isNotBlank()) {
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = palette.accentPurple.copy(alpha = 0.16f),
                        ) {
                          Text(
                            text = faq.category,
                            color = palette.accentViolet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                          )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                      }
                      Text(
                        text = faq.question,
                        color = palette.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 21.sp,
                      )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                      imageVector =
                        if (isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                      contentDescription = null,
                      tint = palette.accentGold,
                      modifier = Modifier.size(24.dp),
                    )
                  }

                  AnimatedVisibility(visible = isExpanded) {
                    Column {
                      Spacer(modifier = Modifier.height(10.dp))
                      Text(
                        text = faq.answer,
                        color = palette.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
