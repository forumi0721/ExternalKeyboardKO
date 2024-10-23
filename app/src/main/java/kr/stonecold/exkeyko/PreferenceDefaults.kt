package kr.stonecold.exkeyko

/**
 * 입력기의 기본 설정값을 저장하기 위한 클래스
 * 설정과 입력기의 일치를 위해 별도 클래스로 분리
 */
@Suppress("ConstPropertyName")
object PreferenceDefaults {

    /**
     * Subtype 사용 여부 설정값
     * method.xml과 같이 빌드시 설정해야함
     */
    const val system_use_subtype: Boolean = false

    /**
     * 기본 영문 레이아웃
     */
    const val pref_english_layout: String = "q"

    /**
     * 기본 한글 레이아웃
     */
    const val pref_hangul_layout: String = "2"

    /**
     * 한자 입력 방식 설정
     */
    const val pref_hanja_select_type: String = "h"

    /**
     * 자동 순서 교정 옵션 (모아치기)
     */
    const val pref_hangul_auto_reorder: Boolean = true

    /**
     * 두벌식 자판에서 자음 연타로 된소리로 조합하는 옵션
     */
    const val pref_hangul_combi_on_double_stroke: Boolean = true

    /**
     * 두벌식 자판에서 초성에 없는 겹자음을 조합 허용하는 옵션
     */
    const val pref_hangul_non_choseong_combi: Boolean = true

    /**
     * 입력 모드 상태를 상태바에 표시하는 옵션
     */
    const val pref_input_mode_statusbar_message: Boolean = true

    /**
     * 입력 모드 상태를 Toast 메시지로 표시하는 옵션
     */
    const val pref_input_mode_toast_message: Boolean = false

    /**
     * ESC 눌렀을 때 영문 모드로 전환하는 옵션
     */
    const val pref_use_esc_english_mode: Boolean = true

    /**
     * 왼쪽 Shift-Space를 눌렀을 때 한영 전환하는 옵션
     */
    const val pref_use_left_shift_space: Boolean = true

    /**
     * 오른쪽 Shift-Space를 눌렀을 때 한자 입력 모드로 전환하는 옵션
     */
    const val pref_use_right_shift_space: Boolean = true

    /**
     * 오른쪽 Alt를 눌렀을 때 한영 전환하는 옵션
     */
    const val pref_use_right_alt: Boolean = true

    /**
     * 오른쪽 Ctrl을 눌렀을 때 한자 입력 모드로 전환하는 옵션
     */
    const val pref_use_right_ctrl: Boolean = true

    /**
     * Ctrl+` 조합을 ESC로 사용하는 옵션
     */
    const val pref_use_ctrl_grave_to_esc: Boolean = false

    /**
     * Ctrl+숫자(-=) 조합을 F1-F12 기능키로 사용하는 옵션
     */
    const val pref_use_ctrl_number_to_function: Boolean = false
}
