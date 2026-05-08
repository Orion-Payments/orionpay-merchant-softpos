package orionpay.maquinha_simulate.config

/**
 * Centralização das configurações de rede para facilitar a troca de IPs e portas.
 */
object ApiConfig {
    
    private const val API_HOST = "192.168.15.187"
    private const val API_PORT = "8085"
    
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

    // Configurações de Timeout (em milissegundos)
    const val CONNECT_TIMEOUT = 20_000
    const val READ_TIMEOUT = 30_000
}
