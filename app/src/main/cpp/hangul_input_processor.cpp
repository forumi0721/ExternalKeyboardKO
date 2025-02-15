#include <jni.h>
#include <string>
#include <hangul.h>  // libhangul 헤더 파일

static HangulInputContext *context = nullptr;
static HanjaTable* hanja_table = nullptr;

/**
 * @brief UCS4 형식의 문자열을 UTF-8로 변환하는 메서드
 * @param ucs4_str 입력 문자열
 * @return 출력 문자열
 */
std::string ucs4_to_utf8(const ucschar* ucs4_str) {
    std::string utf8_str;
    for (int i = 0; ucs4_str[i] != '\0'; i++) {
        char utf8_char[5] = {0};  // UTF-8 문자는 최대 4바이트
        int len = 0;

        // 유니코드 값을 UTF-8로 변환
        if (ucs4_str[i] < 0x80) {
            utf8_char[0] = static_cast<char>(ucs4_str[i]);
            len = 1;
        } else if (ucs4_str[i] < 0x800) {
            utf8_char[0] = static_cast<char>((ucs4_str[i] >> 6) | 0xC0);
            utf8_char[1] = static_cast<char>((ucs4_str[i] & 0x3F) | 0x80);
            len = 2;
        } else if (ucs4_str[i] < 0x10000) {
            utf8_char[0] = static_cast<char>((ucs4_str[i] >> 12) | 0xE0);
            utf8_char[1] = static_cast<char>(((ucs4_str[i] >> 6) & 0x3F) | 0x80);
            utf8_char[2] = static_cast<char>((ucs4_str[i] & 0x3F) | 0x80);
            len = 3;
        } else {
            utf8_char[0] = static_cast<char>((ucs4_str[i] >> 18) | 0xF0);
            utf8_char[1] = static_cast<char>(((ucs4_str[i] >> 12) & 0x3F) | 0x80);
            utf8_char[2] = static_cast<char>(((ucs4_str[i] >> 6) & 0x3F) | 0x80);
            utf8_char[3] = static_cast<char>((ucs4_str[i] & 0x3F) | 0x80);
            len = 4;
        }

        utf8_str.append(utf8_char, len);  // 변환된 UTF-8 문자열 추가
    }
    return utf8_str;
}

/**
 * @brief libhangul 초기화 메서드
 * @return 성공 여부
 */
extern "C"
JNIEXPORT jint JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_initialize(JNIEnv *env, jobject obj) {
    return hangul_init();
}

/**
 * @brief libhangul 종료 메서드
 * @return 성공 여부
 */
extern "C"
JNIEXPORT jint JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_finalizeProcessor(JNIEnv *env, jobject obj) {
    return hangul_fini();
}

/**
 * @brief 키 입력을 처리하여 한글 조합을 수행하는 메서드
 * @param ascii 입력 키
 * @return 성공 여부
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_process(JNIEnv *env, jobject obj, jint ascii) {
    if (context != nullptr) {
        bool result = hangul_ic_process(context, ascii);
        return result ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

/**
 * @brief preedit 문자열을 반환하는 메서드
 * @return preedit 문자열
 */
extern "C"
JNIEXPORT jstring JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_getPreeditString(JNIEnv *env, jobject obj) {
    if (context != nullptr) {
        const ucschar* preedit = hangul_ic_get_preedit_string(context);
        if (preedit != nullptr) {
            std::string utf8_preedit = ucs4_to_utf8(preedit);
            return env->NewStringUTF(utf8_preedit.c_str());
        }
    }
    return env->NewStringUTF("");
}

/**
  * @brief commit 문자열을 반환하는 메서드
  * @return commit 문자열
  */
extern "C"
JNIEXPORT jstring JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_getCommitString(JNIEnv *env, jobject obj) {
    if (context != nullptr) {
        const ucschar* commit = hangul_ic_get_commit_string(context);
        if (commit != nullptr) {
            std::string utf8_commit = ucs4_to_utf8(commit);
            return env->NewStringUTF(utf8_commit.c_str());
        }
    }
    return env->NewStringUTF("");
}

/**
 * @brief 입력 상태 초기화 메서드
 */
extern "C"
JNIEXPORT void JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_reset(JNIEnv *env, jobject obj) {
    if (context != nullptr) {
        hangul_ic_reset(context);
    }
}

/**
 * @brief 입력 상태를 완료하고 commit 문자열을 반환하는 메서드
 * @return commit 문자열
 */
extern "C"
JNIEXPORT jstring JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_flush(JNIEnv *env, jobject obj) {
    if (context != nullptr) {
        const ucschar* flushString = hangul_ic_flush(context);
        if (flushString != nullptr) {
            std::string utf8_flushString = ucs4_to_utf8(flushString);
            return env->NewStringUTF(utf8_flushString.c_str());
        }
    }
    return env->NewStringUTF("");
}

