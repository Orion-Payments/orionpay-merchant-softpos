package orionpay.maquinha_simulate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orionpay.maquinha_simulate.OrionNavyLight
import orionpay.maquinha_simulate.OrionNavyMid
import orionpay.maquinha_simulate.OrionTextMuted

@Composable
fun CompSection(title: String, content: @Composable ColumnScope.() -> Unit) {

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OrionNavyMid)
            .border(1.dp, OrionNavyLight, RoundedCornerShape(14.dp))
    ) {

        Box(
            Modifier
                .fillMaxWidth()
                .background(OrionNavyLight.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text(title, color = OrionTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), content = content)
    }
}
