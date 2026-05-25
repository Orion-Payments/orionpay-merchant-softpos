package orionpay.maquinha_simulate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import orionpay.maquinha_simulate.OrionBlue
import orionpay.maquinha_simulate.OrionTextLight

@Composable
fun DiagCard(
    color: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (color == Color.Transparent) Color(0xFFF8FAFC)
                else color.copy(alpha = 0.1f)
            )
            .border(
                1.dp,
                if (color == Color.Transparent) Color(0xFFE2E8F0) else color.copy(alpha = 0.35f),
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp),
        content = content
    )
}
