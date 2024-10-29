package kr.stonecold.exkeyko

import android.util.Log
import android.view.KeyEvent

/**
 * 영문 자판 변환을 위한 Key Mapping 클래스
 */
class KoreanChecker {

    /**
     * 현재 사용 중인 키보드 레이아웃 타입 (2, 2y, 3f, 39, 3s, 3y, 32, ahn, ro)
     */
    private var layoutType = "2" // 기본값은 두벌식 레이아웃

    /**
     * 두벌식 사용 키
     */
    private val hangulKey2 = listOf(
        // Row 1
        'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P',
        'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p',
        // Row 2
        'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
        'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l',
        // Row 3
        'Z', 'X', 'C', 'V', 'B', 'N', 'M',
        'z', 'x', 'c', 'v', 'b', 'n', 'm',
    )

    /**
     * 두벌식 옛글 사용키
     */
    private val hangulKey2y = listOf(
        // Row 1
        'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P',
        'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p',
        // Row 2
        'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
        'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l',
        // Row 3
        'Z', 'X', 'C', 'V', 'B', 'N', 'M',
        'z', 'x', 'c', 'v', 'b', 'n', 'm',
    )

    /**
     * 세벌식 최종
     */
    private val hangulKey3f = listOf(
        // Row 0
        '~', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_',
        '`', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=',
        // Row 1
        'Q', 'W', 'E', 'R', 'T', 'Y', 'U' ,'I', 'O', 'P', '{', '}', '|',
        'q', 'w', 'e', 'r', 't', 'y', 'u' ,'i', 'o', 'p', '[', ']', '\\',
        // Row 2
        'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ':', '"',
        'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'',
        // Row 3
        'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>', '?',
        'z', 'x', 'c', 'v', 'b', 'n', 'm',           '/',
    )

    /**
     * 세벌식 390
     */
    private val hangulKey39 = listOf(
        // Row 0
        '!',
        '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
        // Row 1
        'Q', 'W', 'E', 'R', 'T', 'Y', 'U' ,'I', 'O', 'P',
        'q', 'w', 'e', 'r', 't', 'y', 'u' ,'i', 'o', 'p',
        // Row 2
        'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
        'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'',
        // Row 3
        'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>',
        'z', 'x', 'c', 'v', 'b', 'n', 'm',           '/',
    )

    /**
     * 세벌식 순아래
     */
    private val hangulKey3s = listOf(
        // Row 0
        '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=',
        // Row 1
        'Q', 'W', 'E', 'R', 'T', 'Y', 'U' ,'I', 'O', 'P',
        'q', 'w', 'e', 'r', 't', 'y', 'u' ,'i', 'o', 'p', '[', ']', '\\',
        // Row 2
        'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
        'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', ';',
        // Row 3
        'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>',
        'z', 'x', 'c', 'v', 'b', 'n', 'm',           '/',
    )

    /**
     * 세벌식 옛글
     */
    private val hangulKey3y = listOf(
        // Row 0
        '~', '!', '@',                '^',
        '`', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
        // Row 1
        'Q', 'W', 'E', 'R', 'T', 'Y', 'U' ,'I', 'O', 'P',
        'q', 'w', 'e', 'r', 't', 'y', 'u' ,'i', 'o', 'p',
        // Row 2
        'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
        'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'',
        // Row 3
        'Z', 'X', 'C', 'V', 'B', 'N', 'M', '<', '>',
        'z', 'x', 'c', 'v', 'b', 'n', 'm',           '/',
    )

    /**
     * 세벌식 두벌배열
     */
    private val hangulKey32 = listOf(
        // Row 1
        'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P',
        'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p',
        // Row 2
        'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
        'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l',
        // Row 3
        'Z', 'X', 'C', 'V', 'B', 'N', 'M',
        'z', 'x', 'c', 'v', 'b', 'n', 'm',
    )

    /**
     * 세벌식 두벌배열
     */
    private val hangulKeyAhn = listOf(
        // Row 1
        'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P',
        'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p',
        // Row 2
        'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
        'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l',
        // Row 3
        'Z', 'X', 'C', 'V', 'B', 'N', 'M',
        'z', 'x', 'c', 'v', 'b', 'n', 'm',
    )

    /**
     * 로마자
     */
    private val hangulKeyRo = listOf(
        // Row 1
        'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P',
        'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p',
        // Row 2
        'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
        'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l',
        // Row 3
        'Z', 'X', 'C', 'V', 'B', 'N', 'M',
        'z', 'x', 'c', 'v', 'b', 'n', 'm',
    )

    /**
     * 변환 레이아웃 설정 메서드
     * @param layout 레이아웃 유형 ("2", "2y", "3f", "39", "3s", "3y", "32", "ahn", "ro")
     */
    fun setLayout(layout: String) {
        layoutType = layout
        Log.d("KoreanChecker", "Layout type set to $layoutType")
    }

    /**
     * 설정한 레이아웃에 따라 문자 사용여부를 체크
     * @param inputChar 입력 문자
     * @return 사용 여부
     */
    fun checkKeyCode(inputChar: Char): Boolean {
        val used = when (layoutType) {
            "2" -> hangulKey2.contains(inputChar)
            "2y" -> hangulKey2y.contains(inputChar)
            "3f" -> hangulKey3f.contains(inputChar)
            "39" -> hangulKey39.contains(inputChar)
            "3s" -> hangulKey3s.contains(inputChar)
            "3y" -> hangulKey3y.contains(inputChar)
            "32" -> hangulKey32.contains(inputChar)
            "ahn" -> hangulKeyAhn.contains(inputChar)
            "ro" -> hangulKeyRo.contains(inputChar)
            else -> {
                Log.e("KoreanChecker", "Unrecognized layout type: $layoutType")
                true
            }
        }

        Log.d(
            "KoreanChecker",
            "Input character '$inputChar' using layout '$layoutType': $used"
        )
        return used
    }
}
