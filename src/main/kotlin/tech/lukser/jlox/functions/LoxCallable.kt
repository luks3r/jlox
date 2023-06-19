package tech.lukser.jlox.functions

import tech.lukser.jlox.Interpreter

interface LoxCallable {
    fun arity(): Int
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}
