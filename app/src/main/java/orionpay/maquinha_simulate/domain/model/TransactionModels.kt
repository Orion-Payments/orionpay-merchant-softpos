package orionpay.maquinha_simulate.domain.model

import org.json.JSONObject
import orionpay.maquinha_simulate.data.model.TxResult

interface PaymentRepository {
    suspend fun loginTerminal(): String?
    suspend fun authorize(payload:
                          JSONObject, token: String, idempotencyKey: String): TxResult
    suspend fun sendEmail(transactionId: String, email: String): Pair<Boolean, String>
}