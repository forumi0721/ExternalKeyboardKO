package kr.stonecold.exkeyko

import android.content.Context
import android.util.Log
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream

/**
 * 한글 입력 처리를 담당하는 클래스입니다.
 * JNI를 통해 네이티브 코드를 호출합니다.
 * @param keyboardLayout String 사용할 키보드 배열입니다.
 */
@Suppress("RedundantUnitReturnType")
class HangulInputProcessor(private val keyboardLayout: String) : Closeable {

    companion object {
        init {
            System.loadLibrary("hangulinputprocessor")
        }
    }

    init {
        initialize()
        createContext(keyboardLayout)
        Log.d("HangulInputProcessor", "HangulInputProcessor initialized with layout: $keyboardLayout")
    }

    /**
     * 리소스를 해제하고 네이티브 객체를 삭제합니다.
     */
    override fun close() {
        finalizeProcessor()
        deleteContext()
        Log.d("HangulInputProcessor", "HangulInputProcessor resources have been released")
    }

    /**
     * 한자 파일을 로드하고 필요한 경우 업데이트합니다.
     * @param context Context 파일 저장 및 asset 접근을 위한 Android Context입니다.
     * @return Boolean 한자 테이블 로드 성공 여부를 반환합니다.
     */
    fun initHanjaTable(context: Context): Boolean {
        val fileName = "dictionary.txt"
        val filePath = File(context.filesDir, fileName)

        try {
            val assetManager = context.assets
            val assetInputStream = assetManager.open(fileName)

            if (!filePath.exists() || !Util.isFileSame(assetInputStream, filePath)) {
                Log.d("HangulInputProcessor", "한자 테이블을 업데이트합니다.")
                if (filePath.exists()) {
                    filePath.delete()
                }
                val outputFile = File(context.filesDir, fileName)
                assetInputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
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
     * 프로세서를 초기화합니다.
     * @return Boolean 성공 여부를 반환합니다.
     */
    private external fun initialize(): Int

    /**
     * 프로세서를 종료합니다.
     * @return Boolean 성공 여부를 반환합니다.
     */
    private external fun finalizeProcessor(): Int

    /**
     * 키 입력을 처리하여 한글 조합을 수행합니다.
     * @param ascii Int 입력된 키의 ASCII 값입니다.
     * @return Boolean 조합 성공 여부를 반환합니다.
     */
    external fun process(ascii: Int): Boolean

    /**
     * 현재 조합 중인 preedit 문자열을 반환합니다.
     * @return String? preedit 문자열 또는 null입니다.
     */
    external fun getPreeditString(): String?

    /**
     * 현재 조합 완료된 commit 문자열을 반환합니다.
     * @return String? commit 문자열 또는 null입니다.
     */
    external fun getCommitString(): String?

    /**
     * 입력 상태를 초기화합니다.
     */
    external fun reset(): Unit

    /**
     * 입력 상태를 완료하고 commit 문자열을 반환합니다.
     * @return String? commit 문자열 또는 null입니다.
     */
    external fun flush(): String?

    /**
     * backspace 입력을 처리합니다.
     * @return Boolean backspace 처리 성공 여부를 반환합니다.
     */
    external fun backspace(): Boolean

    /**
     * 현재 조합 중인 문자가 비어 있는지 확인합니다.
     * @return Boolean 조합 중인 문자가 비어 있는지 여부를 반환합니다.
     */
    external fun isEmpty(): Boolean

    /**
     * 조합 중인 초성이 있는지 확인합니다.
     * @return Boolean 초성이 있는지 여부를 반환합니다.
     */
    external fun hasChoseong(): Boolean

    /**
     * 조합 중인 중성이 있는지 확인합니다.
     * @return Boolean 중성이 있는지 여부를 반환합니다.
     */
    external fun hasJungseong(): Boolean

    /**
     * 조합 중인 종성이 있는지 확인합니다.
     * @return Boolean 종성이 있는지 여부를 반환합니다.
     */
    external fun hasJongseong(): Boolean

    /**
     * 조합 옵션을 확인합니다.
     * @param option Int 옵션 코드입니다.
     * @return Boolean 옵션 값을 반환합니다.
     */
    external fun getOption(option: Int): Boolean

    /**
     * 조합 옵션을 설정합니다.
     * @param option Int 옵션 코드입니다.
     * @param value Boolean 설정할 옵션 값입니다.
     */
    external fun setOption(option: Int, value: Boolean): Unit

    /**
     * 자판 배열을 선택합니다.
     * @param id String 자판 배열 ID입니다.
     */
    external fun selectKeyboard(id: String): Unit

    /**
     * HangulInputContext 객체를 생성합니다.
     * @param keyboardLayout String 키보드 배열입니다.
     */
    private external fun createContext(keyboardLayout: String): Unit

    /**
     * HangulInputContext 객체를 삭제합니다.
     */
    private external fun deleteContext(): Unit

    /**
     * 현재 HangulInputContext가 transliteration 방식인지 확인합니다.
     * @return Boolean transliteration 여부를 반환합니다.
     */
    external fun isTransliteration(): Boolean

    /**
     * 한자 테이블을 로드합니다.
     * @param filePath String 한자 테이블 파일 경로입니다.
     * @return Boolean 로드 성공 여부를 반환합니다.
     */
    private external fun loadHanjaTable(filePath: String): Boolean

    /**
     * 한자 테이블을 삭제합니다.
     */
    external fun deleteHanjaTable(): Unit

    /**
     * 정확하게 일치하는 한자를 검색합니다.
     * @param key String 검색할 키입니다.
     * @return Array<String>? 일치하는 한자의 배열 또는 null을 반환합니다.
     */
    external fun matchExactHanja(key: String): Array<String>?

    /**
     * 정확하게 일치하는 한자를 검색하고 맵으로 반환합니다.
     * @param key String 검색할 키입니다.
     * @return LinkedHashMap<String, String>? 일치하는 한자의 맵 또는 null을 반환합니다.
     */
    external fun matchExactHanjaMap(key: String): LinkedHashMap<String, String>?

    /**
     * 접두사로 일치하는 한자를 검색합니다.
     * @param key String 검색할 키입니다.
     * @return Array<String>? 일치하는 한자의 배열 또는 null을 반환합니다.
     */
    external fun matchPrefixHanja(key: String): Array<String>?

    /**
     * 접미사로 일치하는 한자를 검색합니다.
     * @param key String 검색할 키입니다.
     * @return Array<String>? 일치하는 한자의 배열 또는 null을 반환합니다.
     */
    external fun matchSuffixHanja(key: String): Array<String>?
}
