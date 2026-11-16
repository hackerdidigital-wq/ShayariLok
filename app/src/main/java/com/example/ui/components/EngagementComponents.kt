package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.ShayariPoll
import com.example.model.SupportedCountry
import com.example.model.SupportedLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.BadgeOrange
import com.example.ui.theme.LocalShayariColors
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple

@Composable
fun DailyStreakAndLocaleBar(
  showStreak: Boolean,
  streakDays: Int,
  selectedLanguage: SupportedLanguage?,
  selectedCountry: SupportedCountry?,
  onOpenLocalePicker: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (showStreak) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, palette.accentGold.copy(alpha = 0.45f)),
        modifier = Modifier.weight(1f).testTag("daily_streak_banner"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(28.dp)
                .background(
                  Brush.linearGradient(colors = listOf(BadgeOrange, NeonPink)),
                  CircleShape,
                ),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_flame),
              contentDescription = strings.dayStreakSuffix,
              tint = Color.White,
              modifier = Modifier.size(16.dp),
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "$streakDays ${strings.dayStreakSuffix}",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.ExtraBold,
              color = palette.accentGold,
              maxLines = 1,
            )
            Text(
              text = strings.dailyRewardSubtitle,
              style = MaterialTheme.typography.labelSmall,
              color = palette.textSecondary,
              fontSize = 10.sp,
              maxLines = 1,
            )
          }
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = palette.surface.copy(alpha = 0.92f),
      border = BorderStroke(1.dp, palette.outline),
      modifier =
        Modifier.clip(RoundedCornerShape(16.dp))
          .clickable(onClick = onOpenLocalePicker)
          .minimumInteractiveComponentSize()
          .testTag("language_country_chip"),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_language),
          contentDescription = strings.menuLanguageRegion,
          tint = palette.accentGold,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text =
            buildString {
              val flag = selectedCountry?.flagEmoji.orEmpty()
              if (flag.isNotBlank()) {
                append(flag)
                append(" ")
              }
              append(selectedLanguage?.nativeName ?: "हिन्दी")
              if (selectedLanguage?.isRtl == true) {
                append(" (RTL)")
              }
            },
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = palette.textPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
fun LivePollCard(
  poll: ShayariPoll,
  selectedOptionId: String?,
  onVote: (pollId: String, optionId: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  val hasVoted = !selectedOptionId.isNullOrBlank()
  val effectiveTotal =
    poll.totalVotes + if (hasVoted && poll.options.none { it.id == selectedOptionId && it.votes > 0 }) 1 else 0

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = palette.surface.copy(alpha = 0.95f),
    border = BorderStroke(1.dp, palette.accentPink.copy(alpha = 0.45f)),
    shadowElevation = 4.dp,
    modifier = modifier.fillMaxWidth().testTag("live_poll_card_${poll.id}"),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = palette.accentPink.copy(alpha = if (palette.isDark) 0.22f else 0.14f),
          border = BorderStroke(0.8.dp, palette.accentPink.copy(alpha = 0.65f)),
        ) {
          Text(
            text = strings.livePollBadge,
            style = MaterialTheme.typography.labelSmall,
            color = if (palette.isDark) Color(0xFFFFB6D9) else Color(0xFF9B0047),
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
          )
        }
        Text(
          text = "$effectiveTotal ${strings.votesSuffix}",
          style = MaterialTheme.typography.labelSmall,
          color = palette.textSecondary,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = poll.question,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = palette.textPrimary,
      )

      Spacer(modifier = Modifier.height(10.dp))

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        poll.options.forEach { option ->
          val isChosen = option.id == selectedOptionId
          val displayVotes = option.votes + if (isChosen && option.votes == 0) 1 else 0
          val ratio = if (effectiveTotal > 0) (displayVotes.toFloat() / effectiveTotal.toFloat()).coerceIn(0f, 1f) else 0f
          val percent = (ratio * 100f).toInt()

          Surface(
            shape = RoundedCornerShape(12.dp),
            color =
              if (isChosen) palette.accentPurple.copy(alpha = if (palette.isDark) 0.50f else 0.16f)
              else palette.surfaceVariant.copy(alpha = 0.78f),
            border =
              BorderStroke(
                1.dp,
                if (isChosen) palette.accentGold.copy(alpha = 0.85f) else palette.outline,
              ),
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onVote(poll.id, option.id) }
                .minimumInteractiveComponentSize()
                .testTag("poll_option_${poll.id}_${option.id}"),
          ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = if (isChosen) "✓ ${option.label}" else option.label,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                  color = palette.textPrimary,
                  modifier = Modifier.weight(1f),
                )
                if (hasVoted) {
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isChosen) palette.accentGold else palette.textSecondary,
                  )
                }
              }
              if (hasVoted) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                  progress = { ratio },
                  color = if (isChosen) palette.accentGold else palette.accentPurple,
                  trackColor = palette.textMuted.copy(alpha = 0.18f),
                  modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageCountrySelectorDialog(
  languages: List<SupportedLanguage>,
  countries: List<SupportedCountry>,
  selectedLanguageCode: String,
  selectedCountryCode: String,
  onSelectLanguage: (String) -> Unit,
  onSelectCountry: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val palette = LocalShayariColors.current
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = palette.surface,
    shape = RoundedCornerShape(24.dp),
    title = {
      Text(
        text = strings.menuLanguageRegion,
        color = palette.textPrimary,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = strings.selectLanguageHeading,
          style = MaterialTheme.typography.labelMedium,
          color = palette.accentGold,
          fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          languages.forEach { lang ->
            val selected = lang.code.equals(selectedLanguageCode, ignoreCase = true)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (selected) palette.accentPurple else palette.surfaceVariant,
              border =
                BorderStroke(
                  1.dp,
                  if (selected) palette.accentGold else palette.outline,
                ),
              modifier =
                Modifier.clip(RoundedCornerShape(12.dp))
                  .clickable { onSelectLanguage(lang.code) }
                  .minimumInteractiveComponentSize()
                  .testTag("select_lang_${lang.code}"),
            ) {
              Text(
                text = if (lang.isRtl) "${lang.nativeName} (RTL)" else lang.nativeName,
                color = if (selected) Color.White else palette.textPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = strings.selectCountryHeading,
          style = MaterialTheme.typography.labelMedium,
          color = palette.accentGold,
          fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          countries.forEach { country ->
            val selected = country.code.equals(selectedCountryCode, ignoreCase = true)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (selected) palette.accentPurple else palette.surfaceVariant,
              border =
                BorderStroke(
                  1.dp,
                  if (selected) palette.accentGold else palette.outline,
                ),
              modifier =
                Modifier.clip(RoundedCornerShape(12.dp))
                  .clickable { onSelectCountry(country.code) }
                  .minimumInteractiveComponentSize()
                  .testTag("select_country_${country.code}"),
            ) {
              Text(
                text = "${country.flagEmoji} ${country.name}".trim(),
                color = if (selected) Color.White else palette.textPrimary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(strings.doneAction, color = palette.accentGold, fontWeight = FontWeight.Bold)
      }
    },
  )
}
