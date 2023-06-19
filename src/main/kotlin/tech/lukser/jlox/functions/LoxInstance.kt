package tech.lukser.jlox.functions

import tech.lukser.jlox.common.Token
import tech.lukser.jlox.errors.RuntimeError

class LoxInstance(private val clazz: LoxClass) {
    private val fields = mutableMapOf<String, Any?>()

    fun get(name: Token): Any? {
        if (fields.containsKey(name.lexeme)) {
            return fields[name.lexeme]
        }
        val method = clazz.findMethod(name.lexeme)
        if (method != null) return method.bind(this)
        throw RuntimeError(name, "Undefined property '${name.lexeme}'")
    }

    override fun toString(): String {
        return clazz.name + " instance"
    }

    operator fun set(name: Token, value: Any?) {
        fields[name.lexeme] = value
    }
}
