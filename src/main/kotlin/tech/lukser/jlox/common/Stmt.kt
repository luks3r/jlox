package tech.lukser.jlox.common

abstract class Stmt {
    interface Visitor<T> {
        fun visitExpressionStmt(stmt: Expression): T
        fun visitPrintStmt(stmt: Print): T
        fun visitVarStmt(stmt: Var): T
        fun visitBlockStmt(stmt: Block): T
        fun visitIfStmt(stmt: If): T
        fun visitWhileStmt(stmt: While): T
        fun visitFunctionStmt(stmt: Function): T
        fun visitReturnStmt(stmt: Return): T
        fun visitClassStmt(stmt: Class): T
    }

    abstract fun <T> accept(visitor: Visitor<T>): T

    class Expression(val expr: Expr) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitExpressionStmt(this)
    }

    class Print(val expr: Expr) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitPrintStmt(this)
    }

    class Var(val name: Token, val initializer: Expr?) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitVarStmt(this)
    }

    class Block(val statements: List<Stmt?>) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitBlockStmt(this)
    }

    class If(val condition: Expr, val thenBranch: Stmt, val elseBranch: Stmt?) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitIfStmt(this)
    }

    class While(val condition: Expr, val body: Stmt) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitWhileStmt(this)
    }

    class Function(val name: Token, val parameters: List<Token>, val body: List<Stmt>) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitFunctionStmt(this)
    }

    class Return(val keyword: Token, val value: Expr?) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitReturnStmt(this)
    }

    class Class(val name: Token, val superclass: Expr.Variable?, val methods: List<Function>) : Stmt() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitClassStmt(this)
    }
}
