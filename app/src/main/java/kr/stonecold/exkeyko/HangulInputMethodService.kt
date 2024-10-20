package kr.stonecold.exkeyko

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection
import android.widget.Toast
import androidx.core.content.ContextCompat.*

class HangulInputMethodService : InputMethodService(), SharedPreferences.OnSharedPreferenceChangeListener {

    // 입력 처리 관련 변수
    private lateinit var hangulInputProcessor: HangulInputProcessor
    private val englishConverter = EnglishConverter()
    private var isKoreanMode = false  // 한글/영문 모드

    // 설정 변수
    private lateinit var prefs: SharedPreferences
    private var prefEnglishLayout: String = PreferenceDefaults.pref_english_layout
    private var prefHangulLayout: String = PreferenceDefaults.pref_hangul_layout
    private var prefHanjaSelectType: String = PreferenceDefaults.pref_hanja_select_type
    private var prefHangulAutoReorder: Boolean = PreferenceDefaults.pref_hangul_auto_reorder
    private var prefHangulCombiOnDoubleStroke: Boolean = PreferenceDefaults.pref_hangul_combi_on_double_stroke
    private var prefHangulNonChoseongCombi: Boolean = PreferenceDefaults.pref_hangul_non_choseong_combi
    private var prefInputModeStatusbarMessage: Boolean = PreferenceDefaults.pref_input_mode_statusbar_message
    private var prefInputModeToastMessage: Boolean = PreferenceDefaults.pref_input_mode_toast_message
    private var prefUseEscEnglishMode: Boolean = PreferenceDefaults.pref_use_esc_english_mode
    private var prefUseLeftShiftSpace: Boolean = PreferenceDefaults.pref_use_left_shift_space
    private var prefUseRightShiftSpace: Boolean = PreferenceDefaults.pref_use_right_shift_space
    private var prefUseRightAlt: Boolean = PreferenceDefaults.pref_use_right_alt
    private var prefUseRightCtrl: Boolean = PreferenceDefaults.pref_use_right_ctrl
    private var prefUseCtrlNumberToFunction: Boolean = PreferenceDefaults.pref_use_ctrl_number_to_function
    private var prefUseCtrlGraveToEsc: Boolean = PreferenceDefaults.pref_use_ctrl_grave_to_esc

    private var prefHanjaUseOverlay:Boolean = true

    // 화면 표시 관련 변수
    private var toast: Toast? = null
    private var candidateScrollView: CandidateScrollView? = null
    private var hanjaOverlay: HanjaOverlay? = null

    override fun onCreate() {
        super.onCreate()

        // HangulInputProcessor 초기화
        hangulInputProcessor = HangulInputProcessor(prefHangulLayout)
        hangulInputProcessor.initHanjaTable(this)

        // SharedPreferences 인스턴스 가져오기 및 리스너 등록
        prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.registerOnSharedPreferenceChangeListener(this)

        // SharedPreferences에서 사용자 설정 로드
        loadPreferences()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)

