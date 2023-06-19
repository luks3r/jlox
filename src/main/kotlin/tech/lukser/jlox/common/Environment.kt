package tech.lukser.jlox.common

import tech.lukser.jlox.errors.RuntimeError

class Environment(val enclosing: Environment? = null) {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun ancestor(distance: Int): Environment {
        var environment: Environment = this
        for (i in 0 until distance) {
            environment = environment.enclosing ?: continue
        }
        return environment
    }

    operator fun get(name: Token): Any? {
        if (values.containsKey(name.lexeme)) {
            return values[name.lexeme]
        }
        if (enclosing != null) {
            return enclosing[name]
        }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'")
    }

    fun getAt(distance: Int, name: String): Any? {
        return ancestor(distance).values[name]
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme] = value
            return
        }
        if (enclosing != null) {
            enclosing.assign(name, value)
            return
        }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'")
    }

    fun assignAt(distance: Int, name: Token, value: Any?) {
        ancestor(distance).values[name.lexeme] = value
    }

    override fun toString(): String {
        val builder = StringBuilder(values.toString())
        if (enclosing!= null) {
            builder.append(" -> ")
            builder.append(enclosing.toString())
        }
        return builder.toString()
    }
}
