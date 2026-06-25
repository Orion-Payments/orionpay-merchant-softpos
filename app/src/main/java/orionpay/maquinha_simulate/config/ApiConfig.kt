package orionpay.maquinha_simulate.config

/**
 * Centralização das configurações de rede para facilitar a troca de IPs e portas.
 */
object ApiConfig {
    
    // Se estiver usando EMULADOR, use "10.0.2.2"
    // Se estiver usando DISPOSITIVO FÍSICO, use o IP da sua máquina (ex: "192.168.15.187")
    const val API_HOST = "192.168.15.187" // <--- IP DA MÁQUINA (DISPOSITIVO FÍSICO)
    const val API_PORT = "8085"
    
    const val BASE_URL = "http://$API_HOST:$API_PORT"
    const val API_V1_BASE = "$BASE_URL/api/v1"
    
    // Endpoints específicos
    const val LOGIN_URL = "$BASE_URL/api/auth/login"
    const val TRANSACTIONS_AUTHORIZE = "$API_V1_BASE/transactions/authorize"
    const val TRANSACTIONS_SEND_EMAIL = "$API_V1_BASE/transactions/{transactionId}/send-email"
    
    // Configurações padrão
    const val MERCHANT_ID = "0f6ac19c-1bc2-43f8-a289-d0cf39615f02"

    // Credenciais de Autenticação
    const val AUTH_EMAIL = "chicoaraujo1063@gmail.com"
    const val AUTH_PASSWORD = "Admin@123456"

    // Configurações de Timeout (Reduzido para falhar mais rápido se a rede estiver ruim)
    const val CONNECT_TIMEOUT = 20_000
    const val READ_TIMEOUT = 15_000
}
