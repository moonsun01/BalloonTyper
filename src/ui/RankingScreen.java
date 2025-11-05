package ui;

import data.*;
import javax.swing.*;
import java.util.List;
//전체랭킹화면
public class RankingScreen extends JPanel {//JPanel: 화면을 담는 빈 상자{

    public RankingScreen(){

        //글을 여러 줄 보여줄 수 있는 텍스트 상자
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);//읽기전용, 사용자가 수정하지 못하도록
        add(new JScrollPane(textArea)); //JPanel 위에 스크롤 가능

        RankingRepository repo = new CsvRankingRepository();
        List<ScoreEntry> list = repo.loadAll();

        StringBuilder  builder = new StringBuilder(); //문자를 이어붙일 때 사용하는 도구
        for(ScoreEntry entry : list){//list안에 있는 여러개의 ScoreEntry(점수+이름)를 하나씩 꺼내서 화면에 표시할 문자열로 만들어줌
            builder.append(entry.getName())//저장된 닉네임
                    .append("  ")
                    .append(entry.getScore())//저장된 점수
                    .append("점\n");
        }
        textArea.setText(builder.toString());

}
}