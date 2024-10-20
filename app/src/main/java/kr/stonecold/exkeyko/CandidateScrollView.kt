package kr.stonecold.exkeyko

import android.content.Context
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout

class CandidateScrollView(context: Context) : HorizontalScrollView(context) {

    // 후보 버튼들을 담을 LinearLayout
    private val candidatesContainer: LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    // 후보가 선택되었을 때 호출되는 리스너
    var onCandidateSelectedListener: ((String) -> Unit)? = null

    init {
        // 스크롤뷰에 LinearLayout을 추가
        addView(candidatesContainer)
    }

    /**
     * 한자 후보를 추가하는 함수
     * @param hanjaMap: 한자와 그 설명을 담은 맵
     */
    fun addCards(hanjaMap: HashMap<String, String>?) {
        // 기존에 추가된 모든 후보 버튼 제거
        candidatesContainer.removeAllViews()

        // 한자 맵이 null일 경우 작업 종료
        if (hanjaMap == null) {
            Log.w("CandidateScrollView", "Hanja map is null, no candidates to display")
            return
        }

        // UI 스레드에서 실행하도록 post 사용
        post {
            val candidateButtons = mutableListOf<Button>()

            // 한자 맵을 순회하며 각 한자와 설명으로 버튼을 생성
            hanjaMap.forEach { (key, desc) ->
                val candidateButton = Button(context).apply {
                    text = desc
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setOnClickListener {
                        // 후보가 선택되었을 때 리스너 호출
                        onCandidateSelectedListener?.invoke(key)
                        Log.d("CandidateScrollView", "Candidate '$desc' with key '$key' selected")
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
