package com.cpen321.usermanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.decode.SvgDecoder

/**
 * Displays a team logo with SVG support using Coil.
 * Optionally shows the team abbreviation below the logo.
 */
@Composable
fun TeamLogo(
    logoUrl: String,
    teamAbbrev: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    showAbbrev: Boolean = true,
    abbrevFontSize: TextUnit = 12.sp,
    abbrevColor: Color = MaterialTheme.colorScheme.onSurface,
    contentDescription: String? = teamAbbrev
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(logoUrl)
                .decoderFactory(SvgDecoder.Factory())
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit,
            loading = {
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(size / 2),
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                // Show team abbreviation as fallback
                Box(
                    modifier = Modifier
                        .size(size)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = teamAbbrev,
                        fontSize = (size.value / 3).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        )
        
        if (showAbbrev) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = teamAbbrev,
                fontSize = abbrevFontSize,
                color = abbrevColor,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Displays a matchup between two teams with their logos.
 * Shows Away vs Home with logos and abbreviations.
 */
@Composable
fun TeamMatchup(
    awayTeamLogoUrl: String,
    awayTeamAbbrev: String,
    homeTeamLogoUrl: String,
    homeTeamAbbrev: String,
    modifier: Modifier = Modifier,
    logoSize: Dp = 40.dp,
    showAbbrevs: Boolean = true,
    abbrevFontSize: TextUnit = 12.sp,
    abbrevColor: Color = MaterialTheme.colorScheme.onSurface,
    vsText: String = "vs"
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TeamLogo(
            logoUrl = awayTeamLogoUrl,
            teamAbbrev = awayTeamAbbrev,
            size = logoSize,
            showAbbrev = showAbbrevs,
            abbrevFontSize = abbrevFontSize,
            abbrevColor = abbrevColor
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = vsText,
            fontSize = abbrevFontSize,
            color = abbrevColor,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        TeamLogo(
            logoUrl = homeTeamLogoUrl,
            teamAbbrev = homeTeamAbbrev,
            size = logoSize,
            showAbbrev = showAbbrevs,
            abbrevFontSize = abbrevFontSize,
            abbrevColor = abbrevColor
        )
    }
}
