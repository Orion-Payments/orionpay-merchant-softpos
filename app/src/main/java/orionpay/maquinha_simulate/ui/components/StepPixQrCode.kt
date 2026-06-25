package orionpay.maquinha_simulate.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import orionpay.maquinha_simulate.ui.theme.*
import orionpay.maquinha_simulate.utils.QrCodeGenerator

@Composable
fun StepPixQrCode(
    amount: String,
    pixCode: String = "00020126360014br.gov.bcb.pix0114123456789012345204000053039865802BR5913OrionPay Ltda6009Sao Paulo62070503***6304E22D",
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val qrBitmap = remember(pixCode) { QrCodeGenerator.generate(pixCode) }

    Column(
        Modifier
            .fillMaxSize()
            .background(OrionWhite),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))
        
        Text("Pagamento via Pix", color = OrionText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Escaneie o código abaixo", color = OrionTextLight, fontSize = 14.sp)

        Spacer(Modifier.height(32.dp))

        Surface(
            modifier = Modifier.size(280.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 4.dp
        ) {
            Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                qrBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "QR Code Pix",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } ?: CircularProgressIndicator(color = OrionBlue)
            }
        }

        Spacer(Modifier.height(32.dp))

        Text("Valor a pagar", color = OrionTextLight, fontSize = 14.sp)
        Text(amount, color = OrionBlue, fontSize = 32.sp, fontWeight = FontWeight.Bold)

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onConfirm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OrionBlue)
        ) {
            Text("Simular Pagamento Confirmado", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        
        TextButton(onClick = onBack, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Cancelar", color = OrionTextLight)
        }
    }
}
