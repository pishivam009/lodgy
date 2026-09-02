package com.lodgy.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lodgy.app.R

@Composable
fun PinDots(length: Int, filledCount: Int, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier) {
        repeat(length) { index ->
            val filled = index < filledCount
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
fun Keypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit, modifier: Modifier = Modifier) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    KeypadKey(label = digit.toString(), modifier = Modifier.weight(1f)) { onDigit(digit) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f))
            KeypadKey(label = "0", modifier = Modifier.weight(1f)) { onDigit('0') }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1.3f)
                    .clickable(onClick = onBackspace),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Icon(AuthIcons.Backspace, contentDescription = stringResource(R.string.pin_setup_backspace))
            }
        }
    }
}

@Composable
private fun KeypadKey(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(1.3f)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall)
    }
}
