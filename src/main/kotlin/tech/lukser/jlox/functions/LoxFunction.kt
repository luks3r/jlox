package tech.lukser.jlox.functions

import tech.lukser.jlox.Interpreter
import tech.lukser.jlox.common.Environment
import tech.lukser.jlox.common.Stmt
import tech.lukser.jlox.errors.Return

class LoxFunction(
    private val declaration: Stmt.Function, private val closure: Environment, private val isInitializer: Boolean = false
) : LoxCallable {

    override fun arity(): Int {
        return declaration.parameters.size
    }

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in 0 until declaration.parameters.size) {
            environment.define(declaration.parameters[i].lexeme, arguments[i])
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            if (isInitializer) return closure.getAt(0, "this")
            return returnValue.value
        }

        if (isInitializer) return closure.getAt(0, "this")
        return null
    }

    fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }
}
