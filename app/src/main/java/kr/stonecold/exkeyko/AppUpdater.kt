package kr.stonecold.exkeyko

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object AppUpdater {
    const val DEFAULT_GITHUB_REPO = "forumi0721/ExternalKeyboardKO"

    data class ReleaseInfo(
        val tagName: String,
        val versionName: String,
        val versionCode: Int,
        val title: String,
        val notes: String,
        val htmlUrl: String,
        val apkDownloadUrl: String?,
        val apkFileName: String?,
        val apkSizeBytes: Long
    )

    sealed class UpdateCheckResult {
        data class UpdateAvailable(val release: ReleaseInfo) : UpdateCheckResult()
        data class UpToDate(val release: ReleaseInfo) : UpdateCheckResult()
        data object NoRelease : UpdateCheckResult()
    }

    private val SEMVER_REGEX = Regex("""^v?(\d+)\.(\d+)\.(\d+)(?:[-+].*)?$""", RegexOption.IGNORE_CASE)

    fun parseVersionCode(versionOrTag: String): Int? {
        val match = SEMVER_REGEX.matchEntire(versionOrTag.trim()) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: return null
        val patch = match.groupValues[3].toIntOrNull() ?: return null
        return major * 1_000_000 + minor * 1_000 + patch
    }

    fun normalizeVersionName(versionOrTag: String): String {
        val trimmed = versionOrTag.trim()
        return if (trimmed.startsWith("v", ignoreCase = true)) trimmed.substring(1) else trimmed
    }

    fun isNewerVersion(
        currentVersionName: String,
        currentVersionCode: Int,
        latestTagName: String
    ): Boolean {
        val latestCode = parseVersionCode(latestTagName) ?: return false
        val currentCode = parseVersionCode(currentVersionName) ?: currentVersionCode
        return latestCode > currentCode
    }

    fun parseReleaseJson(jsonStr: String): ReleaseInfo? {
        val obj = runCatching { JSONObject(jsonStr) }.getOrNull() ?: return null
        if (obj.optBoolean("draft", false) || obj.optBoolean("prerelease", false)) {
            return null
        }
        val tagName = obj.optString("tag_name", "").trim()
        val versionCode = parseVersionCode(tagName) ?: return null
        val versionName = normalizeVersionName(tagName)
        val title = obj.optString("name", "").trim().ifBlank { tagName }
        val notes = obj.optString("body", "").trim()
        val htmlUrl = obj.optString("html_url", "").trim()

        var apkUrl: String? = null
        var apkName: String? = null
        var apkSize = 0L

        val assets = obj.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val name = asset.optString("name", "").trim()
                val contentType = asset.optString("content_type", "").trim()
                val downloadUrl = asset.optString("url", "").trim()
                val isApk = name.endsWith(".apk", ignoreCase = true) ||
                    contentType.equals("application/vnd.android.package-archive", ignoreCase = true)
                if (isApk && downloadUrl.isNotBlank()) {
                    apkUrl = downloadUrl
                    apkName = name.ifBlank { "update-$tagName.apk" }
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }
        }

        return ReleaseInfo(
            tagName = tagName,
            versionName = versionName,
            versionCode = versionCode,
            title = title,
            notes = notes,
            htmlUrl = htmlUrl,
            apkDownloadUrl = apkUrl,
            apkFileName = apkName,
            apkSizeBytes = apkSize
        )
    }

    suspend fun checkLatestRelease(
        repoSlug: String = DEFAULT_GITHUB_REPO,
        currentVersionName: String = BuildConfig.VERSION_NAME,
        currentVersionCode: Int = BuildConfig.VERSION_CODE
    ): Result<UpdateCheckResult> = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("https://api.github.com/repos/$repoSlug/releases/latest").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/vnd.github+json")
                if (BuildConfig.GITHUB_TOKEN.isNotBlank()) setRequestProperty("Authorization", "Bearer ${BuildConfig.GITHUB_TOKEN}")
                setRequestProperty("User-Agent", "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}")
            }
            try {
                val code = conn.responseCode
                if (code == 404) {
                    return@runCatching UpdateCheckResult.NoRelease
                }
                if (code !in 200..299) {
                    error("GitHub API HTTP $code")
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val release = parseReleaseJson(body) ?: return@runCatching UpdateCheckResult.NoRelease
                if (isNewerVersion(currentVersionName, currentVersionCode, release.tagName)) {
                    UpdateCheckResult.UpdateAvailable(release)
                } else {
                    UpdateCheckResult.UpToDate(release)
                }
            } finally {
                conn.disconnect()
            }
        }
    }

    suspend fun downloadApk(
        context: Context,
        release: ReleaseInfo,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val downloadUrl = release.apkDownloadUrl?.takeIf { it.isNotBlank() }
                ?: error("다운로드 가능한 APK 에셋이 없습니다.")
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            updatesDir.listFiles()?.forEach { oldFile ->
                if (oldFile.isFile) oldFile.delete()
            }

            val safeName = (release.apkFileName ?: "update-${release.versionName}.apk")
                .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
            val targetFile = File(updatesDir, safeName)

            var currentUrl = downloadUrl
            var conn: HttpURLConnection? = null
            for (redirect in 0..5) {
                val next = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 60_000
                    setRequestProperty("Accept", "application/octet-stream")
                    if (URL(currentUrl).protocol == "https" && URL(currentUrl).host.equals("api.github.com", ignoreCase = true) && BuildConfig.GITHUB_TOKEN.isNotBlank()) {
                        setRequestProperty("Authorization", "Bearer ${BuildConfig.GITHUB_TOKEN}")
                    }
                    setRequestProperty("User-Agent", "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}")
                }
                val code = next.responseCode
                if (code in 300..399) {
                    val location = next.getHeaderField("Location")
                    next.disconnect()
                    if (location.isNullOrBlank()) error("리다이렉트 위치가 비어 있습니다.")
                    val redirectUrl = URL(URL(currentUrl), location)
                    require(redirectUrl.protocol == "https") { "안전하지 않은 다운로드 주소입니다." }
                    currentUrl = redirectUrl.toString()
                } else {
                    conn = next
                    break
                }
            }
            val activeConn = conn ?: error("리다이렉트 횟수를 초과했습니다.")
            try {
                if (activeConn.responseCode !in 200..299) {
                    error("APK 다운로드 실패 (HTTP ${activeConn.responseCode})")
                }
                val totalBytes = activeConn.contentLengthLong.takeIf { it > 0L } ?: release.apkSizeBytes
                var downloadedBytes = 0L
                var lastProgress = -1
                activeConn.inputStream.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            if (totalBytes > 0L) {
                                val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                if (pct != lastProgress) {
                                    lastProgress = pct
                                    withContext(Dispatchers.Main) { onProgress(pct) }
                                }
                            }
                        }
                        output.flush()
                    }
                }
            } finally {
                activeConn.disconnect()
            }
            if (!targetFile.exists() || targetFile.length() == 0L) {
                error("다운로드된 APK 파일이 비어 있습니다.")
            }
            withContext(Dispatchers.Main) { onProgress(100) }
            targetFile
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun buildUnknownSourcesSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri()
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun launchApkInstall(context: Context, apkFile: File): Result<Unit> = runCatching {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
fun AppUpdateEffect(repoSlug: String = AppUpdater.DEFAULT_GITHUB_REPO) {
    AppUpdateController(repoSlug = repoSlug, autoCheck = true, showCard = false)
}

@Composable
fun AppUpdateCard(
    repoSlug: String = AppUpdater.DEFAULT_GITHUB_REPO,
    autoCheck: Boolean = true
) {
    AppUpdateController(repoSlug = repoSlug, autoCheck = autoCheck, showCard = true)
}

@Composable
private fun AppUpdateController(
    repoSlug: String,
    autoCheck: Boolean,
    showCard: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var availableUpdate by remember { mutableStateOf<AppUpdater.ReleaseInfo?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var pendingInstallFile by remember { mutableStateOf<File?>(null) }

    fun triggerInstall(file: File) {
        if (!AppUpdater.canRequestPackageInstalls(context)) {
            pendingInstallFile = file
            errorText = "앱 설치 권한을 허용한 뒤 돌아오면 설치가 자동으로 진행됩니다."
            runCatching {
                context.startActivity(AppUpdater.buildUnknownSourcesSettingsIntent(context))
            }.onFailure {
                errorText = "설치 권한 설정 화면을 열 수 없습니다: ${it.message}"
            }
            return
        }
        AppUpdater.launchApkInstall(context, file).onSuccess {
            pendingInstallFile = null
            errorText = null
        }.onFailure { err ->
            errorText = "설치 화면을 열지 못했습니다: ${err.message}"
        }
    }

    fun checkNow(showFeedback: Boolean) {
        if (isChecking) return
        isChecking = true
        errorText = null
        scope.launch {
            AppUpdater.checkLatestRelease(repoSlug = repoSlug).onSuccess { result ->
                when (result) {
                    is AppUpdater.UpdateCheckResult.UpdateAvailable -> {
                        availableUpdate = result.release
                        statusText = "새 버전 ${result.release.tagName} 사용 가능"
                    }
                    is AppUpdater.UpdateCheckResult.UpToDate -> {
                        statusText = "최신 버전입니다 (${result.release.tagName})"
                        if (showFeedback) {
                            Toast.makeText(context, "현재 최신 버전입니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    AppUpdater.UpdateCheckResult.NoRelease -> {
                        statusText = "게시된 릴리즈가 없습니다."
                        if (showFeedback) {
                            Toast.makeText(context, "게시된 릴리즈가 아직 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }.onFailure { err ->
                statusText = "업데이트 확인 실패: ${err.message ?: "네트워크 오류"}"
                if (showFeedback) {
                    Toast.makeText(context, "업데이트 확인에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            }
            isChecking = false
        }
    }

    LaunchedEffect(autoCheck) {
        if (autoCheck) {
            checkNow(showFeedback = false)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                pendingInstallFile != null &&
                AppUpdater.canRequestPackageInstalls(context)
            ) {
                pendingInstallFile?.let { file ->
                    if (file.exists()) triggerInstall(file) else pendingInstallFile = null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showCard) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("앱 업데이트", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "현재 버전 v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                statusText?.let {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
                OutlinedButton(
                    onClick = { checkNow(showFeedback = true) },
                    enabled = !isChecking,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isChecking) "업데이트 확인 중…" else "업데이트 확인")
                }
            }
        }
    }

    availableUpdate?.let { release ->
        val sizeMb = if (release.apkSizeBytes > 0L) {
            String.format(Locale.US, "%.1f MB", release.apkSizeBytes / (1024.0 * 1024.0))
        } else {
            null
        }
        AlertDialog(
            onDismissRequest = {
                if (!isDownloading) {
                    availableUpdate = null
                    errorText = null
                }
            },
            title = { Text("새 버전 업데이트 (${release.tagName})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "현재 버전 v${BuildConfig.VERSION_NAME} -> 최신 버전 ${release.tagName}" +
                            (sizeMb?.let { " ($it)" } ?: ""),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (release.notes.isNotBlank()) {
                        Text(release.notes.take(600), fontSize = 12.sp, color = Color.Gray)
                    }
                    if (isDownloading) {
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0, 100) / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("다운로드 중… $progress%", fontSize = 12.sp, color = Color.Gray)
                    }
                    errorText?.let {
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                if (release.apkDownloadUrl != null) {
                    Button(
                        onClick = {
                            if (isDownloading) return@Button
                            isDownloading = true
                            progress = 0
                            errorText = null
                            scope.launch {
                                AppUpdater.downloadApk(context, release) { pct -> progress = pct }
                                    .onSuccess { file ->
                                        isDownloading = false
                                        triggerInstall(file)
                                    }
                                    .onFailure { err ->
                                        isDownloading = false
                                        errorText = err.message ?: "다운로드 중 오류가 발생했습니다."
                                    }
                            }
                        },
                        enabled = !isDownloading
                    ) {
                        Text(if (isDownloading) "다운로드 중…" else "다운로드 및 설치")
                    }
                } else if (release.htmlUrl.isNotBlank()) {
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, release.htmlUrl.toUri()))
                    }) {
                        Text("릴리즈 페이지 열기")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        availableUpdate = null
                        errorText = null
                    },
                    enabled = !isDownloading
                ) {
                    Text("나중에")
                }
            }
        )
    }
}
