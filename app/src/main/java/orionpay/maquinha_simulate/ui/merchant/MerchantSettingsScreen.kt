package orionpay.maquinha_simulate.ui.merchant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import orionpay.maquinha_simulate.domain.model.MerchantDomain
import orionpay.maquinha_simulate.infrastructure.auth.HttpTerminalAuthAdapter
import orionpay.maquinha_simulate.infrastructure.network.HttpMerchantAdapter
import orionpay.maquinha_simulate.ui.theme.*

@Composable
fun MerchantSettingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val merchantAdapter = remember { HttpMerchantAdapter() }
    val authAdapter = remember { HttpTerminalAuthAdapter() }
    
    var merchantInfo by remember { mutableStateOf<MerchantDomain?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val token = authAdapter.login()
            if (token != null) {
                merchantInfo = merchantAdapter.getMerchantMe(token)
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrionBackground)
    ) {
        // Header no estilo OrionBlue com cantos arredondados na base
        Surface(
            color = OrionBlue,
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 24.dp, top = 48.dp, bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OrionWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Configurações",
                    color = OrionWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrionBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                ProfileSection(merchantInfo)
                
                Spacer(modifier = Modifier.height(32.dp))
                
                SettingsGroup("Segurança") {
                    SettingsItem(
                        title = "Login Biométrico",
                        subtitle = "Acessar app com digital",
                        icon = Icons.Default.Fingerprint,
                        trailing = {
                            var checked by remember { mutableStateOf(true) }
                            Switch(
                                checked = checked,
                                onCheckedChange = { checked = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OrionWhite,
                                    checkedTrackColor = OrionBlue
                                )
                            )
                        }
                    )
                    SettingsItem(
                        title = "Autenticação 2FA",
                        subtitle = "Código via SMS/E-mail",
                        icon = Icons.Default.Security,
                        onClick = {}
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                SettingsGroup("Configurações do Merchant") {
                    SettingsItem(
                        title = "Perfil de Taxas",
                        subtitle = "Consultar meu MDR",
                        icon = Icons.Default.Percent,
                        onClick = {}
                    )
                    SettingsItem(
                        title = "Dados Bancários",
                        subtitle = "Onde recebo minhas vendas",
                        icon = Icons.Default.AccountBalance,
                        onClick = {}
                    )
                    SettingsItem(
                        title = "Notificações Push",
                        subtitle = "Alertas de vendas e pagamentos",
                        icon = Icons.Default.NotificationsActive,
                        onClick = {}
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                TextButton(
                    onClick = { /* Logout */ },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("SAIR DA CONTA", color = OrionError, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ProfileSection(merchant: MerchantDomain?) {
    Surface(
        color = OrionWhite,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, OrionBackground)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(OrionBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                val initials = if (!merchant?.name.isNullOrBlank()) {
                    merchant!!.name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString("").uppercase()
                } else "OP"
                Text(initials, color = OrionBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    merchant?.name ?: "Lojista Orion",
                    color = OrionText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    merchant?.document ?: "CPF/CNPJ não informado",
                    color = OrionTextLight,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            color = OrionTextLight,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        Surface(
            color = OrionWhite,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, OrionBackground)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OrionBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = OrionBlue, modifier = Modifier.size(20.dp))
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = OrionText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(subtitle, color = OrionTextLight, fontSize = 12.sp)
        }
        
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = OrionTextLight)
        }
    }
}
