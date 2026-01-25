package uz.vv.gateway

fun String.extractServiceName(): String? {
    val regex = Regex("^/api/v\\d+/([^/]+)")
    return regex.find(this)?.groupValues?.get(1)
}

fun <T> Class<T>.getIsRoutedKey(): String {
    return "${this.name}.entered"
}