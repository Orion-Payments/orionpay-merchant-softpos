package orionpay.maquinha_simulate.domain.port

import orionpay.maquinha_simulate.domain.model.TransactionDomain
import orionpay.maquinha_simulate.domain.model.TransactionResultDomain

/**
 * Porta de saída para o gateway de pagamento.
 * Implementações concretas (HTTP, mock, etc.) vivem na camada infrastructure.
 */
interface PaymentGatewayPort {
    suspend fun process(
        transaction: TransactionDomain,
        idempotencyKey: String,
        token: String
    ): TransactionResultDomain
}

