package tech.lukser.jlox.errors

class Return(val value: Any?) : RuntimeException(null, null, false, false) {
}
