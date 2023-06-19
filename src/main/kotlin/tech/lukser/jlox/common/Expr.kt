package tech.lukser.jlox.common

abstract class Expr {
    interface Visitor<T> {
        fun visitBinaryExpr(expr: Binary): T
        fun visitGroupingExpr(expr: Grouping): T
        fun visitLiteralExpr(expr: Literal): T
        fun visitUnaryExpr(expr: Unary): T
        fun visitVariableExpr(expr: Variable): T
        fun visitAssignExpr(expr: Assign): T
        fun visitLogicalExpr(expr: Logical): T
        fun visitCallExpr(expr: Call): T
        fun visitGetExpr(expr: Get): T
        fun visitSetExpr(expr: Set): T
        fun visitThisExpr(expr: This): T
        fun visitSuperExpr(expr: Super): T
    }

    abstract fun <T> accept(visitor: Visitor<T>): T

    class Binary(val lhs: Expr, val op: Token, val rhs: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitBinaryExpr(this)
    }

    class Grouping(val expression: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitGroupingExpr(this)
    }

    class Literal(val value: Any?) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitLiteralExpr(this)
    }

    class Unary(val op: Token, val expression: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitUnaryExpr(this)
    }

    class Variable(val name: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitVariableExpr(this)
    }

    class Assign(val name: Token, val value: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitAssignExpr(this)
    }

    class Logical(val lhs: Expr, val op: Token, val rhs: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitLogicalExpr(this)
    }

    class Call(val callee: Expr, val paren: Token, val arguments: List<Expr>) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitCallExpr(this)
    }

    class Get(val obj: Expr, val name: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitGetExpr(this)
    }

    class Set(val obj: Expr, val name: Token, val value: Expr) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitSetExpr(this)
    }

    class This(val keyword: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitThisExpr(this)
    }

    class Super(val keyword: Token, val method: Token) : Expr() {
        override fun <T> accept(visitor: Visitor<T>): T = visitor.visitSuperExpr(this)
    }
}
