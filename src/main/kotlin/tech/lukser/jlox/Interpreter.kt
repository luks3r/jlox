package tech.lukser.jlox

import tech.lukser.jlox.Lox.runtimeError
import tech.lukser.jlox.common.*
import tech.lukser.jlox.errors.Return
import tech.lukser.jlox.errors.RuntimeError
import tech.lukser.jlox.functions.*

class Interpreter : Expr.Visitor<Any?>, Stmt.Visitor<Unit> {
    private val locals: MutableMap<Expr, Int> = mutableMapOf()
    private val globals: Environment = Environment()
    private var environment: Environment = globals

    init {
        globals.define("clock", ClockCallable())
    }

    fun interpret(statements: List<Stmt?>) {
        try {
            statements.forEach {
                it?.let { execute(it) }
            }
        } catch (e: RuntimeError) {
            runtimeError(e)
        }
    }

    // Expressions
    override fun visitBinaryExpr(expr: Expr.Binary): Any? {
        val lhs = evaluate(expr.lhs)
        val rhs = evaluate(expr.rhs)
        when (expr.op.type) {
            TokenType.BANG_EQUAL -> {
                return !isEqual(lhs, rhs)
            }

            TokenType.EQUAL_EQUAL -> {
                return isEqual(lhs, rhs)
            }

            TokenType.GREATER -> {
                checkNumberOperands(expr.op, lhs, rhs)
                return lhs as Double > rhs as Double
            }

            TokenType.GREATER_EQUAL -> {
                checkNumberOperands(expr.op, lhs, rhs)
                return lhs as Double >= rhs as Double
            }

            TokenType.LESS -> {
                checkNumberOperands(expr.op, lhs, rhs)
                return (lhs as Double) < rhs as Double
            }

            TokenType.LESS_EQUAL -> {
                checkNumberOperands(expr.op, lhs, rhs)
                return lhs as Double <= rhs as Double
            }

            TokenType.MINUS -> {
                checkNumberOperands(expr.op, lhs, rhs)
                return lhs as Double - rhs as Double
            }

            TokenType.PLUS -> {
                if (lhs is Double && rhs is Double) return lhs + rhs
                if (lhs is String && rhs is String) return lhs + rhs
                throw RuntimeError(expr.op, "Can only add numbers or strings")
            }

            TokenType.STAR -> {
                checkNumberOperands(expr.op, lhs, rhs)
                return lhs as Double * rhs as Double
            }

            TokenType.SLASH -> {
                checkNumberOperands(expr.op, lhs, rhs)
                return lhs as Double / rhs as Double
            }

            else -> {
                return null
            }
        }
    }

