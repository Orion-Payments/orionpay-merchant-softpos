package orionpay.maquinha_simulate.infrastructure.time

import orionpay.maquinha_simulate.domain.port.TimeProviderPort
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** Implementação padrão de TimeProviderPort baseada no relógio do sistema. */
class SystemTimeProvider : TimeProviderPort {
    override fun nowIso(): String =
        OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}

