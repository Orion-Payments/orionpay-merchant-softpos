package orionpay.maquinha_simulate.ui.components


import android.content.Intent
import orionpay.maquinha_simulate.BuildConfig
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.*
import org.json.JSONObject
import orionpay.maquinha_simulate.OrionBlue
import orionpay.maquinha_simulate.OrionError
import orionpay.maquinha_simulate.OrionNavy
import orionpay.maquinha_simulate.OrionNavyLight
import orionpay.maquinha_simulate.OrionNavyMid
import orionpay.maquinha_simulate.OrionSuccess
import orionpay.maquinha_simulate.OrionText
import orionpay.maquinha_simulate.OrionTextMuted
import orionpay.maquinha_simulate.OrionTextSub
import orionpay.maquinha_simulate.data.model.ComprovanteData
import orionpay.maquinha_simulate.utils.buildComprovanteText
import orionpay.maquinha_simulate.utils.callSendEmailApi
import orionpay.maquinha_simulate.utils.isValidEmail
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*



@Composable
fun ComprovanteScreen(data: ComprovanteData, onClose: () -> Unit, onBackToHome: () -> Unit = onClose) {
    val context      = androidx.compose.ui.platform.LocalContext.current
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    val navPad       = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusPad    = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var email        by remember { mutableStateOf("") }
    var sending      by remember { mutableStateOf(false) }
    var emailError   by remember { mutableStateOf(false) }
    var sendApiError by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(OrionNavy)) {

        // ── Conteúdo principal ────────────────────────────────────────────
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(OrionNavyMid)
                    .padding(top = statusPad + 12.dp, bottom = 12.dp, start = 16.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.ArrowBack, null, tint = OrionText, modifier = Modifier.size(20.dp))
                }
                Text("Comprovante", color = OrionText, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, buildComprovanteText(data))
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "Comprovante OrionPay — ${data.amount}")
                    }
                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartilhar comprovante"))
                }) {
                    Icon(Icons.Default.Share, null, tint = OrionBlue, modifier = Modifier.size(20.dp))
                }
            }

            // Body scrollável
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(24.dp))

                // Cabeçalho do comprovante
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(OrionNavyMid)
                        .border(1.dp, OrionNavyLight, RoundedCornerShape(16.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(OrionSuccess.copy(alpha = 0.15f))
                            .border(2.dp, OrionSuccess.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = OrionSuccess, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Pagamento aprovado", color = OrionSuccess, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(data.amount, color = OrionText, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(data.dateTime, color = OrionTextMuted, fontSize = 12.sp)
                }

                Spacer(Modifier.height(14.dp))

                CompSection("Dados da transação") {
                    CompRow("Forma de pagamento", data.product)
                    CompRow("Bandeira",           data.brand.ifEmpty { "—" })
                    CompRow("Cartão",             data.maskedPan.ifEmpty { "—" })
                    CompRow("Portador",           data.holder)
                    CompRow("Modo de entrada",    data.entryMode)
                }

                Spacer(Modifier.height(14.dp))

                CompSection("Autorização") {
                    CompRow("Cód. autorização", data.authCode.ifEmpty { "—" })
                    CompRow("NSU",              data.nsu.ifEmpty { "—" })
                    CompRow("Terminal",         data.terminalSn)
                    CompRow("Estabelecimento",  data.merchantId.take(20) + "...")
                }

                Spacer(Modifier.height(24.dp))

                // Seção de e-mail
                Text("Enviar comprovante por e-mail",
                    color = OrionText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("O comprovante será enviado diretamente ao cliente.",
                    color = OrionTextMuted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value         = email,
                    onValueChange = { email = it; emailError = false; sendApiError = "" },
                    placeholder   = { Text("cliente@email.com", color = OrionTextSub) },
                    leadingIcon   = {
                        Icon(Icons.Default.Email, null, tint = OrionTextMuted, modifier = Modifier.size(18.dp))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction    = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { if (isValidEmail(email)) { sending = true; sendApiError = "" } else emailError = true }
                    ),
                    isError  = emailError,
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = OrionBlue,
                        unfocusedBorderColor    = OrionNavyLight,
                        errorBorderColor        = OrionError,
                        focusedTextColor        = OrionText,
                        unfocusedTextColor      = OrionText,
                        cursorColor             = OrionBlue,
                        focusedContainerColor   = OrionNavyMid,
                        unfocusedContainerColor = OrionNavyMid,
                        errorContainerColor     = OrionNavyMid,
                    ),
                    shape      = RoundedCornerShape(12.dp),
                    modifier   = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (emailError) {
                    Spacer(Modifier.height(4.dp))
                    Text("E-mail inválido", color = OrionError, fontSize = 12.sp)
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (!isValidEmail(email)) {
                            emailError = true
                            return@Button
                        }
                        sending = true
                        sendApiError = ""
                        // Tudo no mesmo scope.launch — snackbarHost acessível aqui
                        scope.launch {
                            val emailCapture = email
                            val apiResult = callSendEmailApi(
                                transactionId = data.transactionId,
                                email         = emailCapture
                            )
                            sending = false
                            if (apiResult.first) {
                                val snackResult = snackbarHost.showSnackbar(
                                    message     = "Comprovante enviado para $emailCapture",
                                    actionLabel = "Nova transação",
                                    duration    = SnackbarDuration.Indefinite
                                )
                                if (snackResult == SnackbarResult.ActionPerformed) {
                                    onBackToHome()
                                }
                            } else {
                                sendApiError = apiResult.second
                            }
                        }
                    },
                    enabled  = email.isNotEmpty() && !sending,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = OrionBlue,
                        disabledContainerColor = OrionNavyLight
                    )
                ) {
                    if (sending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Enviando...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar por e-mail", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                AnimatedVisibility(visible = sendApiError.isNotEmpty()) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(OrionError.copy(alpha = 0.1f))
                                .border(1.dp, OrionError.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, null, tint = OrionError, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(sendApiError, color = OrionError, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(navPad + 24.dp))
            }
        }

        // Snackbar sobreposto no topo do Box — sempre visível
        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        ) { snackData ->
            Snackbar(
                shape              = RoundedCornerShape(14.dp),
                containerColor     = OrionSuccess,
                contentColor       = Color.White,
                actionContentColor = Color.White,
                action = {
                    TextButton(onClick = {
                        snackData.performAction()
                        onBackToHome()
                    }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                snackData.visuals.actionLabel ?: "Nova transação",
                                color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(snackData.visuals.message, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    } // fecha Box
}
