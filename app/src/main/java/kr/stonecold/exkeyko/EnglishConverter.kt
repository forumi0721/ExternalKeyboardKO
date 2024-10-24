package kr.stonecold.exkeyko

import android.util.Log

/**
 * 영문 자판 변환을 위한 Key Mapping 클래스
 */
class EnglishConverter {

    /**
     * 현재 사용 중인 키보드 레이아웃 타입 (q, c, d, w)
     */
    private var layoutType = "q" // 기본값은 QWERTY 레이아웃

    /**
     * QWERTY에서 Dvorak으로의 소문자 키 매핑
     */
    private val qwertyToDvorakLowerCaseMap = mapOf(
        // Row 1
        'q' to '\'', 'w' to ',', 'e' to '.', 'r' to 'p', 't' to 'y',
        'y' to 'f', 'u' to 'g', 'i' to 'c', 'o' to 'r', 'p' to 'l',
        '[' to '/', ']' to '=', '-' to '[', '=' to ']',
        // Row 2
        'a' to 'a', 's' to 'o', 'd' to 'e', 'f' to 'u', 'g' to 'i',
        'h' to 'd', 'j' to 'h', 'k' to 't', 'l' to 'n', ';' to 's',
        '\'' to '-',
        // Row 3
        'z' to ';', 'x' to 'q', 'c' to 'j', 'v' to 'k', 'b' to 'x',
        'n' to 'b', 'm' to 'm', ',' to 'w', '.' to 'v', '/' to 'z'
    )

    /**
     * QWERTY에서 Dvorak으로의 대문자 키 매핑
     */
    private val qwertyToDvorakUpperCaseMap = mapOf(
        // Row 1
        'Q' to '"', 'W' to '<', 'E' to '>', 'R' to 'P', 'T' to 'Y',
        'Y' to 'F', 'U' to 'G', 'I' to 'C', 'O' to 'R', 'P' to 'L',
        '{' to '?', '}' to '+', '_' to '{', '+' to '}',
        // Row 2
        'A' to 'A', 'S' to 'O', 'D' to 'E', 'F' to 'U', 'G' to 'I',
        'H' to 'D', 'J' to 'H', 'K' to 'T', 'L' to 'N', ':' to 'S',
         '"' to '_',
        // Row 3
        'Z' to ':', 'X' to 'Q', 'C' to 'J', 'V' to 'K', 'B' to 'X',
        'N' to 'B', 'M' to 'M', '<' to 'W', '>' to 'V', '?' to 'Z'
    )

    /**
     * QWERTY에서 Colemak으로의 소문자 키 매핑
     */
    private val qwertyToColemakLowerCaseMap = mapOf(
        // Row 1
        'q' to 'q', 'w' to 'w', 'e' to 'f', 'r' to 'p', 't' to 'g',
        'y' to 'j', 'u' to 'l', 'i' to 'u', 'o' to 'y', 'p' to ';',
        // Row 2
        'a' to 'a', 's' to 'r', 'd' to 's', 'f' to 't', 'g' to 'd',
        'h' to 'h', 'j' to 'n', 'k' to 'e', 'l' to 'i', ';' to 'o',
        // Row 3
        'z' to 'z', 'x' to 'x', 'c' to 'c', 'v' to 'v', 'b' to 'b',
        'n' to 'k', 'm' to 'm'
    )

    /**
     * QWERTY에서 Colemak으로의 대문자 키 매핑
     */
    private val qwertyToColemakUpperCaseMap = mapOf(
        // Row 1
        'Q' to 'Q', 'W' to 'W', 'E' to 'F', 'R' to 'P', 'T' to 'G',
        'Y' to 'J', 'U' to 'L', 'I' to 'U', 'O' to 'Y', 'P' to ':',
        // Row 2
        'A' to 'A', 'S' to 'R', 'D' to 'S', 'F' to 'T', 'G' to 'D',
        'H' to 'H', 'J' to 'N', 'K' to 'E', 'L' to 'I', ':' to 'O',
        // Row 3
        'Z' to 'Z', 'X' to 'X', 'C' to 'C', 'V' to 'V', 'B' to 'B',
        'N' to 'K', 'M' to 'M'
    )

    /**
     * QWERTY에서 Workman으로의 소문자 키 매핑
     */
    private val qwertyToWorkmanLowerCaseMap = mapOf(
        // Row 1
        'q' to 'q', 'w' to 'd', 'e' to 'r', 'r' to 'w', 't' to 'b',
        'y' to 'j', 'u' to 'f', 'i' to 'u', 'o' to 'p', 'p' to ';',
        // Row 2
        'a' to 'a', 's' to 's', 'd' to 'h', 'f' to 't', 'g' to 'g',
        'h' to 'y', 'j' to 'n', 'k' to 'e', 'l' to 'o', ';' to 'i',
        // Row 3
        'z' to 'z', 'x' to 'x', 'c' to 'm', 'v' to 'c', 'b' to 'v',
        'n' to 'k', 'm' to 'l'
    )

    /**
     * QWERTY에서 Workman으로의 대문자 키 매핑
     */
    private val qwertyToWorkmanUpperCaseMap = mapOf(
        // Row 1
        'Q' to 'Q', 'W' to 'D', 'E' to 'R', 'R' to 'W', 'T' to 'B',
        'Y' to 'J', 'U' to 'F', 'I' to 'U', 'O' to 'P', 'P' to ':',
        // Row 2
        'A' to 'A', 'S' to 'S', 'D' to 'H', 'F' to 'T', 'G' to 'G',
        'H' to 'Y', 'J' to 'N', 'K' to 'E', 'L' to 'O', ':' to 'I',
        // Row 3
        'Z' to 'Z', 'X' to 'X', 'C' to 'M', 'V' to 'C', 'B' to 'V',
        'N' to 'K', 'M' to 'L'
    )

    /**
     * 변환 레이아웃 설정 메서드
     * @param layout 레이아웃 유형 (q: QWERTY, c: Colemak, d: Dvorak)
     */
    fun setLayout(layout: String) {
        layoutType = layout
        Log.d("EnglishConverter", "Layout type set to $layoutType")
    }

    /**
     * 설정한 레이아웃에 따라 문자를 변환하는 클래스
     * @param inputChar 입력 문자
     * @return 출력 문자
     */
    fun convert(inputChar: Char): Char {
        val convertedChar = when (layoutType) {
            "c" -> qwertyToColemakLowerCaseMap[inputChar]
                ?: qwertyToColemakUpperCaseMap[inputChar]
                ?: inputChar
            "d" -> qwertyToDvorakLowerCaseMap[inputChar]
                ?: qwertyToDvorakUpperCaseMap[inputChar]
                ?: inputChar
            "w" -> qwertyToWorkmanLowerCaseMap[inputChar]
                ?: qwertyToWorkmanUpperCaseMap[inputChar]
                ?: inputChar
            else -> inputChar
        }

        Log.d(
            "EnglishConverter",
            "Input character '$inputChar' converted to '$convertedChar' using layout '$layoutType'"
        )
        return convertedChar
    }
}
