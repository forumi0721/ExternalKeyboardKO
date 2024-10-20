package kr.stonecold.exkeyko

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kr.stonecold.exkeyko.ui.theme.ExKeyKOTheme

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13 이상에서만 알림 권한 요청
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionIfNeeded()
        }

        // 설정 화면을 구성하는 Composable 함수 호출
        setContent {
            ExKeyKOTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(context = this)
                }
            }
        }
    }

    /**
     * Android 13 이상에서 알림 권한을 요청합니다.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermissionIfNeeded() {
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        when {
            shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                Toast.makeText(this, getString(R.string.msg_post_notifications_toast), Toast.LENGTH_SHORT).show()
            }

            else -> {
                // 권한 요청
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    if (isGranted) {
                        Toast.makeText(this, getString(R.string.msg_post_notifications_grant), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, getString(R.string.msg_post_notifications_deny), Toast.LENGTH_SHORT).show()
                    }
                }.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun SettingsScreen(context: Context) {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // 상태 변수로 설정 값을 관리
    var selectedEnglishKeyboard by remember { mutableStateOf(prefs.getString("pref_english_layout", PreferenceDefaults.pref_english_layout) ?: PreferenceDefaults.pref_english_layout) }
    var selectedKoreanKeyboard by remember { mutableStateOf(prefs.getString("pref_hangul_layout", PreferenceDefaults.pref_hangul_layout) ?: PreferenceDefaults.pref_hangul_layout) }
    var isAutoReorderEnabled by remember { mutableStateOf(prefs.getBoolean("pref_hangul_auto_reorder", PreferenceDefaults.pref_hangul_auto_reorder)) }
    var isCombiOnDoubleStrokeEnabled by remember { mutableStateOf(prefs.getBoolean("pref_hangul_combi_on_double_stroke", PreferenceDefaults.pref_hangul_combi_on_double_stroke)) }
    var isNonChoseongCombiEnabled by remember { mutableStateOf(prefs.getBoolean("pref_hangul_non_choseong_combi", PreferenceDefaults.pref_hangul_non_choseong_combi)) }
    var prefInputModeStatusbarMessage by remember { mutableStateOf(prefs.getBoolean("pref_input_mode_statusbar_message", PreferenceDefaults.pref_input_mode_statusbar_message)) }
    var prefInputModeToastMessage by remember { mutableStateOf(prefs.getBoolean("pref_input_mode_toast_message", PreferenceDefaults.pref_input_mode_toast_message)) }
    var prefUseEscEnglishMode by remember { mutableStateOf(prefs.getBoolean("pref_use_esc_english_mode", PreferenceDefaults.pref_use_esc_english_mode)) }
    var prefUseLeftShiftSpace by remember { mutableStateOf(prefs.getBoolean("pref_use_left_shift_space", PreferenceDefaults.pref_use_left_shift_space)) }
    var prefUseRightShiftSpace by remember { mutableStateOf(prefs.getBoolean("pref_use_right_shift_space", PreferenceDefaults.pref_use_right_shift_space)) }
    var prefUseRightAlt by remember { mutableStateOf(prefs.getBoolean("pref_use_right_alt", PreferenceDefaults.pref_use_right_alt)) }
    var prefUseRightCtrl by remember { mutableStateOf(prefs.getBoolean("pref_use_right_ctrl", PreferenceDefaults.pref_use_right_ctrl)) }
    var prefUseCtrlGraveToEsc by remember { mutableStateOf(prefs.getBoolean("pref_use_ctrl_grave_to_esc", PreferenceDefaults.pref_use_ctrl_grave_to_esc)) }
    var prefUseCtrlNumberToFuntion by remember { mutableStateOf(prefs.getBoolean("pref_use_ctrl_number_to_function", PreferenceDefaults.pref_use_ctrl_number_to_function)) }
    var textInput by remember { mutableStateOf(TextFieldValue("")) }

    val optionEnglishKeyboard = listOf(
        stringResource(R.string.english_qwerty) to "q",
        stringResource(R.string.english_dvorak) to "d",
        stringResource(R.string.english_colemak) to "c",
    )
    val optionKoreanKeyboard = listOf(
        stringResource(R.string.hangul_2) to "2",
        stringResource(R.string.hangul_2y) to "2y",
        stringResource(R.string.hangul_3f) to "3f",
        stringResource(R.string.hangul_39) to "39",
        stringResource(R.string.hangul_3s) to "3s",
        stringResource(R.string.hangul_3y) to "3y",
        stringResource(R.string.hangul_32) to "32",
        stringResource(R.string.hangul_ahn) to "ahn",
        stringResource(R.string.hangul_ro) to "ro",
    )

    // 설정 화면 레이아웃
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            item {
                // 앱 정보 섹션
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.app_title),
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(id = R.string.by_stonecold),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // 키보드 설정 카테고리
                CategorySection(title = stringResource(R.string.input_method_settings_title))
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // 키보드 설정 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { openKeyboardSettings(context) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text(text = stringResource(R.string.keyboard_activation_title), color = Color.White)
                    }
                    Button(
                        onClick = { openDefaultInputMethod(context) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text(text = stringResource(R.string.change_input_method_title), color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                // 키보드 배열 선택 섹션
                OptionItemKeyboard(
                    title = stringResource(R.string.english_keyboard_title),
                    description = stringResource(R.string.english_keyboard_desc),
                    keyboardOptions = optionEnglishKeyboard,
                    selectedKeyboard = selectedEnglishKeyboard,
                    onKeyboardSelected = { newValue ->
                        selectedEnglishKeyboard = newValue
                        savePreference(prefs, "pref_english_layout", newValue)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OptionItemKeyboard(
                    title = stringResource(R.string.korean_keyboard_title),
                    description = stringResource(R.string.korean_keyboard_desc),
                    keyboardOptions = optionKoreanKeyboard,
                    selectedKeyboard = selectedKoreanKeyboard,
                    onKeyboardSelected = { newValue ->
                        selectedKoreanKeyboard = newValue
                        savePreference(prefs, "pref_hangul_layout", newValue)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 각종 옵션 선택 섹션
            item {
                OptionItem(
                    title = stringResource(R.string.hangul_auto_reorder_title),
                    description = stringResource(R.string.hangul_auto_reorder_desc),
                    checkedState = isAutoReorderEnabled,
                    isEnabled = true,
                    onCheckedChange = { newValue ->
                        isAutoReorderEnabled = newValue
                        savePreference(prefs, "pref_hangul_auto_reorder", newValue)
                    }
                )
                OptionItem(
                    title = stringResource(R.string.hangul_combi_on_double_stroke_title),
                    description = stringResource(R.string.hangul_combi_on_double_stroke_desc),
                    checkedState = isCombiOnDoubleStrokeEnabled,
                    isEnabled = true,
                    onCheckedChange = { newValue ->
                        isCombiOnDoubleStrokeEnabled = newValue
                        savePreference(prefs, "pref_hangul_combi_on_double_stroke", newValue)
                    }
                )
                OptionItem(
                    title = stringResource(R.string.hangul_non_choseong_combi_title),
                    description = stringResource(R.string.hangul_non_choseong_combi_desc),
                    checkedState = isNonChoseongCombiEnabled,
                    isEnabled = true,
                    onCheckedChange = { newValue ->
                        isNonChoseongCombiEnabled = newValue
                        savePreference(prefs, "pref_hangul_non_choseong_combi", newValue)
                    }
                )
                OptionItem(
                    title = stringResource(R.string.input_mode_statusbar_message_title),
                    description = stringResource(R.string.input_mode_statusbar_message_desc),
                    checkedState = prefInputModeStatusbarMessage,
                    isEnabled = true,
                    onCheckedChange = { newValue ->
                        prefInputModeStatusbarMessage = newValue
                        savePreference(prefs, "pref_input_mode_statusbar_message", newValue)
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OptionItemWithPermission(
                        title = stringResource(R.string.input_mode_toast_message_title),
                        description = stringResource(R.string.input_mode_toast_message_desc),
                        checkedState = prefInputModeToastMessage,
                        onCheckedChange = { newValue ->
                            prefInputModeToastMessage = newValue
                            savePreference(prefs, "pref_input_mode_toast_message", newValue)
                        },
                        prefs = prefs
                    )
                } else {
                    OptionItem(
                        title = stringResource(R.string.input_mode_toast_message_title),
                        description = stringResource(R.string.input_mode_toast_message_desc),
                        checkedState = prefInputModeToastMessage,
                        isEnabled = true,
                        onCheckedChange = { newValue ->
                            prefInputModeToastMessage = newValue
                            savePreference(prefs, "pref_input_mode_toast_message", newValue)
                        }
                    )
                }
                OptionItem(
                    title = stringResource(R.string.use_esc_english_mode),
                    description = stringResource(R.string.use_esc_english_desc),
                    checkedState = prefUseEscEnglishMode,
                    isEnabled = true,
                    onCheckedChange = { newValue ->
                        prefUseEscEnglishMode = newValue
                        savePreference(prefs, "pref_use_esc_english_mode", newValue)
                    }
                )
                OptionItem(
                    title = stringResource(R.string.use_left_shift_space_title),
                    description = stringResource(R.string.use_left_shift_space_desc),
                    checkedState = prefUseLeftShiftSpace,
                    isEnabled = true,
                    onCheckedChange = { newValue ->
                        prefUseLeftShiftSpace = newValue
                        savePreference(prefs, "pref_use_shift_space", newValue)
                    }
                )
                OptionItem(
                    title = stringResource(R.string.use_right_shift_space_title),
                    description = stringResource(R.string.use_right_shift_space_desc),
                    checkedState = prefUseRightShiftSpace,
                    isEnabled = true,
                    onCheckedChange = { newValue ->
                        prefUseRightShiftSpace = newValue
                        savePreference(prefs, "pref_use_shift_space", newValue)
                    }
                )
                OptionItem(
                    title = stringResource(R.string.use_right_alt_title),
                    description = stringResource(R.string.use_right_alt_desc),
                    checkedState = prefUseRightAlt,
                    isEnabled = true,
                    onCheckedChange = { newValue ->
                        prefUseRightAlt = newValue
                        savePreference(prefs, "pref_use_right_alt", newValue)
                    }
                )
                OptionItem(
                    title = stringResource(R.string.use_right_ctrl_title),
                    description = stringResource(R.string.use_right_ctrl_desc),
                    checkedState = prefUseRightCtrl,
                    isEnabled = true,
                    onCheckedChange = { newValue ->
                        prefUseRightCtrl = newValue
                        savePreference(prefs, "pref_use_right_ctrl", newValue)
                    }
                )
                OptionItem(
                    title = stringResource(R.string.use_ctrl_grave_to_esc_title),
                    description = stringResource(R.string.use_ctrl_grave_to_esc_desc),
                    checkedState = prefUseCtrlGraveToEsc,
                    isEnabled = true,
                    onCheckedChange = { newValue ->
                        prefUseCtrlGraveToEsc = newValue
                        savePreference(prefs, "pref_use_ctrl_grave_to_esc", newValue)
                    }
                )
                OptionItem(
                    title = stringResource(R.string.use_ctrl_number_to_function_title),
                    description = stringResource(R.string.use_ctrl_number_to_function_title_desc),
                    checkedState = prefUseCtrlNumberToFuntion,
                    isEnabled = true,
                    onCheckedChange = { newValue ->
                        prefUseCtrlNumberToFuntion = newValue
                        savePreference(prefs, "pref_use_ctrl_number_to_function", newValue)
                    }
                )
            }

            item {
                // 공지 섹션
                NoticeSection()
            }
        }

        // 하단 고정 입력 필드
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, Color.Gray)
        ) {
            Column {
                Text(
                    text = stringResource(R.string.input_test),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
                )
                TextField(
                    value = textInput,
                    onValueChange = { newText -> textInput = newText },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .padding(8.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                    maxLines = 5
                )
            }
        }
    }
}

@Composable
fun CategorySection(title: String) {
    Text(
        text = title,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(8.dp)
    )
}

@Composable
fun OptionItemKeyboard(
    title: String,
    description: String,
    keyboardOptions: List<Pair<String, String>>,
    selectedKeyboard: String,
    onKeyboardSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }

        // 오른쪽: DropdownMenu
        Box(
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            var expanded by remember { mutableStateOf(false) }

            Button(onClick = { expanded = !expanded }) {
                Text(text = keyboardOptions.firstOrNull { it.second == selectedKeyboard }?.first ?: stringResource(R.string.select))
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                keyboardOptions.forEach { (option, value) ->
                    DropdownMenuItem(
                        onClick = {
                            onKeyboardSelected(value)
                            expanded = false
                        },
                        text = { Text(option) }
                    )
                }
            }
        }
    }
}

@Composable
fun OptionItem(title: String, description: String, checkedState: Boolean, isEnabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checkedState,
            onCheckedChange = onCheckedChange,
            enabled = isEnabled
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun OptionItemWithPermission(
    title: String,
    description: String,
    checkedState: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    prefs: SharedPreferences
) {
    // Activity로부터 권한 요청을 처리하는 등록
    val activity = LocalContext.current as ComponentActivity
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                onCheckedChange(true)  // 권한이 부여되면 체크박스를 true로 설정
                savePreference(prefs, "pref_input_mode_toast_message", true)
            } else {
                onCheckedChange(false)  // 권한이 없으면 false로 유지
                savePreference(prefs, "pref_input_mode_toast_message", false)
            }
        }
    )

    // 권한이 있는지 확인하는 함수
    fun hasNotificationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // OptionItem 호출
    OptionItem(
        title = title,
        description = description,
        checkedState = if (hasNotificationPermission()) checkedState else false,  // 권한이 없으면 false
        isEnabled = true,
        onCheckedChange = { newValue ->
            if (newValue) {
                if (hasNotificationPermission()) {
                    // 권한이 있으면 true로 설정
                    onCheckedChange(true)
                    savePreference(prefs, "pref_input_mode_toast_message", true)
                } else {
                    // 권한이 없으면 권한 요청
                    permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                // 사용자가 체크박스를 false로 변경한 경우
                onCheckedChange(false)
                savePreference(prefs, "pref_input_mode_toast_message", false)
            }
        }
    )
}

@Composable
fun NoticeSection() {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = stringResource(id = R.string.notice),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.feature_not_guaranteed),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.notice_libhangul_use),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.notice_additional_info),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/**
 * 키보드 설정 화면으로 이동
 * @param context Context 애플리케이션 컨텍스트
 */
fun openKeyboardSettings(context: Context) {
    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
    context.startActivity(intent)
}

/**
 * 기본 입력기 선택 화면으로 이동
 * @param context Context 애플리케이션 컨텍스트
 */
fun openDefaultInputMethod(context: Context) {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showInputMethodPicker()
}

/**
 * 설정값을 SharedPreferences에 저장
 * @param prefs SharedPreferences 설정 객체
 * @param key String 설정의 키
 * @param value Any 저장할 값 (String 또는 Boolean)
 */
fun savePreference(prefs: SharedPreferences, key: String, value: Any) {
    with(prefs.edit()) {
        when (value) {
            is String -> putString(key, value)
            is Boolean -> putBoolean(key, value)
        }
        apply()
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ExKeyKOTheme {
        SettingsScreen(context = null as? Context ?: LocalContext.current)
    }
}