/**
 * @brief backspace 입력 처리 메서드
 * @return 성공 여부
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_backspace(JNIEnv *env, jobject obj) {
    if (context != nullptr) {
        bool result = hangul_ic_backspace(context);
        return result ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

/**
 * @brief 조합 중인 문자가 비어 있는지 확인하는 메서드
 * @return 조합 Empty 여부
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_isEmpty(JNIEnv *env, jobject obj) {
    if (context != nullptr) {
        bool result = hangul_ic_is_empty(context);
        return result ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_TRUE;
}

/**
 * @brief 조합 중인 초성이 있는지 확인하는 메서드
 * @return 초성 존재 여부
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_hasChoseong(JNIEnv *env, jobject obj) {
    if (context != nullptr) {
        bool result = hangul_ic_has_choseong(context);
        return result ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

/**
 * @brief 조합 중인 중성이 있는지 확인하는 메서드
 * @return 중성 존재 여부
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_hasJungseong(JNIEnv *env, jobject obj) {
    if (context != nullptr) {
        bool result = hangul_ic_has_jungseong(context);
        return result ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

/**
 * @brief 조합 중인 종성이 있는지 확인하는 메서드
 * @return 종성 존재 여부
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_hasJongseong(JNIEnv *env, jobject obj) {
    if (context != nullptr) {
        bool result = hangul_ic_has_jongseong(context);
        return result ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

/**
 * @brief libhangul 옵션을 확인하는 코드
 * @param option 옵션 코드
 * @return 옵션 값
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_getOption(JNIEnv *env, jobject obj, jint option) {
    if (context != nullptr) {
        bool result = hangul_ic_get_option(context, option);
        return result ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

/**
 * @brief libhangul 옵션을 설정하는 메서드
 * @param option 옵션 코드
 * @param value 옵션 값
 */
extern "C"
JNIEXPORT void JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_setOption(JNIEnv *env, jobject obj, jint option, jboolean value) {
    if (context != nullptr) {
        hangul_ic_set_option(context, option, value == JNI_TRUE);
    }
}

/**
 * @brief 한글 레이아웃을 설정하는 메서드
 * @param keyboardLayout 레이아웃 ID
 */
extern "C"
JNIEXPORT void JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_selectKeyboard(JNIEnv *env, jobject obj, jstring keyboardLayout) {
    const char *idStr = env->GetStringUTFChars(keyboardLayout, nullptr);
    if (context != nullptr) {
        hangul_ic_select_keyboard(context, idStr);
    }
    env->ReleaseStringUTFChars(keyboardLayout, idStr);
}

/**
 * @brief hic 객체를 생성하는 메서드
 * @param keyboardLayout 레이아웃 ID
 */
extern "C"
JNIEXPORT void JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_createContext(JNIEnv *env, jobject obj, jstring keyboardLayout) {
    const char *layoutStr = env->GetStringUTFChars(keyboardLayout, nullptr);
    if (context != nullptr) {
        hangul_ic_delete(context);
    }
    context = hangul_ic_new(layoutStr);
    env->ReleaseStringUTFChars(keyboardLayout, layoutStr);

    if (context == nullptr) {
        // Context 생성 실패 시 예외 처리 또는 로그 추가 가능
    }
}

/**
 * @brief hic 객체를 삭제하는 메서드
 */
extern "C"
JNIEXPORT void JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_deleteContext(JNIEnv *env, jobject obj) {
    if (context != nullptr) {
        hangul_ic_delete(context);
        context = nullptr;
    }
}

/**
 * @brief hic가 transliteration 방식인지 확인하는 메서드
 * @return transliteration 여부
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_isTransliteration(JNIEnv *env, jobject obj) {
    if (context != nullptr) {
        bool result = hangul_ic_is_transliteration(context);
        return result ? JNI_TRUE : JNI_FALSE;
    }
    return JNI_FALSE;
}

/**
 * @brief 한자 테이블 로드 메서드
 * @param filePath 한자 테이블 파일 경로
 * @return 성공 여부
 */
extern "C"
JNIEXPORT jboolean JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_loadHanjaTable(JNIEnv *env, jobject obj, jstring filePath) {
    const char *file_path = env->GetStringUTFChars(filePath, nullptr);
    hanja_table = hanja_table_load(file_path);
    env->ReleaseStringUTFChars(filePath, file_path);

    return hanja_table != nullptr ? JNI_TRUE : JNI_FALSE;
}

/**
 * @brief 한자 테이블 삭제 메서드
 */
extern "C"
JNIEXPORT void JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_deleteHanjaTable(JNIEnv *env, jobject obj) {
    if (hanja_table != nullptr) {
        hanja_table_delete(hanja_table);
        hanja_table = nullptr;
    }
}

