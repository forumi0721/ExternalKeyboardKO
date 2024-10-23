package kr.stonecold.exkeyko

import android.Manifest
import android.content.ComponentName
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
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.Toast

/**
 * 한글 입력 메서드 서비스 클래스입니다.
 * 키 입력 처리 및 한/영 전환, 한자 변환 등을 담당합니다.
 */
class HangulInputMethodService : InputMethodService(), SharedPreferences.OnSharedPreferenceChangeListener {

    // 입력 처리 관련 변수
    private lateinit var hangulInputProcessor: HangulInputProcessor
    private val englishConverter = EnglishConverter()
    private var currentImputMode:InputMode = InputMode.ENGLISH
    private enum class InputMode { ENGLISH, KOREAN, }

    // Subtype
    private var switchLanguageMode: () -> Unit
        get() = if (PreferenceDefaults.system_use_subtype) {
            ::switchLanguageModeSubtype
        } else {
            ::switchLanguageModeToggle
        }
        set(_) { }

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

    private var prefHanjaUseOverlay: Boolean = true

    // 화면 표시 관련 변수
    private var toast: Toast? = null
    private val candidateScrollView: CandidateScrollView by lazy {
        CandidateScrollView(this)
    }
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

    /**
     * 입력 시작시 Statusbar 표시를 위해 onStartInputㄹ들 override 합니다.
     * @param attribute EditorInfo? 기본 파라디터입니다.
     * @param restarting Boolean 기본 파라디터입니다.
     */
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
         super.onStartInput(attribute, restarting)
        showLanguageMode(false)
    }

    /**
     * 키보드를 보이지 않기 위해 onCreateInputView를 override 합니다.
     * @return View Dummy view 입니다.
     */
    override fun onCreateInputView(): View {
        val dummyView = View(this)
        dummyView.visibility = View.GONE
        return dummyView
    }

    /**
     * 키보드를 보이지 않기 위해 onEvaluateFullscreenMode를 override 합니다.
     * @return Boolean 숨기기 처리합니다.
     */
    override fun onEvaluateFullscreenMode(): Boolean {
        return false
    }

    /**
     * 키보드를 보이지 않기 위해 onEvaluateInputViewShown를 override 합니다.
     * @return Boolean 숨기기 처리합니다.
     */
    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return false
    }

    /**
     * Custom 후보키 View를 사용하기 위해 onCreateCandidatesView를 override 합니다.
     * @return View 후보키 View 입니다.
     */
    override fun onCreateCandidatesView(): View {
        return candidateScrollView
    }

    /**
     * 키가 눌릴 때 호출되는 메서드입니다.
     * @param keyCode Int 눌린 키의 코드입니다.
     * @param event KeyEvent 키 이벤트 객체입니다.
     * @return Boolean 처리 여부를 반환합니다.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val inputConnection = currentInputConnection ?: return false

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
                switchLanguageMode()
            }
            return super.onKeyDown(keyCode, event)
        }

        // 한영 전환: Right ALT 또는 Left Shift + Space
        if ((prefUseRightAlt && keyCode == KeyEvent.KEYCODE_ALT_RIGHT) ||
            (prefUseLeftShiftSpace && (event.metaState and KeyEvent.META_SHIFT_LEFT_ON) != 0 && keyCode == KeyEvent.KEYCODE_SPACE)
        ) {
            updateComposingState(inputConnection, true)
            switchLanguageMode()
            return true  // 한영 전환 처리 완료
        }

        // Ctrl+~를 ESC로 변환
        if (prefUseCtrlGraveToEsc && event.isCtrlPressed && keyCode == KeyEvent.KEYCODE_GRAVE) {
            val newKeyCode = KeyEvent.KEYCODE_ESCAPE
            val newMetaState = event.metaState and KeyEvent.META_CTRL_MASK.inv()
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
        if (prefUseCtrlNumberToFunction && event.isCtrlPressed) {
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
                val newMetaState = event.metaState and KeyEvent.META_CTRL_MASK.inv()
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

        // 특수 키 처리
        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
            val asciiChar = event.unicodeChar.toChar()
            if (asciiChar != 0.toChar()) {
                updateComposingState(inputConnection, true)
                return super.onKeyDown(keyCode, event)
            }
        }

        // 한자 입력 처리
        if ((prefUseRightCtrl && keyCode == KeyEvent.KEYCODE_CTRL_RIGHT) ||
            (prefUseRightShiftSpace && (event.metaState and KeyEvent.META_SHIFT_RIGHT_ON) != 0 && keyCode == KeyEvent.KEYCODE_SPACE)
        ) {
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
                if (!event.isShiftPressed) {
                    updateComposingState(inputConnection, true)
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    /**
     * 키 업 이벤트 처리 메서드입니다.
     * @param keyCode Int 키 코드입니다.
     * @param event KeyEvent? 키 이벤트 객체입니다.
     * @return Boolean 처리 여부를 반환합니다.
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val inputConnection = currentInputConnection ?: return false

        // 방향키 입력 시 강제 커밋
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
            keyCode == KeyEvent.KEYCODE_DPAD_RIGHT ||
            keyCode == KeyEvent.KEYCODE_DPAD_UP ||
            keyCode == KeyEvent.KEYCODE_DPAD_DOWN
        ) {
            if (!hangulInputProcessor.isEmpty()) {
                updateComposingState(inputConnection, true)
            }
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

    /**
     * 설정이 변경되었을 때 호출되는 메서드입니다.
     * @param sharedPreferences SharedPreferences? 변경된 설정입니다.
     * @param key String? 변경된 키입니다.
     */
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        loadPreferences()
    }

    /**
     * 설정값을 로드하는 메서드입니다.
     */
    private fun loadPreferences() {
        prefEnglishLayout = prefs.getString("pref_english_layout", PreferenceDefaults.pref_english_layout)
            ?: PreferenceDefaults.pref_english_layout
        prefHangulLayout = prefs.getString("pref_hangul_layout", PreferenceDefaults.pref_hangul_layout)
            ?: PreferenceDefaults.pref_hangul_layout
        prefHanjaSelectType = prefs.getString("pref_hanja_select_type", PreferenceDefaults.pref_hanja_select_type)
            ?: PreferenceDefaults.pref_hanja_select_type
        prefHangulAutoReorder = prefs.getBoolean("pref_hangul_auto_reorder", PreferenceDefaults.pref_hangul_auto_reorder)
        prefHangulCombiOnDoubleStroke = prefs.getBoolean("pref_hangul_combi_on_double_stroke", PreferenceDefaults.pref_hangul_combi_on_double_stroke)
        prefHangulNonChoseongCombi = prefs.getBoolean("pref_hangul_non_choseong_combi", PreferenceDefaults.pref_hangul_non_choseong_combi)
        prefInputModeStatusbarMessage = prefs.getBoolean("pref_input_mode_statusbar_message", PreferenceDefaults.pref_input_mode_statusbar_message)
        prefInputModeToastMessage = prefs.getBoolean("pref_input_mode_toast_message", PreferenceDefaults.pref_input_mode_toast_message)
        prefUseEscEnglishMode = prefs.getBoolean("pref_use_esc_english_mode", PreferenceDefaults.pref_use_esc_english_mode)
        prefUseLeftShiftSpace = prefs.getBoolean("pref_use_left_shift_space", PreferenceDefaults.pref_use_left_shift_space)
        prefUseRightShiftSpace = prefs.getBoolean("pref_use_right_shift_space", PreferenceDefaults.pref_use_right_shift_space)
        prefUseRightAlt = prefs.getBoolean("pref_use_right_alt", PreferenceDefaults.pref_use_right_alt)
        prefUseRightCtrl = prefs.getBoolean("pref_use_right_ctrl", PreferenceDefaults.pref_use_right_ctrl)
        prefUseCtrlNumberToFunction = prefs.getBoolean("pref_use_ctrl_number_to_function", PreferenceDefaults.pref_use_ctrl_number_to_function)
        prefUseCtrlGraveToEsc = prefs.getBoolean("pref_use_ctrl_grave_to_esc", PreferenceDefaults.pref_use_ctrl_grave_to_esc)

        // HangulInputProcessor 설정 적용
        hangulInputProcessor.selectKeyboard(prefHangulLayout)
        hangulInputProcessor.setOption(0, prefHangulAutoReorder)
        hangulInputProcessor.setOption(1, prefHangulCombiOnDoubleStroke)
        hangulInputProcessor.setOption(2, prefHangulNonChoseongCombi)

        // EnglishConverter 설정 적용
        englishConverter.setLayout(prefEnglishLayout)

        // 한자 입력 설정
        dismissCandidates(true)
        prefHanjaUseOverlay = (prefHanjaSelectType == "v" || prefHanjaSelectType == "h") && Settings.canDrawOverlays(this)
        prefHanjaSelectType = if (!prefHanjaUseOverlay) "c" else prefHanjaSelectType

        // StatusBar 초기화
        if (!prefInputModeStatusbarMessage) {
            hideStatusIcon()
        }

        // 알림 권한 체크
        if (prefInputModeToastMessage) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                prefInputModeToastMessage = false
            }
        }
    }

    /**
     * 현재 모드가 한국어인지 확인하는 메서드입니다.
     * @return Boolean 현재 한국어 모드 여부를 반환합니다.
     */
    private fun isKoreanMode(): Boolean {
        return currentImputMode == InputMode.KOREAN
    }

    /**
     * 한글/영문 모드를 전환하는 메서드입니다.
     */
    private fun switchLanguageModeToggle() {
        dismissCandidates()
        currentImputMode = if (currentImputMode == InputMode.ENGLISH) InputMode.KOREAN else InputMode.ENGLISH
        showLanguageMode()
        Log.d("HangulInputMethodService", "Language mode switched to ${if (currentImputMode == InputMode.KOREAN) "Korean" else "English"}")
    }

    /**
     * 한글/영문 모드를 전환하는 메서드입니다.
     */
    private fun switchLanguageModeSubtype() {
        dismissCandidates()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentInputMethodId = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val currentSubtype = imm.currentInputMethodSubtype
        val subtypes = imm.getEnabledInputMethodSubtypeList(null, true) // true 대신 false 사용
        val nextSubtype = subtypes.firstOrNull { it != currentSubtype }
        switchInputMethod(currentInputMethodId , nextSubtype)
        Log.d("HangulInputMethodService", "Language mode switched to ${if (currentImputMode == InputMode.KOREAN) "Korean" else "English"}")
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype?) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        currentImputMode = if (newSubtype?.languageTag == "ko-KR") InputMode.KOREAN else InputMode.ENGLISH
        showLanguageMode()
    }

    /**
     * 한글/영문 모드를 상태바에 표시하거나 Toast로 알리는 메서드입니다.
     * @param toastMessage Boolean Toast 메시지 표시 여부입니다.
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
                toast = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
                toast?.show()
            }
        }
    }

    /**
     * 입력 중인 한글 상태를 업데이트하는 메서드입니다.
     * @param inputConnection InputConnection 입력 연결 객체입니다.
     * @param forceCommit Boolean 강제 커밋 여부입니다.
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

            // 조합 중인 글자가 있으면 화면에 표시
            val preeditText = hangulInputProcessor.getPreeditString()
            if (!preeditText.isNullOrEmpty()) {
                inputConnection.setComposingText(preeditText, 1)
            }
        }
    }

    /**
     * 한자 입력을 처리하는 메서드입니다.
     * @param inputConnection InputConnection 입력 연결 객체입니다.
     */
    private fun handleHanjaInput(inputConnection: InputConnection) {
        var hanjaText = ""
        var removeCursor = false

        // 입력 중인 글자 확인
        val preeditText = hangulInputProcessor.getPreeditString()
        if (!preeditText.isNullOrEmpty()) {
            hanjaText = preeditText
            removeCursor = true
        } else {
            // 선택한 텍스트 확인
            val selectedText = inputConnection.getSelectedText(0)
            if (!selectedText.isNullOrEmpty()) {
                hanjaText = selectedText.toString()
                removeCursor = false
            } else {
                // 커서 앞의 문자 확인
                val cursorText = inputConnection.getTextBeforeCursor(1, 0)
                if (!cursorText.isNullOrEmpty()) {
                    hanjaText = cursorText.toString()
                    removeCursor = true
                }
            }
        }

        if (hanjaText.isNotEmpty()) {
            val hanjaMap = hangulInputProcessor.matchExactHanjaMap(hanjaText)
            if (!hanjaMap.isNullOrEmpty()) {
                showCandidates(hanjaText, hanjaMap, removeCursor)
            }
        }
    }

    /**
     * 후보자 뷰를 표시하는 메서드입니다. (한자 변환 처리)
     * @param hanjaText String 원본 문자입니다.
     * @param hanjaMap HashMap<String, String> 한자 후보 목록입니다.
     * @param removeCursor Boolean 커서를 제거할지 여부입니다.
     */
    private fun showCandidates(hanjaText: String, hanjaMap: HashMap<String, String>, removeCursor: Boolean) {
        if (prefHanjaUseOverlay) {
            populateHanjaOverlay(hanjaText, hanjaMap, removeCursor)
        } else {
            setCandidatesViewShown(false)
            setCandidatesViewShown(true)
            populateCandidates(hanjaText, hanjaMap, removeCursor)
        }
    }

    /**
     * 오버레이 뷰에 한자 후보 목록을 추가하는 메서드입니다.
     * @param hanjaText String 원본 문자입니다.
     * @param hanjaMap HashMap<String, String> 한자 후보 목록입니다.
     * @param removeCursor Boolean 커서를 제거할지 여부입니다.
     */
    private fun populateHanjaOverlay(hanjaText: String, hanjaMap: HashMap<String, String>, removeCursor: Boolean) {
        hanjaOverlay?.dismissOverlay()
        hanjaOverlay = HanjaOverlay(this)
        hanjaOverlay?.showHanjaOverlay(hanjaText, hanjaMap, prefHanjaSelectType)

        hanjaOverlay?.onCandidateSelectedListener = { selectedHanja ->
            Log.d("HangulInputMethodService", "Selected Hanja: $selectedHanja")

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
     * 후보자 뷰에 한자 후보 목록을 추가하는 메서드입니다.
     * @param hanjaText String 원본 문자입니다.
     * @param hanjaMap HashMap<String, String> 한자 후보 목록입니다.
     * @param removeCursor Boolean 커서를 제거할지 여부입니다.
     */
    private fun populateCandidates(hanjaText: String, hanjaMap: HashMap<String, String>, removeCursor: Boolean) {
        candidateScrollView.onCandidateSelectedListener = { selectedKey ->
            Log.d("HangulInputMethodService", "Selected candidate key: $selectedKey")
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
        candidateScrollView.addCards(hanjaText, hanjaMap)
        Log.d("HangulInputMethodService", "populateCandidates completed with ${hanjaMap.size} items")
    }

    /**
     * 후보자 뷰를 없애는 메서드입니다.
     * @param dismissAll Boolean 모든 후보자를 제거할지 여부입니다.
     */
    private fun dismissCandidates(dismissAll: Boolean = false) {
        hanjaOverlay?.dismissOverlay()
        if (dismissAll || !prefHanjaUseOverlay) {
            setCandidatesViewShown(false)
        }
    }
}
