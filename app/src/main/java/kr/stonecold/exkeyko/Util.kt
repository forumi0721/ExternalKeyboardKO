package kr.stonecold.exkeyko

import android.annotation.SuppressLint
import android.util.Log
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * 유틸리티 함수를 제공하는 객체입니다.
 */
@Suppress("unused")
@SuppressLint("PrivateApi")
object Util {

    /**
     * System Properties를 읽어오기 위한 get 메서드 캐싱
     */
    private val getMethod by lazy {
        val systemPropertiesClass = Class.forName("android.os.SystemProperties")
        systemPropertiesClass.getMethod("get", String::class.java)
    }

    /**
     * 주어진 키에 해당하는 시스템 속성 값을 가져옵니다.
     * @param key String 시스템 속성의 키입니다.
     * @return String? 해당 키에 대한 시스템 속성 값 또는 null (오류 발생 시)
     */
    fun getSystemProperty(key: String): String? {
        return try {
            getMethod.invoke(null, key) as? String
        } catch (e: Exception) {
            Log.e("Util", "시스템 속성 가져오기 실패: $key", e)
            null
        }
    }

    /**
     * 파일의 해시값을 비교하여 동일한 파일인지 확인합니다.
     * @param assetInputStream InputStream 앱의 assets에서 가져온 InputStream입니다.
     * @param filePath File 디스크 상의 파일 경로입니다.
     * @return Boolean 두 파일이 동일한지 여부를 반환합니다.
     */
    fun isFileSame(assetInputStream: InputStream, filePath: File): Boolean {
        return try {
            val assetHash = calculateHash(assetInputStream)
            val fileHash = calculateHash(filePath.inputStream())
            Log.d("Util", "해시 비교: assetHash = $assetHash, fileHash = $fileHash")
            assetHash == fileHash
        } catch (e: Exception) {
            Log.e("Util", "파일 해시 비교 중 오류 발생", e)
            false
        }
    }

    /**
     * 입력 스트림의 해시값(SHA-256)을 계산합니다.
     * @param inputStream InputStream 해시값을 계산할 파일의 InputStream입니다.
     * @return String 해시값을 반환합니다.
     */
    private fun calculateHash(inputStream: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(1024)
        var bytesRead: Int
        inputStream.use { stream ->
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
