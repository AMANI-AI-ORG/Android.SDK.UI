package ai.amani.sample.presentation.entry

import ai.amani.sample.domain.model.Country
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Palette (from the verify app's register colors) ─────────────────────────────────────
private val ScreenBg = Color(0xFFF1EEE9)
private val CardBg = Color(0xFFFFFFFF)
private val TitleColor = Color(0xFF16151A)
private val Subtitle = Color(0xFF9A9AA2)
private val Pink = Color(0xFFEA3365)
private val PinkSoft = Color(0xFFFDEBEF)
private val SearchBg = Color(0xFFEFEEF1)
private val Hint = Color(0xFF9A9AA2)
private val RowText = Color(0xFF1C1B20)

// Rubik fonts shipped inside the Amani UI SDK.
private val rubik400 = FontFamily(Font(ai.amani.amani_sdk.R.font.rubik_400, FontWeight.Normal))
private val rubik500 = FontFamily(Font(ai.amani.amani_sdk.R.font.rubik_500, FontWeight.Medium))

/** Locale code → ISO country (for the flag). Same mapping as the verify app's CountrySelector. */
private fun localeToCountryCode(code: String): String = when (code.lowercase()) {
    "tr" -> "TR"; "hy" -> "AM"; "pt-br" -> "BR"; "bg" -> "BG"; "cs" -> "CZ"
    "da" -> "DK"; "fr" -> "FR"; "ka" -> "GE"; "de" -> "DE"; "el" -> "GR"
    "hu" -> "HU"; "id" -> "ID"; "it" -> "IT"; "lo" -> "LA"; "lt" -> "LT"
    "nl" -> "NL"; "fl" -> "PH"; "pl" -> "PL"; "pt" -> "PT"; "ro" -> "RO"
    "ru" -> "RU"; "sk" -> "SK"; "es" -> "ES"; "th" -> "TH"; "uk" -> "UA"
    "en" -> "GB"; "vi" -> "VN"
    else -> ""
}

private fun flagEmoji(code: String): String {
    val cc = localeToCountryCode(code)
    if (cc.length != 2) return ""
    val base = 0x1F1E6
    val first = base + (cc[0].uppercaseChar() - 'A')
    val second = base + (cc[1].uppercaseChar() - 'A')
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

/**
 * Entry screen, a Compose reproduction of the verify app's RegisterFragment (no language picker —
 * country selection drives the SDK language). The pink primary button starts the QR scan.
 */
@Composable
fun RegisterScreen(
    countries: List<Country>,
    selected: Country,
    onCountrySelected: (Country) -> Unit,
    onScan: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Logo badge (top-left)
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, top = 20.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Pink),
                contentAlignment = Alignment.Center
            ) {
                Text("A", color = Color.White, fontSize = 26.sp, fontFamily = rubik500)
            }

            Text(
                text = "—  IDENTITY",
                color = Pink,
                fontSize = 13.sp,
                letterSpacing = 2.sp,
                fontFamily = rubik500,
                modifier = Modifier.padding(start = 20.dp, top = 30.dp)
            )
            Text(
                text = "Verify\nwho you are.",
                color = TitleColor,
                fontSize = 42.sp,
                fontFamily = rubik500,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, top = 10.dp)
            )

            // White country card fills the rest.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(CardBg)
                    .padding(horizontal = 24.dp)
                    .padding(top = 28.dp)
            ) {
                Text(
                    text = "Select your country",
                    color = TitleColor,
                    fontSize = 24.sp,
                    fontFamily = rubik500,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "We'll match the right ID flow to your region.",
                    color = Subtitle,
                    fontSize = 15.sp,
                    fontFamily = rubik400,
                    modifier = Modifier.padding(top = 6.dp)
                )
                CountrySelector(
                    items = countries,
                    selected = selected,
                    onSelected = onCountrySelected
                )
            }
        }

        // Floating primary action.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Pink)
                .clickable { onScan() },
            contentAlignment = Alignment.Center
        ) {
            Text("Scan QR to begin", color = Color.White, fontSize = 17.sp, fontFamily = rubik500)
        }
    }
}

/** Search + scrollable country list; the selected row is highlighted pink with a check. */
@Composable
private fun CountrySelector(
    items: List<Country>,
    selected: Country,
    onSelected: (Country) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = items.filter { it.displayName.contains(query.trim(), ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SearchBg)
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Hint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                if (query.isEmpty()) {
                    Text("Search country", color = Hint, fontSize = 16.sp, fontFamily = rubik400)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = RowText, fontSize = 16.sp, fontFamily = rubik400),
                    cursorBrush = SolidColor(Pink),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = filtered, key = { it.displayName }) { item ->
                val isSelected = item.displayName == selected.displayName
                val flag = flagEmoji(item.language)
                var rowModifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                if (isSelected) {
                    rowModifier = rowModifier
                        .background(PinkSoft)
                        .border(1.5.dp, Pink, RoundedCornerShape(18.dp))
                }
                rowModifier = rowModifier
                    .clickable { onSelected(item) }
                    .padding(horizontal = 16.dp, vertical = 18.dp)

                Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
                    if (flag.isNotEmpty()) {
                        Text(text = flag, fontSize = 22.sp)
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(
                        modifier = Modifier.weight(1f),
                        text = item.displayName,
                        color = RowText,
                        fontSize = 18.sp,
                        fontFamily = if (isSelected) rubik500 else rubik400
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier.size(26.dp).clip(CircleShape).background(Pink),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
