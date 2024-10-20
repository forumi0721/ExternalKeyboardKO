package kr.stonecold.exkeyko

/**
 * 입력기 설정을 나타내는 싱글톤 객체
 * 이 클래스는 사용자의 키보드 입력 설정을 관리하며, 각 필드는 기본값으로 초기화되어 있습니다.
 */
object PreferenceDefaults {

    /**
     * 기본 영문 자판 설정
     * Qwerty로 초기화
     */
    var pref_english_layout: String = "q"

    /**
     * 기본 한글 자판 설정
     * 두벌식으로 초기화
     */
    var pref_hangul_layout: String = "2"

    /**
     * 한자 입력 방식 설정
     * 후보뷰로 초기화
     */
    var pref_hanja_select_type: String = "o"

    /**
     * 자동 순서 교정 옵션 (모아치기)
     * 기본값: true
     */
    var pref_hangul_auto_reorder: Boolean = true

    /**
     * 두벌식 자판에서 자음 연타로 된소리로 조합하는 옵션
     * 기본값: true
     */
    var pref_hangul_combi_on_double_stroke: Boolean = true

    /**
     * 두벌식 자판에서 초성에 없는 겹자음을 조합 허용하는 옵션
     * 기본값: true
     */
    var pref_hangul_non_choseong_combi: Boolean = true

    /**
     * 입력 모드 상태를 상태바에 표시하는 옵션
     * 기본값: true
     */
    var pref_input_mode_statusbar_message: Boolean = true

    /**
     * 입력 모드 상태를 Toast 메시지로 표시하는 옵션
     * 기본값: false
     */
    var pref_input_mode_toast_message: Boolean = false

    /**
     * ESC 눌렀을 때 영문 모드로 전환하는 옵션
     * 기본값: true
     */
    var pref_use_esc_english_mode: Boolean = true

    /**
     * 왼쪽 Shift-Space를 눌렀을 때 한영 전환하는 옵션
     * 기본값: true
     */
    var pref_use_left_shift_space: Boolean = true

    /**
     * 오른쪽 Shift-Space를 눌렀을 때 한자 입력 모드로 전환하는 옵션
     * 기본값: true
     */
    var pref_use_right_shift_space: Boolean = true

    /**
     * 오른쪽 Alt(RALT)를 눌렀을 때 한영 전환하는 옵션
     * 기본값: true
     */
    var pref_use_right_alt: Boolean = true

    /**
     * 오른쪽 Ctrl(RCtrl)을 눌렀을 때 한자 입력 모드로 전환하는 옵션
     * 기본값: true
     */
    var pref_use_right_ctrl: Boolean = true

    /**
     * Ctrl+숫자(-=) 조합을 F1-F12 기능키로 사용하는 옵션
     * 기본값: false
     */
    var pref_use_ctrl_number_to_function: Boolean = false

    /**
     * Ctrl+`(Grave) 조합을 ESC로 사용하는 옵션
     * 기본값: false
     */
    var pref_use_ctrl_grave_to_esc: Boolean = false
}