/**
 * @brief 정확하게 일치하는 한자를 검색하는 메서드
 * @param key 입력 값
 * @return 한자 배열
 */
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_matchExactHanja(JNIEnv *env, jobject obj, jstring key) {
    if (hanja_table == nullptr) {
        return nullptr;  // 한자 테이블이 로드되지 않음
    }

    const char *key_str = env->GetStringUTFChars(key, nullptr);
    HanjaList *hanja_list = hanja_table_match_exact(hanja_table, key_str);
    env->ReleaseStringUTFChars(key, key_str);

    if (hanja_list == nullptr) {
        return nullptr;  // 일치하는 한자 없음
    }

    int list_size = hanja_list_get_size(hanja_list);  // 한자 리스트 크기 가져오기
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray resultArray = env->NewObjectArray(list_size, stringClass, nullptr);

    for (int i = 0; i < list_size; ++i) {
        const Hanja* hanja = hanja_list_get_nth(hanja_list, i);  // 리스트에서 n번째 한자 가져오기
        std::string combined = std::string(hanja_get_value(hanja)) + ":" + std::string(hanja_get_comment(hanja));
        jstring resultString = env->NewStringUTF(combined.c_str());
        env->SetObjectArrayElement(resultArray, i, resultString);
        env->DeleteLocalRef(resultString);
    }

    hanja_list_delete(hanja_list);  // 한자 리스트 삭제
    return resultArray;  // 결과 배열 반환
}

/**
 * @brief 정확하게 일치하는 한자를 검색하는 메서드 (Map)
 * @param key 입력 값
 * @return 한자 맵
 */
extern "C"
JNIEXPORT jobject JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_matchExactHanjaMap(JNIEnv *env, jobject obj, jstring key) {
    if (hanja_table == nullptr) {
        return nullptr;  // 한자 테이블이 로드되지 않음
    }

    const char *key_str = env->GetStringUTFChars(key, nullptr);
    HanjaList *hanja_list = hanja_table_match_exact(hanja_table, key_str);
    env->ReleaseStringUTFChars(key, key_str);

    if (hanja_list == nullptr) {
        return nullptr;  // 일치하는 한자 없음
    }

    int list_size = hanja_list_get_size(hanja_list);
    jclass linkedHashMapClass = env->FindClass("java/util/LinkedHashMap");
    jmethodID linkedHashMapInit = env->GetMethodID(linkedHashMapClass, "<init>", "()V");
    jmethodID linkedHashMapPut = env->GetMethodID(linkedHashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jobject linkedHashMap = env->NewObject(linkedHashMapClass, linkedHashMapInit);

    for (int i = 0; i < list_size; ++i) {
        const Hanja* hanja = hanja_list_get_nth(hanja_list, i);
        const char* hanja_value = hanja_get_value(hanja);
        const char* hanja_comment = hanja_get_comment(hanja);
        jstring jHanja = env->NewStringUTF(hanja_value);
        jstring jComment = env->NewStringUTF(hanja_comment);
        env->CallObjectMethod(linkedHashMap, linkedHashMapPut, jHanja, jComment);
        env->DeleteLocalRef(jHanja);
        env->DeleteLocalRef(jComment);
    }

    hanja_list_delete(hanja_list);
    return linkedHashMap;
}

/**
 * @brief 접두사로 일치하는 한자를 검색하는 메서드
 * @param key 입력 값
 * @return Array<String>? 한자 배열
 */
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_matchPrefixHanja(JNIEnv *env, jobject obj, jstring key) {
    if (hanja_table == nullptr) {
        return nullptr;  // 한자 테이블이 로드되지 않음
    }

    const char *key_str = env->GetStringUTFChars(key, nullptr);
    HanjaList *hanja_list = hanja_table_match_prefix(hanja_table, key_str);
    env->ReleaseStringUTFChars(key, key_str);

    if (hanja_list == nullptr) {
        return nullptr;  // 일치하는 한자 없음
    }

    int list_size = hanja_list_get_size(hanja_list);
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray resultArray = env->NewObjectArray(list_size, stringClass, nullptr);

    for (int i = 0; i < list_size; ++i) {
        const char *hanja_value = hanja_list_get_nth_value(hanja_list, i);
        jstring hanja_jstr = env->NewStringUTF(hanja_value);
        env->SetObjectArrayElement(resultArray, i, hanja_jstr);
        env->DeleteLocalRef(hanja_jstr);
    }

    hanja_list_delete(hanja_list);
    return resultArray;
}

/**
 * @brief 접미사로 일치하는 한자를 검색하는 메서드
 * @param key 입력 값
 * @return 한자 배열
 */
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_kr_stonecold_exkeyko_HangulInputProcessor_matchSuffixHanja(JNIEnv *env, jobject obj, jstring key) {
    if (hanja_table == nullptr) {
        return nullptr;  // 한자 테이블이 로드되지 않음
    }

    const char *key_str = env->GetStringUTFChars(key, nullptr);
    HanjaList *hanja_list = hanja_table_match_suffix(hanja_table, key_str);
    env->ReleaseStringUTFChars(key, key_str);

    if (hanja_list == nullptr) {
        return nullptr;  // 일치하는 한자 없음
    }

    int list_size = hanja_list_get_size(hanja_list);
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray resultArray = env->NewObjectArray(list_size, stringClass, nullptr);

    for (int i = 0; i < list_size; ++i) {
        const char *hanja_value = hanja_list_get_nth_value(hanja_list, i);
        jstring hanja_jstr = env->NewStringUTF(hanja_value);
        env->SetObjectArrayElement(resultArray, i, hanja_jstr);
        env->DeleteLocalRef(hanja_jstr);
    }

    hanja_list_delete(hanja_list);
    return resultArray;
}
