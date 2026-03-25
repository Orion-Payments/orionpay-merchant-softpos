package orionpay.maquinha_simulate.utils

import java.util.Calendar

fun isoNow(): String {
    val c = Calendar.getInstance()
    return "%04d-%02d-%02dT%02d:%02d:%02d".format(
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
        c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND)
    )
}
