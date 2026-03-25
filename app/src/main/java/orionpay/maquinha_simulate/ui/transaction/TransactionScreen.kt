package orionpay.maquinha_simulate

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.*
import orionpay.maquinha_simulate.config.ApiConfig
import orionpay.maquinha_simulate.data.model.CardData
import orionpay.maquinha_simulate.data.model.ComprovanteData
import orionpay.maquinha_simulate.data.model.TxResult
import orionpay.maquinha_simulate.data.model.isRetryable
import orionpay.maquinha_simulate.domain.enums.ProductType
import orionpay.maquinha_simulate.domain.enums.TxState
import orionpay.maquinha_simulate.ui.components.FlowHeader
import orionpay.maquinha_simulate.ui.components.StepAmount
import orionpay.maquinha_simulate.ui.components.StepNfcWait
import orionpay.maquinha_simulate.ui.components.StepPaymentMethod
import orionpay.maquinha_simulate.ui.components.StepProcessing
import orionpay.maquinha_simulate.ui.components.StepResult
import orionpay.maquinha_simulate.utils.isoNow
import orionpay.maquinha_simulate.utils.maskDisplay
import orionpay.maquinha_simulate.utils.maskPan
import java.util.*

@Composable
fun TransactionScreen(
    onEnableNfc: (Boolean) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var step            by remember { mutableStateOf(1) }
    var rawAmount       by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf(ProductType.CREDIT_AVISTA) }
    var cardData        by remember { mutableStateOf<CardData?>(null) }
    var nfcStatus       by remember { mutableStateOf("Aguardando cartão...") }
    var nfcError        by remember { mutableStateOf(false) }
    var txResult        by remember { mutableStateOf<TxResult?>(null) }
    var merchantId      by remember { mutableStateOf(ApiConfig.MERCHANT_ID) }
    var terminalSn      by remember { mutableStateOf("POS-ORION-992") }
    var extRef          by remember { mutableStateOf("PEDIDO-${System.currentTimeMillis() % 100000}") }

    // ── IDEMPOTÊNCIA ────────────────────────────────────────────────────────
    var idempotencyKey  by remember { mutableStateOf("") }
    var isSubmitting    by remember { mutableStateOf(false) }

    val amountDouble    = (rawAmount.toLongOrNull() ?: 0L) / 100.0
    val formattedAmount = java.text.NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(amountDouble)

    fun submitTransaction() {
        if (isSubmitting) return
        isSubmitting = true

        if (idempotencyKey.isEmpty()) {
            idempotencyKey = java.util.UUID.randomUUID().toString()
        }

        Log.d("ORION_IDEM", "X-Idempotency-Key: $idempotencyKey")

        scope.launch {
            try {
                val token = getInternalAuthToken()
                Log.d("ORION_AUTH", "Token obtido: ${token?.take(10)}...")

                if (token == null) {
                    txResult = TxResult(TxState.ERROR, "Falha na autenticação do terminal")
                    isSubmitting = false
                    step = 5
                    return@launch
                }

                val card = cardData
                val payload = buildTxPayload(
                    merchantId     = merchantId,
                    amount         = amountDouble,
                    productType    = selectedProduct.apiKey,
                    terminalSn     = terminalSn,
                    externalRef    = extRef,
                    entryMode      = card?.entryMode ?: "CHIP",
                    cardBrand      = card?.brand ?: "",
                    cardHolder     = card?.holder ?: "",
                    cardNumber     = card?.panRaw ?: "",
                    expirationDate = card?.expiry ?: "",
                    cvv            = if ((card?.entryMode ?: "CHIP") == "CHIP") "000" else card?.cvv2?.ifEmpty { "000" } ?: "000",
                    cryptogram     = card?.cryptogram ?: "",
                    atc            = card?.atc ?: "",
                    iad            = card?.iad ?: "",
                    aip            = card?.aip ?: "",
                    tvr            = card?.tvr ?: ""
                )
                
                // Usando a função centralizada que já usa os timeouts e IPs corretos
                txResult = sendTransactionRaw(payload, token, idempotencyKey)
                
                Log.d("ORION_TX", "Resultado: ${txResult?.state} - ${txResult?.message}")

                Log.d("ORION_TX", "Resposta bruta >> : ${payload?.toString(2)}")

                Log.d("ORION_TX", "Resposta JSON: ${txResult?.rawJson}")
                
            } catch (e: Exception) {
                Log.e("ORION_TX", "Erro no fluxo de envio", e)
                txResult = TxResult(TxState.ERROR, e.message ?: "Erro interno ao enviar")
            } finally {
                isSubmitting = false
                step = 5
            }
        }
    }

    LaunchedEffect(step) { onEnableNfc(step == 3) }

    DisposableEffect(step) {
        NfcDataBus.onCardRead = { card ->
            cardData  = card
            nfcError  = false
            nfcStatus = "Cartão lido! ${card.brand} — ${maskPan(card.panRaw)}"
            step = 4
        }
        NfcDataBus.onCardError = { msg -> nfcError = true; nfcStatus = msg }
        onDispose { NfcDataBus.onCardRead = null; NfcDataBus.onCardError = null }
    }

    Box(Modifier.fillMaxSize().background(OrionNavy)) {
        Column(Modifier.fillMaxSize()) {

            FlowHeader(currentStep = step, totalSteps = 5, onBack = {
                when {
                    step > 1 && step < 4 -> step--
                    step == 1            -> onBack()
                }
            })

            when (step) {

                1 -> StepAmount(
                    formattedAmount = formattedAmount,
                    rawAmount       = rawAmount,
                    onDigit         = { if (rawAmount.length < 9) rawAmount += it },
                    onBackspace     = { if (rawAmount.isNotEmpty()) rawAmount = rawAmount.dropLast(1) },
                    onClear         = { rawAmount = "" },
                    onNext          = { if (amountDouble > 0) step = 2 }
                )

                2 -> StepPaymentMethod(
                    amount   = formattedAmount,
                    selected = selectedProduct,
                    onSelect = { selectedProduct = it },
                    onNext   = { step = 3 }
                )

                3 -> StepNfcWait(
                    amount   = formattedAmount,
                    product  = selectedProduct,
                    status   = nfcStatus,
                    hasError = nfcError,
                    onNext   = { step = 4 }
                )

                4 -> {
                    StepProcessing(amount = formattedAmount)
                    LaunchedEffect(Unit) { submitTransaction() }
                }

                5 -> {
                    val result = txResult
                    val effectiveSuccess = result?.state == TxState.SUCCESS ||
                            result?.rawJson?.contains("already processed", ignoreCase = true) == true ||
                            result?.rawJson?.contains("idempotent",        ignoreCase = true) == true

                    val comp = if (effectiveSuccess) ComprovanteData(
                        amount        = formattedAmount,
                        product       = selectedProduct.label,
                        brand         = cardData?.brand ?: "",
                        maskedPan     = maskDisplay(cardData?.panRaw ?: ""),
                        holder        = cardData?.holder?.ifEmpty { "Não informado" } ?: "Não informado",
                        authCode      = result?.authCode ?: "",
                        nsu           = result?.nsu ?: "",
                        transactionId = result?.transactionId ?: "",
                        dateTime      = isoNow().replace("T", " "),
                        terminalSn    = terminalSn,
                        merchantId    = merchantId,
                        entryMode     = cardData?.entryMode ?: "CHIP",
                        message       = result?.message ?: ""
                    ) else null

                    StepResult(
                        result      = if (effectiveSuccess) result?.copy(state = TxState.SUCCESS) else result,
                        amount      = formattedAmount,
                        product     = selectedProduct,
                        comprovante = comp,
                        canRetry    = result?.isRetryable() == true,
                        onRetry     = {
                            txResult     = null
                            isSubmitting = false
                            step         = 4
                        },
                        onNewSale   = {
                            step           = 1
                            rawAmount      = ""
                            cardData       = null
                            txResult       = null
                            idempotencyKey = ""
                            isSubmitting   = false
                            nfcStatus      = "Aguardando cartão..."
                            nfcError       = false
                            extRef         = "PEDIDO-${System.currentTimeMillis() % 100000}"
                        },
                        onBack      = onBack
                    )
                }
            }
        }

        // Removido o overlay redundante de carregamento para manter apenas a tela azul (StepProcessing)
    }
}
