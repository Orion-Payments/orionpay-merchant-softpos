package orionpay.maquinha_simulate.domain.port

/**
 * Porta responsável por autenticar o terminal no backend e retornar um token.
 */
interface TerminalAuthPort {
    suspend fun login(): String?
}

