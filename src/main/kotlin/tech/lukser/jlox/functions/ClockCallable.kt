package tech.lukser.jlox.functions

import tech.lukser.jlox.Interpreter

class ClockCallable : LoxCallable {
    override fun arity(): Int = 0

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Double =
        System.currentTimeMillis() / 1000.0

    override fun toString(): String = "<native fn: clock>"
}