        showLanguageMode(false)
    }

    override fun onCreateInputView(): View? {
        val dummyView = View(this)
        dummyView.visibility = View.GONE
        return dummyView
    }

    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return false
    }

    override fun onCreateCandidatesView(): View {
        candidateScrollView = CandidateScrollView(this)
        return candidateScrollView!!
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        showLanguageMode(false)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        hideStatusIcon()
    }

    /**
     * 키가 눌릴 때 호출되는 메서드
     * @param keyCode Int 눌린 키의 코드
     * @param event KeyEvent 키 이벤트 객체
     * @return Boolean 처리 여부
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val inputConnection = currentInputConnection ?: return false

        // Shift, Alt, Ctrl 키 눌림 상태와 keyCode 및 event 값 로그 출력
        //Log.d("KeyEvent", "keyCode: $keyCode (${KeyEvent.keyCodeToString(keyCode)})")
        //Log.d("KeyEvent", "event: $event")
        //Log.d("KeyEvent", "Shift 상태: ${event.isShiftPressed}")
        //Log.d("KeyEvent", "Ctrl 상태: ${event.isCtrlPressed}")
        //Log.d("KeyEvent", "Alt 상태: ${event.isAltPressed}")
        //Log.d("KeyEvent", "Meta 상태: ${event.isMetaPressed}")
        //Log.d("KeyEvent", "Function 상태: ${event.isFunctionPressed}")
        //Log.d("KeyEvent", "Caps Lock 상태: ${event.isCapsLockOn}")
        //Log.d("KeyEvent", "Num Lock 상태: ${event.isNumLockOn}")
        //Log.d("KeyEvent", "Scroll Lock 상태: ${event.isScrollLockOn}")

        // ESC 키가 눌리면 영문 모드로 전환
        if (keyCode == KeyEvent.KEYCODE_ESCAPE && event.metaState == 0) {
            if (prefUseEscEnglishMode && isKoreanMode()) {
                updateComposingState(inputConnection, true)
                // 현재 모드가 이미 영어라면 서브타입 전환하지 않음
                if (isKoreanMode()) {
                    switchLanguageMode()
                }
            }
            return super.onKeyDown(keyCode, event)  // 기본 키 처리
        }

        // 한영 전환: Shift + Space 또는 Right ALT
        //if ((prefUseRightAlt && event.isAltPressed && keyCode == KeyEvent.KEYCODE_ALT_RIGHT) ||
        //    (prefUseLeftShiftSpace && event.isShiftPressed && (event.metaState and KeyEvent.META_SHIFT_LEFT_ON) != 0 && keyCode == KeyEvent.KEYCODE_SPACE)) {
        if ((prefUseRightAlt && keyCode == KeyEvent.KEYCODE_ALT_RIGHT) ||
            (prefUseLeftShiftSpace && (event.metaState and KeyEvent.META_SHIFT_LEFT_ON) != 0 && keyCode == KeyEvent.KEYCODE_SPACE)) {
            updateComposingState(inputConnection, true)
            switchLanguageMode()
            return true  // 한영 전환 처리 완료
        }

        // Ctrl+~를 ESC로 변환
        //if (prefUseCtrlGraveToEsc && event.isCtrlPressed && keyCode == KeyEvent.KEYCODE_GRAVE) {
        if (prefUseCtrlGraveToEsc && (event.metaState and KeyEvent.META_CTRL_LEFT_ON) != 0 && keyCode == KeyEvent.KEYCODE_GRAVE) {
            val newKeyCode = KeyEvent.KEYCODE_ESCAPE
            val newMetaState = event.metaState and KeyEvent.META_CTRL_ON.inv() and KeyEvent.META_CTRL_LEFT_ON.inv()
            val newEvent = KeyEvent(
                event.downTime,
                event.eventTime,
                event.action,
                newKeyCode,
                event.repeatCount,
                newMetaState
            )
            inputConnection.sendKeyEvent(newEvent)
            return true
        }

        // Ctrl+Number(-=)를 F1-F12로 변환
        //if (prefUseCtrlNumberToFunction && event.isCtrlPressed) {
        if (prefUseCtrlNumberToFunction && (event.metaState and KeyEvent.META_CTRL_LEFT_ON) != 0) {
            val newKeyCode = when (keyCode) {
                KeyEvent.KEYCODE_1 -> KeyEvent.KEYCODE_F1
                KeyEvent.KEYCODE_2 -> KeyEvent.KEYCODE_F2
                KeyEvent.KEYCODE_3 -> KeyEvent.KEYCODE_F3
                KeyEvent.KEYCODE_4 -> KeyEvent.KEYCODE_F4
                KeyEvent.KEYCODE_5 -> KeyEvent.KEYCODE_F5
                KeyEvent.KEYCODE_6 -> KeyEvent.KEYCODE_F6
                KeyEvent.KEYCODE_7 -> KeyEvent.KEYCODE_F7
                KeyEvent.KEYCODE_8 -> KeyEvent.KEYCODE_F8
                KeyEvent.KEYCODE_9 -> KeyEvent.KEYCODE_F9
                KeyEvent.KEYCODE_0 -> KeyEvent.KEYCODE_F10
                KeyEvent.KEYCODE_MINUS -> KeyEvent.KEYCODE_F11
                KeyEvent.KEYCODE_EQUALS -> KeyEvent.KEYCODE_F12
                else -> null
            }
            if (newKeyCode != null) {
                val newMetaState = event.metaState and KeyEvent.META_CTRL_ON.inv() and KeyEvent.META_CTRL_LEFT_ON.inv()
                val newEvent = KeyEvent(
                    event.downTime,
                    event.eventTime,
                    event.action,
                    newKeyCode,
                    event.repeatCount,
                    newMetaState
                )
                inputConnection.sendKeyEvent(newEvent)
                return true
            }
        }

        // 영문 모드 처리
        if (!isKoreanMode()) {
            if (prefEnglishLayout == "q") {
                return super.onKeyDown(keyCode, event)
            } else {
                val asciiChar = event.unicodeChar.toChar()
                if (asciiChar != 0.toChar()) {
                    val convertedChar = englishConverter.convert(asciiChar)
                    inputConnection.commitText(convertedChar.toString(), 1)
                    return true
                }
            }

            return super.onKeyDown(keyCode, event)
        }

        // Ctrl, Alt, Fn 등의 특수 키가 눌렸을 경우 ASCII로 처리
        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
            val asciiChar = event.unicodeChar.toChar()
            if (asciiChar != 0.toChar()) {
                updateComposingState(inputConnection, true)
                return super.onKeyDown(keyCode, event)
            }
        }

        //// 방향키 입력시 강제 커밋
        //if (keyCode in arrayOf(
        //        KeyEvent.KEYCODE_DPAD_LEFT,
        //        KeyEvent.KEYCODE_DPAD_RIGHT,
        //        KeyEvent.KEYCODE_DPAD_UP,
        //        KeyEvent.KEYCODE_DPAD_DOWN
        //    )
        //) {
        //    if (!hangulInputProcessor.isEmpty()) {
        //        updateComposingState(inputConnection, true)
        //        return true
        //    }
        //    return super.onKeyDown(keyCode, event)
        //}

        // 한자 입력 처리
        if ((prefUseRightCtrl && (event.metaState and KeyEvent.META_CTRL_ON) !=0 && keyCode == KeyEvent.KEYCODE_CTRL_RIGHT) ||
            (prefUseRightShiftSpace && (event.metaState and KeyEvent.META_SHIFT_RIGHT_ON) != 0 && keyCode == KeyEvent.KEYCODE_SPACE)) {
            handleHanjaInput(inputConnection)
            return true
        }

        // 한글 처리
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            val processed = hangulInputProcessor.backspace()
            if (processed) {
                updateComposingState(inputConnection, false)
                if (hangulInputProcessor.isEmpty()) {
                    hangulInputProcessor.reset()
                    return super.onKeyDown(keyCode, event)
                }
                return true
            }
        } else {
            val asciiCode = event.unicodeChar
            if (asciiCode != 0) {
                val processed = hangulInputProcessor.process(asciiCode)
                updateComposingState(inputConnection, !processed)
                if (!processed) {
                    return super.onKeyDown(keyCode, event)
                }
                return true
            } else {
                if ((event.metaState and KeyEvent.META_SHIFT_ON) == 0) {
                    updateComposingState(inputConnection, true)
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    /**
     * 키 업 이벤트 처리 메서드
     * @param keyCode Int 키 코드
     * @param event KeyEvent? 키 이벤트 객체
     * @return Boolean 처리 여부
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val inputConnection = currentInputConnection ?: return false

        // 방향키 입력시 강제 커밋
        if (keyCode in arrayOf(
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN
            )
        ) {
            if (!hangulInputProcessor.isEmpty()) {
                updateComposingState(inputConnection, true)
            }
            return super.onKeyUp(keyCode, event)
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)

        if (oldSelStart != newSelStart || oldSelEnd != newSelEnd) {
            dismissCandidates()
        }
    }

    override fun onFinishInput() {
        super.onFinishInput()

        dismissCandidates()

        val commitString = hangulInputProcessor.flush()
        if (!commitString.isNullOrEmpty()) {
            currentInputConnection.commitText(commitString, 1)
        }
        hangulInputProcessor.reset()
        currentInputConnection.finishComposingText()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)  // 리스너 해제
        hangulInputProcessor.close()  // 리소스 해제
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // 설정이 변경되었을 때 해당 설정을 다시 로드
        loadPreferences()
    }

    /**
     * 설정값 로드하는 메서드
     */
    private fun loadPreferences() {
        prefEnglishLayout = prefs.getString("pref_english_layout", PreferenceDefaults.pref_english_layout)
            ?: PreferenceDefaults.pref_english_layout  // 기본 영문 자판 설정
        prefHangulLayout = prefs.getString("pref_hangul_layout", PreferenceDefaults.pref_hangul_layout)
            ?: PreferenceDefaults.pref_hangul_layout  // 기본 한글 자판 설정
        prefHanjaSelectType = prefs.getString("pref_hanja_select_type", PreferenceDefaults.pref_hanja_select_type)
            ?: PreferenceDefaults.pref_hanja_select_type// 한자 선택 방식
        prefHangulAutoReorder = prefs.getBoolean("pref_hangul_auto_reorder", PreferenceDefaults.pref_hangul_auto_reorder)  // 자동 순서 교정 옵션 (모아치기)
        prefHangulCombiOnDoubleStroke = prefs.getBoolean("pref_hangul_combi_on_double_stroke", PreferenceDefaults.pref_hangul_combi_on_double_stroke)  // 자음 연타 조합 옵션
        prefHangulNonChoseongCombi = prefs.getBoolean("pref_hangul_non_choseong_combi", PreferenceDefaults.pref_hangul_non_choseong_combi)  // 겹자음 조합 옵션
        prefInputModeStatusbarMessage = prefs.getBoolean("pref_input_mode_statusbar_message", PreferenceDefaults.pref_input_mode_statusbar_message)  // 한/영 상태
        prefInputModeToastMessage = prefs.getBoolean("pref_input_mode_toast_message", PreferenceDefaults.pref_input_mode_toast_message)  // 한/영 상태
        prefUseEscEnglishMode = prefs.getBoolean("pref_use_esc_english_mode", PreferenceDefaults.pref_use_esc_english_mode)  // ESC 눌렀을때 영문으로
        prefUseLeftShiftSpace = prefs.getBoolean("pref_use_left_shift_space", PreferenceDefaults.pref_use_left_shift_space)  // Left Shift-Space 눌렀을때 한/영 전환
        prefUseRightShiftSpace = prefs.getBoolean("pref_use_right_shift_space", PreferenceDefaults.pref_use_right_shift_space)  // Right Shift-Space 눌렀을때 한자입력
        prefUseRightAlt = prefs.getBoolean("pref_use_right_alt", PreferenceDefaults.pref_use_right_alt)  // RALT 눌렀을때 한/영 전환
        prefUseRightCtrl = prefs.getBoolean("pref_use_right_ctrl", PreferenceDefaults.pref_use_right_ctrl)  // RCtrl 눌렀을때 한자입력
        prefUseCtrlNumberToFunction = prefs.getBoolean("pref_use_ctrl_number_to_function", PreferenceDefaults.pref_use_ctrl_number_to_function)  // Ctrl+Number(-=)를 F1-F12로
        prefUseCtrlGraveToEsc = prefs.getBoolean("pref_use_ctrl_grave_to_esc", PreferenceDefaults.pref_use_ctrl_grave_to_esc)  // Ctrl+~를 ESC로

        // Set options in HangulInputProcessor
        hangulInputProcessor.selectKeyboard(prefHangulLayout)
        hangulInputProcessor.setOption(0, prefHangulAutoReorder)
        hangulInputProcessor.setOption(1, prefHangulCombiOnDoubleStroke)
        hangulInputProcessor.setOption(2, prefHangulNonChoseongCombi)

        // Set options in EnglishConverter
        englishConverter.setLayout(prefEnglishLayout)

        // 한자입력
        dismissCandidates()
        prefHanjaUseOverlay = if (prefHanjaSelectType == "o") {
            Settings.canDrawOverlays(this)
        } else {
            false
        }

        // Statusbar 초기화
        if (!prefInputModeStatusbarMessage) {
            hideStatusIcon()
        }

        // 알림 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                prefInputModeToastMessage = false
            }
        }
    }

    /**
     * 현재 모드가 한국어인지 확인하는 메서드
     * @return Boolean 현재 한국어 모드 여부
     */
    private fun isKoreanMode(): Boolean {
        // Subtype 쓸 경우
        //val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        //val currentSubtype: InputMethodSubtype? = imm.currentInputMethodSubtype
        //val extraValue = currentSubtype?.languageTag ?: "en"
        //return extraValue == "ko"

        //Subtype 안 쓸 경우
        return isKoreanMode
    }

    /**
     * 한글/영문 모드를 전환하는 메서드
     */
    private fun switchLanguageMode() {
        //Subtype 쓸 경우
        //dismissCandidates()
        //val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        //val currentInputMethodId = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)

        //val currentSubtype = imm.currentInputMethodSubtype
        //val subtypes = imm.getEnabledInputMethodSubtypeList(null, true) // true 대신 false 사용
        //val nextSubtype = subtypes.firstOrNull { it != currentSubtype }
        //switchInputMethod(currentInputMethodId , nextSubtype)

        //Subtype 안 쓸 경우
        dismissCandidates()
        isKoreanMode = !isKoreanMode()
        showLanguageMode()
    }

    /**
     * 한글/영문 모드를 상태바에 표시하거나 Toast로 알리는 메서드
     * @param toastMessage Boolean Toast 메시지 표시 여부
     */
    private fun showLanguageMode(toastMessage: Boolean = true) {
        if (prefInputModeStatusbarMessage) {
            val icon = if (isKoreanMode()) R.drawable.ic_notificationbar_ko else R.drawable.ic_notificationbar_en
            showStatusIcon(icon)
        }
        if (toastMessage && prefInputModeToastMessage) {
            val message = if (isKoreanMode()) R.string.korean else R.string.english
            Handler(Looper.getMainLooper()).post {
                toast?.cancel()
                toast = Toast.makeText(this@HangulInputMethodService.applicationContext, message, Toast.LENGTH_SHORT)
                toast?.show()
            }
        }
    }

    /**
     * 입력 중인 한글 상태를 업데이트하는 메서드
     * @param inputConnection InputConnection 입력 연결 객체
     * @param forceCommit Boolean 강제 커밋 여부
     */
    private fun updateComposingState(inputConnection: InputConnection, forceCommit: Boolean) {
        if (forceCommit) {
            val flushVal = hangulInputProcessor.flush()
            if (!flushVal.isNullOrEmpty()) {
                inputConnection.commitText(flushVal, 1)
            } else {
                val commitText = hangulInputProcessor.getCommitString()
                if (!commitText.isNullOrEmpty()) {
                    inputConnection.commitText(commitText, 1)
                } else {
                    val preeditText = hangulInputProcessor.getPreeditString()
                    if (!preeditText.isNullOrEmpty()) {
                        inputConnection.commitText(preeditText, 1)
                    }
                }
            }
            inputConnection.finishComposingText()
            hangulInputProcessor.reset()
        } else {
            val commitText = hangulInputProcessor.getCommitString()
            if (!commitText.isNullOrEmpty()) {
                inputConnection.commitText(commitText, 1)
            }

            // Preedit string을 확인하고 조합 중인 글자가 있으면 화면에 표시
            val preeditText = hangulInputProcessor.getPreeditString()
            if (!preeditText.isNullOrEmpty()) {
                inputConnection.setComposingText(preeditText, 1)
            }
        }
    }

    /**
     * 한자 입력을 처리하는 메서드
     * @param inputConnection InputConnection 입력 연결 객체
     */
    private fun handleHanjaInput(inputConnection: InputConnection) {
        var removeCursor = false
        var hanjaMap: LinkedHashMap<String, String>? = null

        // 입력 중인 글자
        val preeditText = hangulInputProcessor.getPreeditString()
        if (!preeditText.isNullOrEmpty()) {
            hanjaMap = hangulInputProcessor.matchExactHanjaMap(preeditText.toString())
            removeCursor = true
        } else {
            // 선택한 텍스트
            val selectedText = inputConnection.getSelectedText(0)
            if (!selectedText.isNullOrEmpty()) {
                hanjaMap = hangulInputProcessor.matchExactHanjaMap(selectedText.toString())
                removeCursor = false
            } else {
                //현재 커서
                val cursorText = inputConnection.getTextBeforeCursor(1, 0)
                if (!cursorText.isNullOrEmpty()) {
                    hanjaMap = hangulInputProcessor.matchExactHanjaMap(cursorText.toString())
                    removeCursor = true
                }
            }
        }

        if (!hanjaMap.isNullOrEmpty()) {
            showCandidates(hanjaMap, removeCursor)
            return
        }
    }

    /**
     * 후보자 뷰를 표시하는 메서드 (한자 변환 처리)
     * @param hanjaMap HashMap<String, String>? 한자 후보 목록
     * @param removeCursor Boolean 커서를 제거할지 여부
     */
    private fun showCandidates(hanjaMap: HashMap<String, String>?, removeCursor: Boolean) {
        if (prefHanjaUseOverlay) {
            populateHanjaOverlay(hanjaMap, removeCursor)
        } else {
            if (!hanjaMap.isNullOrEmpty()) {
                setCandidatesViewShown(true)
                populateCandidates(hanjaMap, removeCursor)
            } else {
                setCandidatesViewShown(false)
            }
        }
    }

    /**
     * 오버레이 뷰에 한자 후보 목록을 추가하는 메서드
     * @param hanjaMap HashMap<String, String>? 한자 후보 목록
     * @param removeCursor Boolean 커서를 제거할지 여부
     */
    private fun populateHanjaOverlay(hanjaMap: HashMap<String, String>?, removeCursor: Boolean) {
        if (hanjaOverlay != null) {
            hanjaOverlay?.dismissOverlay()
        }
        hanjaOverlay = HanjaOverlay(this)
        hanjaOverlay?.showHanjaOverlay(hanjaMap!!)

        hanjaOverlay?.onCandidateSelectedListener = { selectedHanja ->
            Log.d("MainActivity", "Selected Hanja: $selectedHanja")

            val inputConnection = currentInputConnection
            inputConnection.finishComposingText()
            hangulInputProcessor.reset()
            if (removeCursor) {
                val extractedText = inputConnection.getExtractedText(ExtractedTextRequest(), 0)
                val currentCursorPosition = extractedText?.selectionStart ?: 0
                inputConnection.setSelection(currentCursorPosition - 1, currentCursorPosition)
            }
            inputConnection.commitText(selectedHanja, 1)
        }
    }

    /**
     * 후보자 뷰에 한자 후보 목록을 추가하는 메서드
     * @param hanjaMap HashMap<String, String>? 한자 후보 목록
     * @param removeCursor Boolean 커서를 제거할지 여부
     */
    private fun populateCandidates(hanjaMap: HashMap<String, String>?, removeCursor: Boolean) {
        candidateScrollView?.onCandidateSelectedListener = { selectedKey ->
            Log.d("MainActivity", "Selected candidate key: $selectedKey")
            // 원하는 동작 수행
            setCandidatesViewShown(false)

            val inputConnection = currentInputConnection
            inputConnection.finishComposingText()
            hangulInputProcessor.reset()
            if (removeCursor) {
                val extractedText = inputConnection.getExtractedText(ExtractedTextRequest(), 0)
                val currentCursorPosition = extractedText?.selectionStart ?: 0
                inputConnection.setSelection(currentCursorPosition - 1, currentCursorPosition)
            }
            inputConnection.commitText(selectedKey, 1)
        }
        candidateScrollView?.addCards(hanjaMap)
        Log.d("HangulInputMethodService", "populateCandidates completed with ${hanjaMap?.size} items")
    }

    /**
     * 후보자 뷰를 없애는 메서드
     */
    private fun dismissCandidates() {
        if (prefHanjaUseOverlay) {
            hanjaOverlay?.dismissOverlay()
        } else {
            setCandidatesViewShown(false)
        }
    }
}
