import kotlin.system.measureNanoTime

typealias BitSet = BitSet128

fun main(args: Array<String>) {

    solveAndReport(arrayOf(
            intArrayOf(8, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 3, 6, 0, 0, 0, 0, 0),
            intArrayOf(0, 7, 0, 0, 9, 0, 2, 0, 0),
            intArrayOf(0, 5, 0, 0, 0, 7, 0, 0, 0),
            intArrayOf(0, 0, 0, 0, 4, 5, 7, 0, 0),
            intArrayOf(0, 0, 0, 1, 0, 0, 0, 3, 0),
            intArrayOf(0, 0, 1, 0, 0, 0, 0, 6, 8),
            intArrayOf(0, 0, 8, 5, 0, 0, 0, 1, 0),
            intArrayOf(0, 9, 0, 0, 0, 0, 4, 0, 0)
    ))
    solveAndReport(arrayOf(
            intArrayOf(0, 5, 0, 1, 0, 4, 6, 0, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 5),
            intArrayOf(6, 0, 9, 0, 2, 0, 4, 0, 0),
            intArrayOf(8, 0, 0, 0, 3, 0, 0, 0, 9),
            intArrayOf(0, 0, 5, 8, 0, 2, 3, 0, 0),
            intArrayOf(7, 0, 0, 0, 5, 0, 0, 0, 2),
            intArrayOf(0, 0, 7, 0, 8, 0, 1, 0, 3),
            intArrayOf(9, 0, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 0, 4, 3, 0, 6, 0, 2, 0))
    )
    solveAndReport(arrayOf(
            intArrayOf(3, 0, 6, 5, 0, 8, 4, 0, 0),
            intArrayOf(5, 2, 0, 0, 0, 0, 0, 0, 0),
            intArrayOf(0, 8, 7, 0, 0, 0, 0, 3, 1),
            intArrayOf(0, 0, 3, 0, 1, 0, 0, 8, 0),
            intArrayOf(9, 0, 0, 8, 6, 3, 0, 0, 5),
            intArrayOf(0, 5, 0, 0, 9, 0, 6, 0, 0),
            intArrayOf(1, 3, 0, 0, 0, 0, 2, 5, 0),
            intArrayOf(0, 0, 0, 0, 0, 0, 0, 7, 4),
            intArrayOf(0, 0, 5, 2, 0, 6, 3, 0, 0))
    )
}

private fun solveAndReport(grid: Array<IntArray>) {
    val sudoku = Sudoku.create(grid)
    println(sudoku)
    var solved = false
    val elapsed = measureNanoTime {
        solved = sudoku.solve(0, 0)
    }
    println("--".repeat(9) + "\n${sudoku}\nsolved: $solved in ${elapsed / 1e6}ms")
}

class BitSet128() {
    var bits0x63 : Long = 0L
    var bits64x127 : Long = 0
    fun set(pos : Int) : BitSet128 {
        when {
            pos < 64 -> bits0x63 = bits0x63 or (1L shl pos)
            else -> bits64x127 = bits64x127 or (1L shl (pos - 64))
        }
        return this
    }

    fun clear(pos : Int) : BitSet128 {
        when {
            pos < 64 -> bits0x63 = bits0x63 and  ((1L shl pos).inv())
            else -> bits64x127 = bits64x127 and ((1L shl (pos - 64)).inv())
        }
        return this
    }

    operator fun get(pos : Int) : Boolean {
        return when {
            pos < 64 -> bits0x63 and (1L shl pos) != 0L
            else -> bits64x127 and (1L shl (pos - 64)) != 0L
        }
    }

    fun intersects(o : BitSet128) : Boolean =
        (bits0x63 and o.bits0x63 != 0L) || (bits64x127 and o.bits64x127 != 0L)
}

class Sudoku private constructor(val digits: Array<BitSet>) {

    fun solve(x : Int, y : Int) : Boolean {
        if (y > 8) return true
        if (!isEmpty(x, y)) {
            if (x < 8) return solve(x + 1, y)
            else if (y < 8) return solve(0, y + 1)
            return true
        }
        else {
            for(digit : Int in  1..9)
                if (canAllocate(digit, x, y)) {
                    allocate(x, y, digit)
                    if (x == 8) {
                        if (solve(0, y + 1)) return true
                    } else if (solve(x + 1, y)) return true
                    deallocate(x, y, digit)
                }
            return false
        }
    }

    fun allocate(x: Int, y: Int, digit : Int) {
        val pos = index(x, y)
        digits[0].set(pos)
        digits[digit].set(pos)
    }

    fun deallocate(x: Int, y: Int, digit : Int) {
        val pos = index(x, y)
        digits[0].clear(pos)
        digits[digit].clear(pos)
    }

    fun canAllocate(digit: Int, x: Int, y: Int): Boolean {
        val bits = digits[digit]
        return when {
            validityMasks[index(x, y)].intersects(bits) -> false
            else -> true
        }
    }

    fun isEmpty(x: Int, y: Int): Boolean = !digits[0][index(x,y)]

    override fun toString(): String {
        val sb = StringBuilder()
        for (y in 0..8) {
            for (x in 0..8) {
                if (!isEmpty(x, y)) {
                    val pos = index(x, y)
                    val digit = (1..9).first { digits[it][pos] }
                    sb.append(' ').append(digit)
                }
                else {
                    sb.append(" .")
                }
            }
            sb.appendln()
        }
        return sb.toString()
    }


    companion object {
        val validityMasks: Array<BitSet> = (0..80)
            .map { index ->
                BitSet().apply {
                    val x = index % 9
                    val y = index / 9
                    for (row in 0..8)
                        this.set(index(x, row))
                    for (column in 0..8)
                        this.set(index(column, y))
                    val blockX = (index % 9) / 3 * 3
                    val blockY = (index / 27) * 3
                    for (bx in blockX..blockX + 2)
                        for (by in blockY..blockY + 2)
                            this.set(index(bx, by))
                }
            }
            .toTypedArray()

        fun index(x: Int, y: Int) = y * 9 + x

        fun create(grid: Array<IntArray>) : Sudoku {
            val digits = Array(10) { _ -> BitSet() }

            for (y in 0..8)
                for (x in 0..8) {
                    val pos = index(x, y)
                    val digit = grid[y][x]
                    if (digit > 0 ) {
                        digits[digit].set(pos)
                        digits[0].set(pos)
                    }
                }
            return Sudoku(digits)
        }
    }
}