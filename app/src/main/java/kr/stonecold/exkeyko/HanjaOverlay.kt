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
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * 한자 후보를 오버레이로 표시하기 위한 View 클래스
 * @param context Context
 */
class HanjaOverlay(private val context: Context) : Application.ActivityLifecycleCallbacks {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    /**
     * Init
     */
    init {
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(this)
        Log.d("HanjaOverlay", "ActivityLifecycleCallbacks registered")
    }

    /**
     * 후보 선택시 이벤트 처리를 위해 호출되는 리스너
     */
    var onCandidateSelectedListener: ((String) -> Unit)? = null

    /**
     * 한자 맵를 화면에 표시하는 메서드
     * @param hanjaText 원본 문자
     * @param hanjaMap 한자 맵
     * @param direction Overlay 방향 (v: 세로, h: 가로)
     */
    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    fun showHanjaOverlay(
        hanjaText: String,
        hanjaMap: HashMap<String, String>,
        direction: String = "v"
    ) {
        // 기존 오버레이가 있으면 제거
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
            Log.d("HanjaOverlay", "Existing overlay removed")
        }

        // 방향에 따라 세로 스크롤 또는 가로 스크롤 생성
        val scrollView = if (direction == "v") {
            ScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
            } // 세로 스크롤
        } else {
            HorizontalScrollView(context).apply {
                isVerticalScrollBarEnabled = false
            } // 가로 스크롤
        }

        val isChoseong = hanjaText.length == 1 && hanjaText[0] in 'ㄱ'..'ㅎ'

        val candidatesContainer = LinearLayout(context).apply {
            orientation = if (direction == "v") LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val buttonList = mutableListOf<LinearLayout>()

        // 한자 후보를 생성하여 리스트에 저장
        hanjaMap.forEach { (key, comment) ->
            // 각각의 key와 comment를 담는 레이아웃 생성
            val candidateLayout = LinearLayout(context).apply {
                orientation = if (direction == "v") LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 8, 16, 8)
                }

                // 처음엔 투명하게 설정 (크기 맞춘 후 색상 적용)
                setBackgroundColor(Color.TRANSPARENT)
            }

            // key 표시 TextView
            val keyTextView = TextView(context).apply {
                text = key
                textSize = 16f // 한자 텍스트 크기
                setTextColor(Color.WHITE) // 흰색 글자
                if (!isChoseong) {
                    setPadding(32, 8, 32, 8) // 좌, 상, 우, 하 패딩 설정
                } else {
                    setPadding(32, 32, 32, 32) // 좌, 상, 우, 하 패딩 설정
                }

                // 가로일 때만 가운데 정렬
                if (direction == "h") {
                    gravity = Gravity.CENTER
                }
            }

            // comment 표시 TextView
            val commentTextView = TextView(context).apply {
                text = if (comment.isNotEmpty()) comment else if (isChoseong) "" else hanjaText
                textSize = 12f // 뜻 텍스트 크기
                setTextColor(Color.LTGRAY) // 밝은 회색 글자
                setPadding(16, 8, 16, 8) // 좌, 상, 우, 하 패딩 설정

                // 가로일 때만 가운데 정렬
                if (direction == "h") {
                    gravity = Gravity.CENTER
                }

                // 초성이면 숨김
                if (isChoseong) {
                    visibility = View.GONE
                }
            }

            // 레이아웃에 TextView 추가
            if (direction == "v") {
                // 세로일 때 좌우 배치 및 공백 추가
                candidateLayout.addView(keyTextView)
                candidateLayout.addView(commentTextView)
            } else {
                // 가로일 때 위아래 배치
                candidateLayout.addView(keyTextView)
                candidateLayout.addView(commentTextView)
            }

            // 클릭 이벤트 추가
            candidateLayout.setOnClickListener {
                Log.d("HanjaOverlay", "Candidate '$key' with comment '$comment' selected")
                dismissOverlay()
                onCandidateSelectedListener?.invoke(key)
            }

            // 리스트에 추가
            buttonList.add(candidateLayout)
            candidatesContainer.addView(candidateLayout)
        }

        // ScrollView에 후보 컨테이너 추가
        scrollView.addView(candidatesContainer)

        // 오버레이에 추가할 레이아웃 정의
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, // 너비를 wrap_content로 설정
            WindowManager.LayoutParams.WRAP_CONTENT, // 높이를 wrap_content로 설정
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        )

        // 방향에 따른 오버레이 위치 설정
        params.gravity = if (direction == "v") {
            Gravity.TOP or Gravity.START // 좌상단
        } else {
            Gravity.BOTTOM or Gravity.START // 좌하단
        }

        params.dimAmount = 0.5f // 배경 어두운 정도 설정 (50% 어두움)

        // 오버레이 추가
        overlayView = scrollView
        windowManager.addView(scrollView, params)
        Log.d("HanjaOverlay", "Overlay added to window")

        // 오버레이 외부 영역을 클릭하면 오버레이를 닫는 리스너 설정
        scrollView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                dismissOverlay() // 오버레이 닫기
                return@setOnTouchListener true
            }
            false
        }

        // 가로 출력시 스크롤
        if (direction == "h") {
            scrollView.setOnGenericMotionListener { _, event ->
                if (event.action == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_CLASS_POINTER)) {
                    val deltaX = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    scrollView.scrollBy((deltaX * 100).toInt(), 0) // 스크롤 값을 조절하여 스크롤 속도 조정
                    true
                } else {
                    false
                }
            }
        }

        // ViewTreeObserver를 사용하여 뷰가 렌더링된 후에 버튼 크기를 통일
        val viewTreeObserver = candidatesContainer.viewTreeObserver
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (direction == "v") {
                    // 세로 방향일 때 버튼 크기를 통일
                    val maxWidth = buttonList.maxOfOrNull { it.measuredWidth } ?: 0
                    val maxHeight = buttonList.maxOfOrNull { it.measuredHeight } ?: 0

                    buttonList.forEach { button ->
                        button.layoutParams = LinearLayout.LayoutParams(maxWidth, maxHeight).apply {
                            setMargins(32, 8, 8, 8) // 좌측 여백
                        }
                        // 크기 설정 후 배경 색상 변경
                        button.setBackgroundColor(0xE6333333.toInt()) // 어두운 회색 배경색 설정
                    }
                } else {
                    // 가로 방향일 때 버튼 배경색을 어두운 회색으로 설정
                    buttonList.forEach { button ->
                        button.setBackgroundColor(0xE6333333.toInt()) // 어두운 회색 배경색 설정
                    }
                }
                // 리스너 제거하여 메모리 누수 방지
                candidatesContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
                Log.d("HanjaOverlay", "Button sizes adjusted")
            }
        })
    }

    /**
     * 오버레이를 제거하는 메서드
     */
    fun dismissOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView)
            overlayView = null
            Log.d("HanjaOverlay", "Overlay dismissed")
        }
    }

    /**
     * onActivityPaused
     * 오버레이 제거를 위해 override
     * @param activity activity
     */
    override fun onActivityPaused(activity: Activity) {
        dismissOverlay()
    }

    /**
     * onActivityStopped
     * 오버레이 제거를 위해 override
     * @param activity activity
     */
    override fun onActivityStopped(activity: Activity) {
        dismissOverlay()
    }

    // 아래의 메서드들은 필요하지만 구현할 내용이 없으므로 빈 몸체로 둡니다.
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
