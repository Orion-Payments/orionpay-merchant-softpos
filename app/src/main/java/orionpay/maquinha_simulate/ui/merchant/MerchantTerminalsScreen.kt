package orionpay.maquinha_simulate.ui.merchant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import orionpay.maquinha_simulate.domain.model.MerchantTerminalDomain
import orionpay.maquinha_simulate.infrastructure.auth.HttpTerminalAuthAdapter
import orionpay.maquinha_simulate.infrastructure.network.HttpMerchantAdapter
import orionpay.maquinha_simulate.ui.theme.*

@Composable
fun MerchantTerminalsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val merchantAdapter = remember { HttpMerchantAdapter() }
    val authAdapter = remember { HttpTerminalAuthAdapter() }
    
    var terminals by remember { mutableStateOf<List<MerchantTerminalDomain>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val token = authAdapter.login()
            if (token != null) {
                terminals = merchantAdapter.getTerminals(token)
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OrionBackground)
    ) {
        Surface(
            color = OrionBlue,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = OrionWhite)
                }
                Text(
                    "Meus Terminais",
                    color = OrionWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { /* Adicionar */ }) {
                    Icon(Icons.Default.Add, null, tint = OrionWhite)
                }
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = OrionBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                items(terminals) { terminal ->
                    TerminalCard(terminal)
                }
            }
        }
    }
}

@Composable
fun TerminalCard(terminal: MerchantTerminalDomain) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = OrionWhite,
            title = { Text("Bloquear Terminal?", color = OrionText, fontWeight = FontWeight.Bold) },
            text = { Text("Esta ação impedirá vendas neste terminal temporariamente.", color = OrionTextLight) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("BLOQUEAR", color = OrionError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("CANCELAR", color = OrionTextLight)
                }
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        color = OrionWhite,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, OrionBackground)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrionBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PointOfSale, null, tint = OrionBlue, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(terminal.model, color = OrionText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(terminal.serialNumber, color = OrionTextLight, fontSize = 13.sp)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val statusColor = if (terminal.isActive) OrionSuccess else OrionTextLight
                val statusText = if (terminal.isActive) "ATIVO" else "INATIVO"
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(statusText, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.LockOpen, null, tint = OrionBlue)
            }
        }
    }
}
