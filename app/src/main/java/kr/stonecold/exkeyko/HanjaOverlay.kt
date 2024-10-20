package kr.stonecold.exkeyko

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

class HanjaOverlay(private val context: Context) : Application.ActivityLifecycleCallbacks {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    init {
        // 앱 상태 변화를 감지하기 위해 리스너 등록
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
    }

    /**
     * 한자 후보를 오버레이로 추가하는 함수
     * @param hanjaMap: 한자와 그 설명을 담은 맵
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showHanjaOverlay(hanjaMap: HashMap<String, String>) {
        // 기존 오버레이가 있으면 제거
        if (overlayView != null) {
            windowManager.removeView(overlayView)
        }

        // HorizontalScrollView와 LinearLayout 생성
        val scrollView = HorizontalScrollView(context).apply {
            // 외부 터치를 감지하여 오버레이를 닫음
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_OUTSIDE) {
                    dismissOverlay() // 오버레이 닫기 함수 호출
                    return@setOnTouchListener true
                }
                false
            }
        }
        val candidatesContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 한자 후보 버튼을 생성하여 ScrollView에 추가
        hanjaMap.forEach { (key, desc) ->
            val candidateButton = Button(context).apply {
                text = desc
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 0, 8, 0) // 좌우 8dp 간격
                }

                // 패딩 추가 (left, top, right, bottom)
                setPadding(16, 8, 16, 8) // 각 값은 dp 단위로 변경 가능

                // 배경을 어둡게 설정 (예: 90% 불투명한 어두운 회색)
                setBackgroundColor(0xE6333333.toInt()) // 어두운 회색 (RGB: 51, 51, 51) + 90% 불투명

                // 글자 색을 밝게 설정 (예: 흰색)
                setTextColor(Color.WHITE) // 흰색 텍스트                // 버튼 클릭 시 동작

                setOnClickListener {
                    Log.d("HanjaOverlay", "Candidate '$desc' with key '$key' selected")
                    dismissOverlay()
                    onCandidateSelectedListener?.invoke(key)
                }
            }
            candidatesContainer.addView(candidateButton)
        }

        // ScrollView에 후보 컨테이너 추가
        scrollView.addView(candidatesContainer)

        // 오버레이에 추가할 레이아웃 정의
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or  // 포커스를 받지 않도록 설정
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or  // 외부 터치 감지
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        )

        // 오버레이의 위치를 화면 하단으로 설정
        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.dimAmount = 0.50f // 배경 어두운 정도 설정 (50% 어두움)

        // 오버레이 추가
        overlayView = scrollView
        windowManager.addView(scrollView, params)
    }

    // 오버레이 제거 함수
    fun dismissOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
        }
    }

    // 한자 선택 리스너
    var onCandidateSelectedListener: ((String) -> Unit)? = null

    // 앱이 백그라운드로 전환되거나 종료될 때 오버레이 제거
    override fun onActivityPaused(activity: Activity) {
        dismissOverlay()
    }

    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}

