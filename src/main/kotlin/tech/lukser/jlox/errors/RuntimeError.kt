package tech.lukser.jlox.errors

import tech.lukser.jlox.common.Token

class RuntimeError(@JvmField val token: Token, message: String?) : RuntimeException(message)
