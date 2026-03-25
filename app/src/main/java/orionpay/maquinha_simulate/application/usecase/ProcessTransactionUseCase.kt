package orionpay.maquinha_simulate.application.usecase

import orionpay.maquinha_simulate.domain.model.TransactionDomain
import orionpay.maquinha_simulate.domain.model.TransactionResultDomain
import orionpay.maquinha_simulate.domain.port.PaymentGatewayPort
import orionpay.maquinha_simulate.domain.port.TerminalAuthPort

/**
 * Caso de uso responsável por orquestrar o envio de uma transação.
 *
 * A lógica existente em TransactionScreen (obter token, montar payload,
 * enviar com HttpURLConnection) será gradualmente migrada para cá e para
 * a implementação concreta de PaymentGatewayPort.
 */
class ProcessTransactionUseCase(
    private val paymentGatewayPort: PaymentGatewayPort,
    private val terminalAuthPort: TerminalAuthPort
) {

    suspend operator fun invoke(
        transaction: TransactionDomain,
        idempotencyKey: String
    ): TransactionResultDomain {
        val token = terminalAuthPort.login()
        if (token.isNullOrEmpty()) {
            return TransactionResultDomain(
                state = orionpay.maquinha_simulate.domain.enums.TxState.ERROR,
                message = "Falha na autenticação do terminal"
            )
        }
        return paymentGatewayPort.process(transaction, idempotencyKey, token)
    }
}

