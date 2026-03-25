package orionpay.maquinha_simulate.config

/**
 * Centralização das configurações de rede para facilitar a troca de IPs e portas.
 */
object ApiConfig {
    
    private const val API_HOST = "IP_DO_SERVIDOR_" 
    private const val API_PORT = "8080"
    
    const val BASE_URL = "http://$API_HOST:$API_PORT"
    const val API_V1_BASE = "$BASE_URL/api/v1"
    
    // Endpoints específicos
    const val LOGIN_URL = "$BASE_URL/api/auth/login"
    const val TRANSACTIONS_AUTHORIZE = "$API_V1_BASE/transactions/authorize"
    const val TRANSACTIONS_SEND_EMAIL = "$API_V1_BASE/transactions/{transactionId}/send-email"
    
    // Configurações padrão
    const val MERCHANT_ID = "3f90ed27-6eca-4bf6-a4e1-607ac55ea73b"

    // Credenciais de Autenticação
    const val AUTH_EMAIL = "admin@orionpay.com.br"
    const val AUTH_PASSWORD = "password123"

    // Configurações de Timeout (em milissegundos)
    const val CONNECT_TIMEOUT = 20_000
    const val READ_TIMEOUT = 30_000
}
