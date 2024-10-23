package kr.stonecold.exkeyko

import android.annotation.SuppressLint
import android.util.Log
import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * 유틸리티 함수를 제공하는 오브젝트
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
     * 시스템 속성 값을 조회하는 메서드
     * @param key 속성 키
     * @return String? 속성 값
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
     * 동일 파일 비교 메서드
     * @param assetInputStream 비교 대상
     * @param filePath 비교 대상
     * @return Boolean 일치 여부
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
     * 입력 스트림의 해시값(SHA-256)을 계산하는 메서드
     * @param inputStream 계산을 위한 입력 Stream
     * @return String 해쉬값
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
