package orionpay.maquinha_simulate.domain.enums

enum class ProductType(val label: String, val apiKey: String) {
    CREDIT_AVISTA("Crédito à vista", "CREDIT_A_VISTA"),
    CREDIT_2X("Crédito 2x",          "CREDIT_PARCELADO_2"),
    CREDIT_3X("Crédito 3x",          "CREDIT_PARCELADO_3"),
    CREDIT_6X("Crédito 6x",          "CREDIT_PARCELADO_6"),
    CREDIT_12X("Crédito 12x",        "CREDIT_PARCELADO_12"),
    DEBIT("Débito",                   "DEBIT"),
    PIX("Pix",                        "PIX"),
    MANUAL("Venda Manual",            "MANUAL"),
    CREDIT_A_VISTA("Crédito à vista", "CREDIT_A_VISTA"); // Alias para evitar IllegalArgumentException durante migração

    val isInstallment: Boolean
        get() = name.startsWith("CREDIT_") && name != "CREDIT_AVISTA" && name != "CREDIT_A_VISTA"
}
