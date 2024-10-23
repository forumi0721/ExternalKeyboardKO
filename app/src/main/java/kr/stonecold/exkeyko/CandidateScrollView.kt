package kr.stonecold.exkeyko

import android.content.Context
import android.util.Log
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

/**
 * 안드로이드 기본 기능인 후보뷰을 이횽하여 한자/심볼 선택을 띄워주는 View 클래스
 * @param context Context
 */
class CandidateScrollView(context: Context) : HorizontalScrollView(context) {

    /**
     * 후보키를 표히할 Container
     */
    private val candidatesContainer: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Init
     */
    init {
        // 스크롤뷰에 LinearLayout을 추가
        addView(candidatesContainer)
    }

    /**
     * 후보 선택시 이벤트 처리를 위해 호출되는 리스너
     */
    var onCandidateSelectedListener: ((String) -> Unit)? = null

    /**
     * 후보키를 추가하는 메서드
     * @param hanjaText 원본 문자
     * @param hanjaMap 화면 표시를 위한 한자와 Comment를 담은 Map
     */
    fun addCards(hanjaText: String, hanjaMap: HashMap<String, String>?) {
        // 기존에 추가된 모든 후보 버튼 제거
        candidatesContainer.removeAllViews()

        // 한자 맵이 null일 경우 작업 종료
        if (hanjaMap == null) {
            Log.w("CandidateScrollView", "Hanja map is null, no candidates to display")
            return
        }

        // UI 스레드에서 실행하기 위해 post 사용
        post {
            val candidateButtons = mutableListOf<Button>()
            val isChoseong = hanjaText.length == 1 && hanjaText[0] in 'ㄱ'..'ㅎ'

            // 한자 맵을 순회하며 각 한자와 설명으로 버튼을 생성
            hanjaMap.forEach { (key, desc) ->
                val buttonText = when {
                    desc.isNotEmpty() -> "$key : $desc"
                    isChoseong -> key
                    else -> "$key : $hanjaText"
                }

                val candidateButton = Button(context).apply {
                    text = buttonText
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        // 후보가 선택되었을 때 리스너 호출
                        onCandidateSelectedListener?.invoke(key)
                        Log.d("CandidateScrollView", "Candidate selected: key='$key', desc='$desc'")
                    }
                }
                candidateButtons.add(candidateButton)
            }

            // 생성된 버튼들을 LinearLayout에 추가
            candidateButtons.forEach { button ->
                candidatesContainer.addView(button)
            }

            // 처음 후보로 스크롤 이동
            scrollTo(0, 0)

            Log.d("CandidateScrollView", "Added ${candidateButtons.size} candidate buttons")
        }
    }
}
