package processor

import java.util.Scanner

class IncompatibleDimensionException(reason: String): Exception(reason)
class ZeroDeterminantException(reason: String): Exception(reason)

data class Matrix(val rows: Int, val columns: Int) {

    private val body: Array<DoubleArray> = Array(rows) { DoubleArray(columns) { 0.0 } }

    data class MatrixIndex(val i: Int, val j: Int)

    val indices: List<MatrixIndex>
        get() = List(rows * columns) { MatrixIndex((it / columns) + 1, (it % columns) + 1) }

    val det: Double?
        get() = if (rows != columns) null else calcDeterminant(this)

    constructor(rows: Int, columns: Int, rule: (Int, Int) -> Double) : this(rows, columns) {
        for ((i, j) in indices)
            this[i, j] = rule(i, j)
    }

    constructor(rows: Int, columns: Int, rule: () -> Double) : this(rows, columns, { _, _ -> rule() })

    operator fun get(i: Int, j: Int) = body[i - 1][j - 1]

    operator fun set(i: Int, j: Int, value: Double) { body[i - 1][j - 1] = value }

    operator fun plus(other: Matrix): Matrix {
        if (rows != other.rows || columns != other.columns)
            throw IncompatibleDimensionException("Impossible to sum matrices with different dimensions: " +
                    "($rows, $columns) != (${other.rows}, ${other.columns})")
        return Matrix(rows, columns) { i, j -> this[i, j] + other[i, j] }
    }

    operator fun times(scalar: Double) = Matrix(rows, columns) { i, j -> this[i, j] * scalar }

    operator fun times(other: Matrix): Matrix {
        if (columns != other.rows)
            throw IncompatibleDimensionException("Impossible to multiply matrices with these dimensions: " +
                    "($rows, $columns) and (${other.rows}, ${other.columns})")
        return Matrix(rows, other.columns) { i, j ->
            var sum = 0.0
            for (k in 1..columns)
                sum += this[i, k] * other[k, j]
            sum
        }
    }

    operator fun not(): Matrix {
        val tDet = det ?: throw IncompatibleDimensionException("Impossible to reverse non-square matrix")
        if (tDet == 0.0)
            throw ZeroDeterminantException("Impossible to reverse matrix with zero determinant")
        return Matrix(rows, columns) { i, j -> calcCofactor(this, i, j) }.transposeMain() * (1 / (tDet))
    }

    fun transposeMain() = Matrix(columns, rows) { i, j -> this[j, i] }

    fun transposeSide() = Matrix(columns, rows) { i, j -> this[columns + 1 - j, rows + 1 - i] }

    fun transposeVer() = Matrix(rows, columns) { i, j -> this[i, columns + 1 - j] }

    fun transposeHor() = Matrix(rows, columns) { i, j -> this[rows + 1 - i, j] }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Matrix

        if (rows != other.rows) return false
        if (columns != other.columns) return false
        if (!body.contentDeepEquals(other.body)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rows
        result = 31 * result + columns
        result = 31 * result + body.contentDeepHashCode()
        return result
    }

    companion object {

        private fun calcCofactor(matrix: Matrix, i: Int, j: Int): Double {
            return calcCofactorSign(i, j) * calcMinor(matrix, i, j)
        }

        private fun calcMinor(matrix: Matrix, n: Int, m: Int): Double {
            if (matrix.rows == 2)
                return matrix[3 - n, 3 - m]
            val iterator = matrix.indices.filter { (i, j) -> i != n && j != m } .iterator()
            return calcDeterminant(Matrix(matrix.rows - 1, matrix.columns - 1) { ->
                val chunk = iterator.next()
                matrix[chunk.i, chunk.j]
            })
        }

        private fun calcCofactorSign(i: Int, j: Int) = if ((i + j) % 2 == 0) 1 else -1

        private fun calcDeterminant(matrix: Matrix): Double {
            if (matrix.rows == 1) return matrix[1, 1]
            var sum = 0.0
            for (j in 1..matrix.columns)
                sum += matrix[1, j] * calcCofactor(matrix, 1, j)
            return sum
        }

    }

}

fun print(matrix: Matrix) {
    for ((i, j) in matrix.indices)
        print("${matrix[i, j]}${if (j == matrix.columns) '\n' else ' '}")
}

fun main() {
    val scanner = Scanner(System.`in`)
    loop@ while (true) {
        print("1. Add matrices\n" +
                "2. Multiply matrix to a constant\n" +
                "3. Multiply matrices\n" +
                "4. Transpose matrix\n" +
                "5. Calculate a determinant\n" +
                "6. Inverse matrix\n" +
                "0. Exit\n" +
                "Your choice: ")
        when (scanner.next()) {
            "1" -> {
                println("Enter size of first matrix:")
                val n1 = scanner.nextInt()
                val m1 = scanner.nextInt()
                println("Enter first matrix:")
                val a = Matrix(n1, m1) { -> scanner.nextDouble() }

                println("Enter size of second matrix:")
                val n2 = scanner.nextInt()
                val m2 = scanner.nextInt()
                println("Enter second matrix:")
                val b = Matrix(n2, m2) { -> scanner.nextDouble() }

                println("The addition result is:")
                print(a + b)
            }
            "2" -> {
                println("Enter size of matrix:")
                val n = scanner.nextInt()
                val m = scanner.nextInt()
                println("Enter matrix:")
                val a = Matrix(n, m) { -> scanner.nextDouble() }

                println("Enter constant:")
                val x = scanner.nextDouble()

                println("The multiplication result is:")
                print(a * x)
            }
            "3" -> {
                println("Enter size of first matrix:")
                val n1 = scanner.nextInt()
                val m1 = scanner.nextInt()
                println("Enter first matrix:")
                val a = Matrix(n1, m1) { -> scanner.nextDouble() }

                println("Enter size of second matrix:")
                val n2 = scanner.nextInt()
                val m2 = scanner.nextInt()
                println("Enter second matrix:")
                val b = Matrix(n2, m2) { -> scanner.nextDouble() }

                println("The multiplication result is:")
                print(a * b)
            }
            "4" -> {
                print("\n1. Main diagonal\n" +
                        "2. Side diagonal\n" +
                        "3. Vertical line\n" +
                        "4. Horizontal line\n" +
                        "Your choice: ")
                val choice = scanner.next()
                println("Enter matrix size:")
                val n = scanner.nextInt()
                val m = scanner.nextInt()
                println("Enter matrix:")
                val a = Matrix(n, m) { -> scanner.nextDouble() }
                println("The result is:")
                when (choice) {
                    "1" -> print(a.transposeMain())
                    "2" -> print(a.transposeSide())
                    "3" -> print(a.transposeVer())
                    "4" -> print(a.transposeHor())
                }
            }
            "5" -> {
                println("Enter matrix size:")
                val n = scanner.nextInt()
                val m = scanner.nextInt()
                println("Enter matrix:")
                val a = Matrix(n, m) { -> scanner.nextDouble() }
                println("The result is:")
                println(a.det)
            }
            "6" -> {
                println("Enter matrix size:")
                val n = scanner.nextInt()
                val m = scanner.nextInt()
                println("Enter matrix:")
                val a = Matrix(n, m) { -> scanner.nextDouble() }
                println("The result is:")
                print(!a)
            }
            "0" -> {
                break@loop
            }
        }
        println()
    }
}
