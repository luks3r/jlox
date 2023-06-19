package tech.lukser.jlox

import tech.lukser.jlox.common.Token
import tech.lukser.jlox.common.TokenType
import tech.lukser.jlox.errors.RuntimeError
import java.io.File
import java.nio.charset.Charset
import kotlin.system.exitProcess

object Lox {
    private val interpreter = Interpreter()
    private var hadError = false
    private var hadRuntimeError = false

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.isEmpty()) {
            runPrompt()
            return
        } else if (args.size == 1) {
            runFile(args[0])
        } else {
            println("Usage: jlox [script]")
            exitProcess(64)
        }
    }

    private fun runFile(path: String) {
        val programString = File(path).readBytes().toString(Charset.defaultCharset())
        run(programString)
        if (hadError) exitProcess(65)
        if (hadRuntimeError) exitProcess(70)
    }

    private fun runPrompt() {
        while (true) {
            print("> ")
            val line = readlnOrNull() ?: break
            run(line)
            hadError = false
        }
    }

    private fun run(source: String) {
        val scanner = Scanner(source)
        val tokens = scanner.scanTokens()
        val parser = Parser(tokens)
        val statements = parser.parse()
        if (hadError) return

        val resolver = Resolver(interpreter)
        resolver.resolve(statements.filterNotNull())
        if (hadError) return

        interpreter.interpret(statements)
    }

    fun error(line: Int, message: String) {
        report(line, "", message)
    }

    fun error(token: Token, message: String) {
        if (token.type === TokenType.EOF) {
            report(token.line, " at end", message)
        } else {
            report(token.line, " at '" + token.lexeme + "'", message)
        }
    }

    private fun report(line: Int, where: String, message: String) {
        val errorMessage = String.format("[line %d] Error %s: %s", line, where, message)
        System.err.println(errorMessage)
        hadError = true
    }

    fun runtimeError(e: RuntimeError) {
        val errorMessage = String.format("[line %d] Error: %s", e.token.line, e.message)
        System.err.println(errorMessage)
        hadRuntimeError = true
    }
}
