package orionpay.maquinha_simulate.utils

// Mascara PAN, CVV e dados sensíveis antes de imprimir no Logcat
// Nunca imprima cardNumber ou cvv em texto claro — qualquer app com READ_LOGS captura
fun maskSensitiveLog(json: String): String {
    return json
        .replace(Regex(""""cardNumber"\s*:\s*"[^"]+""""))  { """"cardNumber":"****"""" }
        .replace(Regex(""""cvv"\s*:\s*"[^"]+""""))          { """"cvv":"***"""" }
        .replace(Regex(""""expirationDate"\s*:\s*"[^"]+"""")) { """"expirationDate":"**/**"""" }
        .replace(Regex(""""cardHolderName"\s*:\s*"[^"]+"""")) { m ->
            val name = m.value.substringAfter(":").trim().trim('"')
            val masked = name.take(1) + "*".repeat(maxOf(name.length - 2, 1)) + name.takeLast(1)
            """"cardHolderName":"$masked""""
        }
}
