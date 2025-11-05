package com.balloon.ui;

import com.balloon.core.ScreenId;
import com.balloon.core.Showable;            // 화면 생명주기(onShown/onHidden)를 위한 인터페이스(수업에서 만든 것)
import com.balloon.core.ScreenRouter;        // 화면 전환 라우터(수업에서 만든 것)
import com.balloon.ranking.RankingCsvRepository;
import com.balloon.ranking.RankingRecord;
import com.balloon.ranking.RankingTableModel;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RankingScreenUI
 * - CSV에서 랭킹 데이터를 읽어 JTable로 표시하는 화면.
 * - 기본 정렬: 점수 내림차순(동점 시 정확도 ↓, 남은시간 ↓, 이름 ↑).
 * - 스타일: 다크 테마 배경, 헤더 스타일, 지브라(줄무늬) 행, 숫자 우측 정렬 등.
 */
public class RankingScreenUI extends JPanel implements Showable {

    // 라우터: "뒤로가기" 버튼으로 시작 화면으로 돌아가기 위해 필요
    private final ScreenRouter router;

    // 테이블 구성 요소들
    private JTable table;
    private RankingTableModel tableModel;

    /**
     * 생성자: 라우터를 주입받아 보관하고, UI를 구성한 뒤 데이터를 로드한다.
     */
    public RankingScreenUI(ScreenRouter router) {
        this.router = router;
        buildUI();          // 화면 구성(레이아웃/컴포넌트/스타일)
        loadDataAndSort();  // CSV 로드 + 기본 정렬 지정
    }

    /**
     * 화면 전체 UI 구성
     * - 상단 바(제목/뒤로 버튼) + 중앙 스크롤 테이블 구조
     * - 테이블 렌더러/헤더/컬럼 폭/정렬 방향 등 스타일 지정
     */
    private void buildUI() {
        // 전체 레이아웃은 BorderLayout: 위(북) 타이틀, 센터 테이블
        setLayout(new BorderLayout());
        // 다크 배경(프로젝트 Theme가 있으면 거기로 치환 가능)
        setBackground(new Color(18, 19, 21));

        // ===== 상단 바(제목 + 뒤로가기 버튼) =====
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
        top.setBackground(getBackground()); // 부모 배경색과 동일

        JLabel title = new JLabel("RANKING");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        top.add(title, BorderLayout.WEST);

        JButton back = new JButton("← 뒤로");
        // 주의: "START_MENU"는 네 프로젝트의 실제 키/상수로 맞춰야 함(ScreenId.START_MENU 등)
        back.addActionListener(e -> router.show(ScreenId.START));
        top.add(back, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // ===== 테이블/모델 생성 =====
        tableModel = new RankingTableModel(List.of()); // 일단 빈 데이터로 생성
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true); // 테이블 높이가 부족해도 배경이 꽉 차 보이게
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true); // 헤더 클릭으로 정렬 토글 가능

        // ===== 컬럼 폭 & 정렬 방향(렌더러) 설정 =====
        TableColumnModel cols = table.getColumnModel();
        cols.getColumn(0).setPreferredWidth(60);   // 순위
        cols.getColumn(1).setPreferredWidth(160);  // 이름
        cols.getColumn(2).setPreferredWidth(120);  // 점수
        cols.getColumn(3).setPreferredWidth(120);  // 정확도
        cols.getColumn(4).setPreferredWidth(120);  // 남은시간
        cols.getColumn(5).setPreferredWidth(220);  // 플레이시각

