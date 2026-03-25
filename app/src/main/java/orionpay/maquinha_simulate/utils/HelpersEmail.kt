package orionpay.maquinha_simulate.utils



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
import orionpay.maquinha_simulate.data.model.ComprovanteData
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*





fun isValidEmail(email: String): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

fun buildComprovanteText(data: ComprovanteData): String = buildString {
    appendLine("╔═══════════════════════════╗")
    appendLine("║     COMPROVANTE ORIONPAY  ║")
    appendLine("╚═══════════════════════════╝")
    appendLine()
    appendLine("✅ PAGAMENTO APROVADO")
    appendLine()
    appendLine("Valor         : ${data.amount}")
    appendLine("Data/Hora     : ${data.dateTime}")
    appendLine()
    appendLine("─────────────────────────────")
    appendLine("  DADOS DO PAGAMENTO")
    appendLine("─────────────────────────────")
    appendLine("Forma         : ${data.product}")
    appendLine("Bandeira      : ${data.brand.ifEmpty { "—" }}")
    appendLine("Cartão        : ${data.maskedPan.ifEmpty { "—" }}")
    appendLine("Portador      : ${data.holder}")
    appendLine("Modo entrada  : ${data.entryMode}")
    appendLine()
    appendLine("─────────────────────────────")
    appendLine("  AUTORIZAÇÃO")
    appendLine("─────────────────────────────")
    appendLine("Cód. Aut.     : ${data.authCode.ifEmpty { "—" }}")
    appendLine("NSU           : ${data.nsu.ifEmpty { "—" }}")
    appendLine("Terminal      : ${data.terminalSn}")
    appendLine("Estabelec.    : ${data.merchantId.take(18)}...")
    appendLine()
    appendLine("═════════════════════════════")
    appendLine("  OrionPay — Ambiente Seguro")
    appendLine("═════════════════════════════")
    appendLine()
    appendLine("Este comprovante é válido como")
    appendLine("prova de pagamento eletrônico.")
}

fun buildComprovanteHtml(data: ComprovanteData): String = """
<!DOCTYPE html>
<html lang="pt-BR">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<style>
  body { font-family: Arial, sans-serif; background: #f0f4f8; margin: 0; padding: 20px; }
  .card { background: #fff; border-radius: 12px; max-width: 480px; margin: 0 auto; overflow: hidden; box-shadow: 0 2px 12px rgba(0,0,0,.1); }
  .header { background: #0D1B2A; padding: 28px 24px; text-align: center; }
  .logo-row { display: flex; align-items: center; justify-content: center; gap: 10px; margin-bottom: 8px; }
  .logo-icon { width: 36px; height: 36px; background: linear-gradient(135deg,#2563EB,#10C97A); border-radius: 8px; display: flex; align-items: center; justify-content: center; }
  .logo-name { color: #fff; font-size: 20px; font-weight: 700; letter-spacing: 1px; }
  .status-chip { display: inline-block; background: #10B981; color: #fff; font-size: 13px; font-weight: 700; padding: 4px 14px; border-radius: 20px; margin-top: 4px; }
  .amount-block { background: #132338; padding: 20px 24px; text-align: center; }
  .amount-label { color: #94A3B8; font-size: 13px; }
  .amount-val { color: #fff; font-size: 36px; font-weight: 800; margin: 4px 0; }
  .amount-dt { color: #64748B; font-size: 12px; }
  .section { padding: 20px 24px 0; }
  .section-title { color: #64748B; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: .6px; margin-bottom: 12px; }
  .row { display: flex; justify-content: space-between; padding: 9px 0; border-bottom: 1px solid #f1f5f9; }
  .row:last-child { border-bottom: none; }
  .row-label { color: #94A3B8; font-size: 13px; }
  .row-val { color: #1e293b; font-size: 13px; font-weight: 600; text-align: right; }
  .auth-code { font-family: monospace; background: #f0f9ff; padding: 2px 8px; border-radius: 4px; }
  .footer { background: #0D1B2A; padding: 16px 24px; text-align: center; margin-top: 20px; }
  .footer-text { color: #64748B; font-size: 11px; }
  .green-dot { display: inline-block; width: 6px; height: 6px; border-radius: 3px; background: #10C97A; margin: 0 4px; vertical-align: middle; }
</style>
</head>
<body>
<div class="card">
  <div class="header">
    <div class="logo-row">
      <div class="logo-icon"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><path d="M12 2L4 6v6c0 5.25 3.5 10.14 8 11.29C16.5 22.14 20 17.25 20 12V6z"/></svg></div>
      <span class="logo-name">OrionPay</span>
    </div>
    <div class="status-chip">✓ PAGAMENTO APROVADO</div>
  </div>
  <div class="amount-block">
    <div class="amount-label">Valor pago</div>
    <div class="amount-val">${data.amount}</div>
    <div class="amount-dt">${data.dateTime}</div>
  </div>
  <div class="section">
    <div class="section-title">Dados do pagamento</div>
    <div class="row"><span class="row-label">Forma</span><span class="row-val">${data.product}</span></div>
    <div class="row"><span class="row-label">Bandeira</span><span class="row-val">${data.brand.ifEmpty { "—" }}</span></div>
    <div class="row"><span class="row-label">Cartão</span><span class="row-val">${data.maskedPan.ifEmpty { "—" }}</span></div>
    <div class="row"><span class="row-label">Portador</span><span class="row-val">${data.holder}</span></div>
    <div class="row"><span class="row-label">Entrada</span><span class="row-val">${data.entryMode}</span></div>
  </div>
  <div class="section" style="margin-top:8px">
    <div class="section-title">Autorização</div>
    <div class="row"><span class="row-label">Cód. autorização</span><span class="row-val"><span class="auth-code">${data.authCode.ifEmpty { "—" }}</span></span></div>
    <div class="row"><span class="row-label">NSU</span><span class="row-val">${data.nsu.ifEmpty { "—" }}</span></div>
    <div class="row"><span class="row-label">Terminal</span><span class="row-val">${data.terminalSn}</span></div>
  </div>
  <div class="footer" style="margin-top:20px">
    <div class="footer-text"><span class="green-dot"></span> OrionPay — Ambiente Seguro <span class="green-dot"></span></div>
    <div class="footer-text" style="margin-top:4px">Este comprovante é válido como prova de pagamento.</div>
  </div>
</div>
</body>
</html>
"""

// Chama API REST de envio de e-mail: POST /transactions/{transactionId}/send-email
// Retorna Pair(sucesso, mensagemErro)


