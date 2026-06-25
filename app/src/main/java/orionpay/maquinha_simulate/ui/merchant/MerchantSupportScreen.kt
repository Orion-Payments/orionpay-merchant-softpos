package orionpay.maquinha_simulate.ui.merchant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import orionpay.maquinha_simulate.domain.model.SupportTicketDomain
import orionpay.maquinha_simulate.infrastructure.auth.HttpTerminalAuthAdapter
import orionpay.maquinha_simulate.infrastructure.network.HttpMerchantAdapter
import orionpay.maquinha_simulate.ui.theme.*

@Composable
fun MerchantSupportScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val merchantAdapter = remember { HttpMerchantAdapter() }
    val authAdapter = remember { HttpTerminalAuthAdapter() }
    
    var tickets by remember { mutableStateOf<List<SupportTicketDomain>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            val token = authAdapter.login()
            if (token != null) {
                tickets = merchantAdapter.getTickets(token)
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
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
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
                    "Suporte Técnico",
                    color = OrionWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { /* Abrir Ticket */ }) {
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
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Seus Chamados",
                        color = OrionTextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                items(tickets) { ticket ->
                    TicketCard(ticket)
                }
            }
        }
    }
}

@Composable
fun TicketCard(ticket: SupportTicketDomain) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { /* Ver Chat */ },
        color = OrionWhite,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OrionBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OrionBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Chat, null, tint = OrionBlue, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(ticket.title, color = OrionText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(ticket.id, color = OrionTextLight, fontSize = 12.sp)
            }
            
            val statusColor = when (ticket.status) {
                "RESOLVIDO" -> OrionSuccess
                "ABERTO" -> OrionWarning
                else -> OrionBlue
            }
            
            Surface(
                color = statusColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = ticket.status,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