        // 기본 렌더러: 좌/중앙/우측 정렬 용도 분리
        DefaultTableCellRenderer left = new DefaultTableCellRenderer();
        left.setHorizontalAlignment(SwingConstants.LEFT);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);

        // 각 컬럼에 렌더러 배치(숫자는 우측, 문자열은 좌/중앙로 보기 좋게)
        cols.getColumn(0).setCellRenderer(center); // 순위
        cols.getColumn(1).setCellRenderer(left);   // 이름
        cols.getColumn(2).setCellRenderer(right);  // 점수
        cols.getColumn(3).setCellRenderer(right);  // 정확도
        cols.getColumn(4).setCellRenderer(right);  // 남은시간
        cols.getColumn(5).setCellRenderer(center); // 플레이시각

        // 정확도 소수자리 포맷을 원하면 전용 렌더러 예시(주석 해제하여 사용)
        /*
        DefaultTableCellRenderer accuracyRenderer = new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                if (value instanceof Double d) {
                    super.setValue(String.format("%.1f", d)); // 소수점 1자리
                } else {
                    super.setValue(value);
                }
            }
        };
        accuracyRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        cols.getColumn(3).setCellRenderer(accuracyRenderer);
        */

        // ===== 헤더 스타일 =====
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false); // 드래그로 칼럼 순서 바꾸기 금지
        header.setForeground(Color.WHITE);
        header.setBackground(new Color(40, 42, 45));
        header.setFont(header.getFont().deriveFont(Font.BOLD));

        // ===== 지브라(줄무늬) 행 배경 렌더러 =====
        // Object.class 기본 렌더러를 커스터마이즈해서 교차 배경 처리
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color odd = new Color(30, 31, 34);  // 홀수 행 배경
            private final Color even = new Color(24, 25, 28); // 짝수 행 배경

            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    // 선택되지 않은 경우 교차 배경 + 글자색 흰색
                    c.setBackground((row % 2 == 0) ? even : odd);
                    c.setForeground(Color.WHITE);
                } else {
                    // 선택된 행은 강조 색상
                    c.setBackground(new Color(64, 86, 124));
                    c.setForeground(Color.WHITE);
                }
                return c;
            }
        });

        // 스크롤에 테이블 얹고 여백만 살짝
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));
        add(scroll, BorderLayout.CENTER);
    }

    /**
     * CSV를 읽고, 기본 정렬 기준(점수 ↓, 정확도 ↓, 남은시간 ↓, 이름 ↑)을 적용한다.
     * - TableRowSorter를 사용해 헤더 클릭 토글 정렬도 자동 활성화한다.
     */
    private void loadDataAndSort() {
        // 1) CSV 로드(없으면 Repository가 자동으로 더미 생성)
        RankingCsvRepository repo = new RankingCsvRepository();
        List<RankingRecord> list = repo.loadAll();

        // 2) 코드 레벨 기본 정렬(점수↓ → 정확도↓ → 남은시간↓ → 이름↑)
        list.sort(Comparator
                .comparingInt(RankingRecord::getScore).reversed()
                .thenComparing(Comparator.comparingDouble(RankingRecord::getAccuracy).reversed())
                .thenComparing(Comparator.comparingInt(RankingRecord::getTimeLeft).reversed())
                .thenComparing(RankingRecord::getName)
        );

        // 3) 테이블에 데이터 반영
        tableModel.setData(list);

        // 4) 헤더 클릭 정렬을 위한 RowSorter 세팅(자료형에 맞춘 Comparator)
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        // 순위(0)는 화면 계산치지만, 정렬할 일은 거의 없으므로 기본 Comparator 유지해도 무방
        sorter.setComparator(2, Comparator.comparingInt(o -> (Integer) o));   // 점수: Integer
        sorter.setComparator(3, Comparator.comparingDouble(o -> (Double) o)); // 정확도: Double
        sorter.setComparator(4, Comparator.comparingInt(o -> (Integer) o));   // 남은시간: Integer
        // playedAt(5)는 문자열 비교(원하면 LocalDateTime 파서 Comparator로 교체 가능)
        table.setRowSorter(sorter);

        // 5) 화면 진입 시 기본 정렬 키: 점수(2) ↓, 정확도(3) ↓, 남은시간(4) ↓
        List<SortKey> keys = new ArrayList<>();
        keys.add(new SortKey(2, SortOrder.DESCENDING)); // 점수 내림차순
        keys.add(new SortKey(3, SortOrder.DESCENDING)); // 정확도 내림차순
        keys.add(new SortKey(4, SortOrder.DESCENDING)); // 남은시간 내림차순
        sorter.setSortKeys(keys);
        sorter.sort();
    }

    // ==== Showable 생명주기 ====

    @Override
    public void onShown() {
        // 화면으로 들어올 때마다 최신 CSV를 다시 읽어 반영(다른 화면에서 점수 저장 후 돌아올 때 대비)
        loadDataAndSort();
    }

    @Override
    public void onHidden() {
        // 특별히 해줄 일 없음(자원 정리 필요 시 여기서 처리 가능)
    }
}
