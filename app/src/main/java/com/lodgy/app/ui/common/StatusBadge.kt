package com.lodgy.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lodgy.app.R
import com.lodgy.app.data.entity.BedStatus
import com.lodgy.app.data.entity.InvoiceStatus
import com.lodgy.app.data.entity.TenantStatus
import com.lodgy.app.ui.theme.LodgyStatus
import com.lodgy.app.ui.theme.StatusLevel

@Composable
fun BedStatus.label(): String =
    stringResource(if (this == BedStatus.OCCUPIED) R.string.bed_status_occupied else R.string.bed_status_vacant)

@Composable
fun InvoiceStatus.label(): String = stringResource(
    when (this) {
        InvoiceStatus.UNPAID -> R.string.invoice_status_unpaid
        InvoiceStatus.PARTIAL -> R.string.invoice_status_partial
        InvoiceStatus.PAID -> R.string.invoice_status_paid
    },
)

@Composable
fun TenantStatus.label(): String =
    stringResource(if (this == TenantStatus.ACTIVE) R.string.tenant_status_active else R.string.tenant_status_vacated)

/** The one place a status is drawn, so every screen gets the same RAG token and symbol for the
 *  same state. The icon leads and the text stays - neither colour nor symbol carries it alone. */
@Composable
fun StatusBadge(level: StatusLevel, icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    val palette = LodgyStatus.colors[level]
    Row(
        modifier = modifier
            .background(palette.container, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = text, tint = palette.onContainer, modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = palette.onContainer)
    }
}
