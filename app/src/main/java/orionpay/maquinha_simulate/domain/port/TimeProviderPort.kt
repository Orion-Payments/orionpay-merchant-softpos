package orionpay.maquinha_simulate.domain.port

/** Abstração de fonte de tempo, útil para testes e para manter o domínio puro. */
interface TimeProviderPort {
    fun nowIso(): String
}

