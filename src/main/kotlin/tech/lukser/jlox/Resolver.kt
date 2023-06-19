package tech.lukser.jlox

import tech.lukser.jlox.common.Expr
import tech.lukser.jlox.common.Stmt
import tech.lukser.jlox.common.Token
import java.util.*

class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private val scopes: Stack<MutableMap<String, Boolean>> = Stack()
    private var currentFunction = FunctionType.NONE
    private var currentClass = ClassType.NONE

    private enum class FunctionType {
        NONE, FUNCTION, METHOD, INITIALIZER,
    }

    private enum class ClassType {
        NONE, CLASS, SUBCLASS,
    }

    override fun visitBinaryExpr(expr: Expr.Binary) {
        resolve(expr.lhs)
        resolve(expr.rhs)
    }

    override fun visitGroupingExpr(expr: Expr.Grouping) {
        resolve(expr.expression)
    }

    override fun visitLiteralExpr(expr: Expr.Literal) {
    }

    override fun visitUnaryExpr(expr: Expr.Unary) {
        resolve(expr.expression)
    }

    override fun visitVariableExpr(expr: Expr.Variable) {
        if (!scopes.empty() && scopes.peek()
                .containsKey(expr.name.lexeme) && scopes.peek()[expr.name.lexeme] == false
        ) {
            Lox.error(expr.name, "Can't read local variable '${expr.name.lexeme}' in its own initializer")
        }

        resolveLocal(expr, expr.name)
    }

    override fun visitAssignExpr(expr: Expr.Assign) {
        resolve(expr.value)
        resolveLocal(expr, expr.name)
    }

    override fun visitLogicalExpr(expr: Expr.Logical) {
        resolve(expr.lhs)
        resolve(expr.rhs)
    }

    override fun visitCallExpr(expr: Expr.Call) {
        resolve(expr.callee)
        for (arg in expr.arguments) {
            resolve(arg)
        }
    }

    override fun visitGetExpr(expr: Expr.Get) {
        resolve(expr.obj)
    }

    override fun visitSetExpr(expr: Expr.Set) {
        resolve(expr.value)
        resolve(expr.obj)
    }

    override fun visitThisExpr(expr: Expr.This) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'this' outside of a class")
        }
        resolveLocal(expr, expr.keyword)
    }

    override fun visitSuperExpr(expr: Expr.Super) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword, "Can't use 'super' outside of a class")
        } else if (currentClass == ClassType.SUBCLASS) {
            Lox.error(expr.keyword, "Can't use 'super' in a class with no superclass")
        }
        resolveLocal(expr, expr.keyword)
    }

    override fun visitExpressionStmt(stmt: Stmt.Expression) {
        resolve(stmt.expr)
    }

    override fun visitPrintStmt(stmt: Stmt.Print) {
        resolve(stmt.expr)
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        stmt.initializer?.let { resolve(it) }
        define(stmt.name)
    }

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements.filterNotNull())
        endScope()
    }

    override fun visitIfStmt(stmt: Stmt.If) {
        resolve(stmt.condition)
        resolve(stmt.thenBranch)
        stmt.elseBranch?.let { resolve(it) }
    }

    override fun visitWhileStmt(stmt: Stmt.While) {
        resolve(stmt.condition)
        resolve(stmt.body)
    }

    override fun visitFunctionStmt(stmt: Stmt.Function) {
        declare(stmt.name)
        define(stmt.name)
        resolveFunction(stmt, FunctionType.FUNCTION)
    }

    override fun visitReturnStmt(stmt: Stmt.Return) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword, "Can't return from top-level code")
        }
        stmt.value?.let {
            if (currentFunction == FunctionType.INITIALIZER) {
                Lox.error(stmt.keyword, "Can't return from initializer")
            }
            resolve(it)
        }
    }

    override fun visitClassStmt(stmt: Stmt.Class) {
        val enclosingClass = currentClass
        currentClass = ClassType.CLASS

        declare(stmt.name)
        define(stmt.name)

        stmt.superclass?.let {
            if (stmt.name.lexeme == it.name.lexeme) Lox.error(it.name, "A class can't inherit from itself")
        }
        stmt.superclass?.let { resolve(it) }
        stmt.superclass?.let {
            beginScope()
            scopes.peek()["super"] = true
        }

        beginScope()
        scopes.peek()["this"] = true

        stmt.methods.forEach {
            var declaration = FunctionType.METHOD
            if (it.name.lexeme == "init") {
                declaration = FunctionType.INITIALIZER
            }
            resolveFunction(it, declaration)
        }

        endScope()
        stmt.superclass?.let { endScope() }
        currentClass = enclosingClass
    }

    private fun beginScope() = scopes.add(mutableMapOf())

    private fun endScope() = scopes.pop()

    private fun resolveLocal(expr: Expr, name: Token) {
        for (i in (scopes.size - 1) downTo 0) {
            if (scopes[i].containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size - 1 - i)
                return
            }
        }
    }

    private fun resolveFunction(function: Stmt.Function, type: FunctionType) {
        val enclosingFunction = currentFunction
        currentFunction = type
        beginScope()
        function.parameters.forEach {
            declare(it)
            define(it)
        }
        resolve(function.body)
        endScope()
        currentFunction = enclosingFunction
    }

    private fun define(name: Token) {
        if (scopes.empty()) return
        scopes.peek()[name.lexeme] = true
    }

    private fun declare(name: Token) {
        if (scopes.empty()) return
        val scope = scopes.peek()
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Variable ${name.lexeme} is already declared in this scope")
        }
        scope[name.lexeme] = true
    }

    fun resolve(stmts: List<Stmt>) {
        for (stmt in stmts) {
            resolve(stmt)
        }
    }

    private fun resolve(stmt: Stmt) = stmt.accept(this)

    private fun resolve(expr: Expr) = expr.accept(this)

}
