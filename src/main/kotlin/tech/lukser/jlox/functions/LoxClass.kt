package tech.lukser.jlox.functions

import tech.lukser.jlox.Interpreter

class LoxClass(val name: String, private val superclass: LoxClass?, private val methods: Map<String, LoxFunction>) : LoxCallable {
    override fun arity(): Int {
        val initializer = findMethod("init") ?: return 0
        return initializer.arity()
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val instance = LoxInstance(this)
        val initializer = findMethod("init")
        initializer?.bind(instance)?.call(interpreter, arguments)
        return instance
    }

    fun findMethod(name: String): LoxFunction? {
        if (methods.containsKey(name)) return methods[name]
        return null
    }

    override fun toString(): String {
        return super.toString()
    }
}
