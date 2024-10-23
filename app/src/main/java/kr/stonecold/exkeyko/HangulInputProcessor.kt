package kr.stonecold.exkeyko

import android.content.Context
import android.util.Log
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream

/**
 * 한글 입력 처리를 위해 JNI와 연결하는 Bridge 클래스
 * @param keyboardLayout 한글 키보드 레이아웃
 */
@Suppress("RedundantUnitReturnType")
class HangulInputProcessor(private val keyboardLayout: String) : Closeable {

    /**
     * Static Init
     */
    companion object {
        init {
            System.loadLibrary("hangulinputprocessor")
        }
    }

    /**
     * Init
     */
    init {
        initialize()
        createContext(keyboardLayout)
        Log.d("HangulInputProcessor", "HangulInputProcessor initialized with layout: $keyboardLayout")
    }

    /**
     * Close 메서드
     */
    override fun close() {
        finalizeProcessor()
        deleteContext()
        Log.d("HangulInputProcessor", "HangulInputProcessor resources have been released")
    }

    /**
     * 한자 초기화 메서드
     * @param context Context
     * @return 초기화 성공 여부
     */
    fun initHanjaTable(context: Context): Boolean {
        val fileName = "dictionary.txt"
        val filePath = File(context.filesDir, fileName)

        try {
            val assetManager = context.assets

            if (!filePath.exists() || !Util.isFileSame(assetManager.open(fileName), filePath)) {
                Log.d("HangulInputProcessor", "한자 테이블을 업데이트합니다.")
                if (filePath.exists()) {
                    filePath.delete()
                }
                assetManager.open(fileName).use { input ->
                    FileOutputStream(filePath).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("HangulInputProcessor", "한자 테이블 초기화 중 오류 발생", e)
            return false
        }

        val result = loadHanjaTable(filePath.absolutePath)
        Log.d("HangulInputProcessor", "Hanja table loaded: $result")
        return result
    }

    // JNI 메서드 선언

    /**
     * libhangul 초기화 메서드
     * @return 성공 여부
     */
    private external fun initialize(): Int

    /**
     * libhangul 종료 메서드
     * @return 성공 여부
     */
    private external fun finalizeProcessor(): Int

    /**
     * 키 입력을 처리하여 한글 조합을 수행하는 메서드
     * @param ascii 입력 키
     * @return 성공 여부
     */
    external fun process(ascii: Int): Boolean

    /**
     * preedit 문자열을 반환하는 메서드
     * @return preedit 문자열
     */
    external fun getPreeditString(): String?

    /**
     * commit 문자열을 반환하는 메서드
     * @return commit 문자열
     */
    external fun getCommitString(): String?

    /**
     * 입력 상태 초기화 메서드
     */
    external fun reset(): Unit

    /**
     * 입력 상태를 완료하고 commit 문자열을 반환하는 메서드
     * @return commit 문자열
     */
    external fun flush(): String?

    /**
     * backspace 입력 처리 메서드
     * @return 성공 여부
     */
    external fun backspace(): Boolean

    /**
     * 조합 중인 문자가 비어 있는지 확인하는 메서드
     * @return 조합 Empty 여부
     */
    external fun isEmpty(): Boolean

    /**
     * 조합 중인 초성이 있는지 확인하는 메서드
     * @return 초성 존재 여부
     */
    external fun hasChoseong(): Boolean

    /**
     * 조합 중인 중성이 있는지 확인하는 메서드
     * @return 중성 존재 여부
     */
    external fun hasJungseong(): Boolean

    /**
     * 조합 중인 종성이 있는지 확인하는 메서드
     * @return 종성 존재 여부
     */
    external fun hasJongseong(): Boolean

    /**
     * libhangul 옵션을 확인하는 코드
     * @param option 옵션 코드
     * @return 옵션 값
     */
    external fun getOption(option: Int): Boolean

    /**
     * libhangul 옵션을 설정하는 메서드
     * @param option 옵션 코드
     * @param value 옵션 값
     */
    external fun setOption(option: Int, value: Boolean): Unit

    /**
     * 한글 레이아웃을 설정하는 메서드
     * @param keyboardLayout 레이아웃 ID
     */
    external fun selectKeyboard(keyboardLayout: String): Unit

    /**
     * hic 객체를 생성하는 메서드
     * @param keyboardLayout 레이아웃 ID
     */
    private external fun createContext(keyboardLayout: String): Unit

    /**
     * hic 객체를 삭제하는 메서드
     */
    private external fun deleteContext(): Unit

    /**
     * hic가 transliteration 방식인지 확인하는 메서드
     * @return transliteration 여부
     */
    external fun isTransliteration(): Boolean

    /**
     * 한자 테이블 로드 메서드
     * @param filePath 한자 테이블 파일 경로
     * @return 성공 여부
     */
    private external fun loadHanjaTable(filePath: String): Boolean

    /**
     * 한자 테이블 삭제 메서드
     */
    external fun deleteHanjaTable(): Unit

    /**
     * 정확하게 일치하는 한자를 검색하는 메서드
     * @param key 입력 값
     * @return 한자 배열
     */
    external fun matchExactHanja(key: String): Array<String>?

    /**
     * 정확하게 일치하는 한자를 검색하는 메서드 (Map)
     * @param key 입력 값
     * @return 한자 맵
     */
    external fun matchExactHanjaMap(key: String): LinkedHashMap<String, String>?

    /**
     * 접두사로 일치하는 한자를 검색하는 메서드
     * @param key 입력 값
     * @return Array<String>? 한자 배열
     */
    external fun matchPrefixHanja(key: String): Array<String>?

    /**
     * 접미사로 일치하는 한자를 검색하는 메서드
     * @param key 입력 값
     * @return 한자 배열
     */
    external fun matchSuffixHanja(key: String): Array<String>?
}
