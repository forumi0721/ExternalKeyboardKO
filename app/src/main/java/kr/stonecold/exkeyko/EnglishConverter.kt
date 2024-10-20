package kr.stonecold.exkeyko

import android.util.Log

/**
 * QWERTY에서 Colemak 및 Dvorak으로 키 매핑을 담당하는 클래스
 * 레이아웃을 동적으로 선택할 수 있습니다.
 */
class EnglishConverter {

    private var layoutType = "q" // 기본값은 QWERTY 레이아웃

    private val qwertyToColemakLowerCaseMap = mapOf(
        'q' to 'q', 'w' to 'w', 'e' to 'f', 'r' to 'p', 't' to 'g',
        'y' to 'j', 'u' to 'l', 'i' to 'u', 'o' to 'y', 'p' to ';',
        'a' to 'a', 's' to 'r', 'd' to 's', 'f' to 't', 'g' to 'd',
        'h' to 'h', 'j' to 'n', 'k' to 'e', 'l' to 'i', ';' to 'o',
        'z' to 'z', 'x' to 'x', 'c' to 'c', 'v' to 'v', 'b' to 'b',
        'n' to 'k', 'm' to 'm'
    )
    private val qwertyToColemakUpperCaseMap = mapOf(
        'Q' to 'Q', 'W' to 'W', 'E' to 'F', 'R' to 'P', 'T' to 'G',
        'Y' to 'J', 'U' to 'L', 'I' to 'U', 'O' to 'Y', 'P' to ':',
        'A' to 'A', 'S' to 'R', 'D' to 'S', 'F' to 'T', 'G' to 'D',
        'H' to 'H', 'J' to 'N', 'K' to 'E', 'L' to 'I', ':' to 'O',
        'Z' to 'Z', 'X' to 'X', 'C' to 'C', 'V' to 'V', 'B' to 'B',
        'N' to 'K', 'M' to 'M'
    )

    private val qwertyToDvorakLowerCaseMap = mapOf(
        'q' to '\'', 'w' to ',', 'e' to '.', 'r' to 'p', 't' to 'y',
        'y' to 'f', 'u' to 'g', 'i' to 'c', 'o' to 'r', 'p' to 'l',
        'a' to 'a', 's' to 'o', 'd' to 'e', 'f' to 'u', 'g' to 'i',
        'h' to 'd', 'j' to 'h', 'k' to 't', 'l' to 'n', 'n' to ';',
        'z' to '/', 'x' to 'q', 'c' to 'j', 'v' to 'k', 'b' to 'x',
        'm' to 'w', ',' to 'v', '.' to 'z', '/' to '/'
    )
    private val qwertyToDvorakUpperCaseMap = mapOf(
        'Q' to '"', 'W' to '<', 'E' to '>', 'R' to 'P', 'T' to 'Y',
        'Y' to 'F', 'U' to 'G', 'I' to 'C', 'O' to 'R', 'P' to 'L',
        'A' to 'A', 'S' to 'O', 'D' to 'E', 'F' to 'U', 'G' to 'I',
        'H' to 'D', 'J' to 'H', 'K' to 'T', 'L' to 'N', 'N' to ':',
        'Z' to '?', 'X' to 'Q', 'C' to 'J', 'V' to 'K', 'B' to 'X',
        'M' to 'W', '<' to 'V', '>' to 'Z', '?' to '?'
    )

    /**
     * 현재 레이아웃 타입을 설정합니다.
     * @param layout "c"는 Colemak, "d"는 Dvorak을 나타냅니다.
     */
    fun setLayout(layout: String) {
        layoutType = layout
        Log.d("EnglishConverter", "Layout type set to $layoutType")
    }

    /**
     * 입력된 문자를 현재 설정된 레이아웃에 따라 변환합니다.
     * 대소문자를 구분하여 변환하며, 매핑이 없는 경우 원래 문자를 반환합니다.
     * @param inputChar 변환할 문자
     * @return 변환된 문자 또는 원래 문자
     */
    fun convert(inputChar: Char): Char {
        val convertedChar = when (layoutType) {
            "c" -> qwertyToColemakLowerCaseMap[inputChar] ?: qwertyToColemakUpperCaseMap[inputChar] ?: inputChar
            "d" -> qwertyToDvorakLowerCaseMap[inputChar] ?: qwertyToDvorakUpperCaseMap[inputChar] ?: inputChar
            else -> inputChar
        }

        Log.d("EnglishConverter", "Input character '$inputChar' converted to '$convertedChar' using layout '$layoutType'")
        return convertedChar
    }
}