    override fun visitGroupingExpr(expr: Expr.Grouping): Any? {
        return evaluate(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal): Any? {
        return expr.value
    }

    override fun visitUnaryExpr(expr: Expr.Unary): Any? {
        val rhl = evaluate(expr.expression)
        return when (expr.op.type) {
            TokenType.BANG -> {
                !isTruthy(rhl)
            }

            TokenType.MINUS -> {
                checkNumberOperands(expr.op, rhl)
                -(rhl as Double)
            }

            else -> {
                null
            }
        }
    }

    override fun visitVariableExpr(expr: Expr.Variable): Any? {
        return lookUpVariable(expr.name, expr)
    }

    override fun visitAssignExpr(expr: Expr.Assign): Any? {
        val value = evaluate(expr.value)

        val distance = locals[expr]
        if (distance != null) {
            environment.assignAt(distance, expr.name, value)
        } else {
            globals.assign(expr.name, value)
        }
        return value
    }

    override fun visitLogicalExpr(expr: Expr.Logical): Any? {
        val lhs = evaluate(expr.lhs)
        if (expr.op.type === TokenType.OR) {
            if (isTruthy(lhs)) return lhs
        } else {
            if (!isTruthy(lhs)) return lhs
        }
        return evaluate(expr.rhs)
    }

    override fun visitCallExpr(expr: Expr.Call): Any? {
        val callee = evaluate(expr.callee)
        val arguments: MutableList<Any?> = ArrayList()
        for (argument in expr.arguments) {
            arguments.add(evaluate(argument))
        }
        if (callee !is LoxCallable) {
            throw RuntimeError(expr.paren, "Can only call functions and classes")
        }
        val function: LoxCallable = callee
        if (arguments.size != function.arity()) {
            throw RuntimeError(
                expr.paren,
                "Expected " + function.arity() + " arguments, got " + arguments.size,
            )
        }
        return function.call(this, arguments)
    }

    override fun visitGetExpr(expr: Expr.Get): Any? {
        val obj = evaluate(expr.obj)
        if (obj is LoxInstance) {
            return obj.get(expr.name)
        }
        throw RuntimeError(expr.name, "Only instances can have properties")
    }

    override fun visitSetExpr(expr: Expr.Set): Any? {
        val obj = evaluate(expr.obj)
        if (obj !is LoxInstance) {
            throw RuntimeError(expr.name, "Only instances can have fields")
        }
        val value = evaluate(expr.value)
        obj[expr.name] = value
        return value
    }

    override fun visitThisExpr(expr: Expr.This): Any? {
        return lookUpVariable(expr.keyword, expr)
    }

    override fun visitSuperExpr(expr: Expr.Super): Any? {
        val distance = locals[expr] ?: return null
        val superclass = environment.getAt(distance, "super") as LoxClass
        val obj = environment.getAt(distance - 1, "this") as LoxInstance
        val method = superclass.findMethod(expr.method.lexeme) ?: throw RuntimeError(
            expr.method, "Undefined property '${expr.method.lexeme}'"
        )
        return method.bind(obj)
    }

    // Statements

    override fun visitReturnStmt(stmt: Stmt.Return) {
        var value: Any? = null
        stmt.value?.let {
            value = evaluate(it)
        }
        throw Return(value)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        evaluate(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        val value = evaluate(stmt.expr)
        println(stringify(value))
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        var value: Any? = null
        stmt.initializer?.let {
            value = evaluate(it)
        }
        environment.define(stmt.name.lexeme, value)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        executeBlock(stmt.statements, Environment(environment))
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch)
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch)
        }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body)
        }
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        val function = LoxFunction(stmt, environment, false)
        environment.define(stmt.name.lexeme, function)
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        var superclass: Any? = null
        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass)
            if (superclass !is LoxClass) {
                throw RuntimeError(stmt.superclass.name, "Superclass must be a class")
            }
        }

        environment.define(stmt.name.lexeme, null)

        stmt.superclass?.let {
            environment = Environment(environment)
            environment.define("super", superclass)
        }

        val methods = mutableMapOf<String, LoxFunction>()
        stmt.methods.forEach {
            methods[it.name.lexeme] = LoxFunction(it, environment, it.name.lexeme == "init")
        }
        val clazz = LoxClass(stmt.name.lexeme, superclass as LoxClass?, methods)

        stmt.superclass?.let {
            environment.enclosing?.let { environment = it }
        }
        environment.assign(stmt.name, clazz)
    }

    // Helpers
    fun executeBlock(statements: List<Stmt?>, environment: Environment) {
        val previous = this.environment
        try {
            this.environment = environment
            statements.forEach { it?.let { execute(it) } }
        } finally {
            this.environment = previous
        }
    }

    fun resolve(expr: Expr, depth: Int) {
        locals[expr] = depth
    }

    private fun lookUpVariable(name: Token, expr: Expr): Any? {
        val distance = locals[expr]
        if (distance != null) {
            return environment.getAt(distance, name.lexeme)
        } else {
            return globals[name]
        }
    }

    private fun isTruthy(rhl: Any?): Boolean {
        if (rhl == null) return false
        return if (rhl is Boolean) rhl else true
    }

    private fun checkNumberOperands(op: Token, vararg operands: Any?) {
        for (operand in operands) if (operand is Double) return
        throw RuntimeError(op, "Operand must be a number")
    }

    private fun evaluate(expr: Expr): Any? {
        return expr.accept(this)
    }

    private fun stringify(value: Any?): String {
        if (value == null) return "nil"
        if (value is Double) {
            var text = value.toString()
            if (text.endsWith(".0")) text = text.substring(0, text.length - 2)
            return text
        }
        return value.toString()
    }

    private fun isEqual(lhs: Any?, rhs: Any?): Boolean {
        if (lhs == null && rhs == null) return true
        return if (lhs == null) false else lhs == rhs
    }

    private fun execute(statement: Stmt) {
        statement.accept(this)
    }
}
