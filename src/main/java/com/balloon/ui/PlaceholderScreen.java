//패키지 : UI 컴포넌트들이 모여 있는 공간 (네임스페이스)
package com.balloon.ui;

import javax.swing.*; //JPanel, JLabel 같은 스윙 컴포넌트를 쓰기 위해 import
import java.awt.*; //레이아웃/폰트/색상 등 awt 관련 클래스 사용

/**
 * PlaceholderScreen
 * - 라우팅 테스트용 임시 화면.
 * - 특정 제목 문자열을 받아 중앙에 크게 표시
 * - start/guide/ranking/game 같은 실제 화면을 만들기 전, 버튼 동작 검증에 사용
 */

public class PlaceholderScreen extends JPanel {

    /**
     * 생성자
     * @param title 이 화면에 중앙 표시할 텍스트 (예 : "GUIDE SCREEN")
     */
    public PlaceholderScreen(String title){
        //화면 전체 레이아웃 : GridBagLayout을 사용해서 중앙 정렬하기 쉬움
        setLayout(new GridBagLayout());

        //중앙에 보여줄 라벨 생성
        JLabel label = new JLabel(title);

        //글자를 조금 크기/굵게 : 기존 폰트 기반으로 굵게 + 28pt
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 28f));

        //라벨 생성(선택): 살짝 어두운 화면에서도 잘 보이도록
        label.setForeground(new Color(40,40,40));

        //패널 중앙에 라벨 추가(GridBagLayout 기본 제약으로 중앙 배치)
        add(label);
    }
}
