package com.balloon.ui;

import com.balloon.core.ScreenId;
import com.balloon.core.Showable;
import com.balloon.core.ScreenRouter;

import com.balloon.ranking.RankingCsvRepository;
import com.balloon.ranking.RankingRecord;
import com.balloon.ranking.RankingTableModel;

import com.balloon.ui.assets.ImageAssets;
import com.balloon.ui.hud.HUDRenderer;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RankingScreenUI
 * - ranking.png 전체 이미지를 배경으로 쓰고,
 *   그 위에 rank / NAME / SCORE / accuracy 4개 컬럼만 표시.
 */
public class RankingScreenUI extends JPanel implements Showable {

    private final ScreenRouter router;

    private JTable table;
    private RankingTableModel tableModel;

    // 배경 이미지(ranking.png)
    private Image bgImage;

    public RankingScreenUI(ScreenRouter router) {
        this.router = router;

        // 배경 이미지 로드
        bgImage = ImageAssets.load("ranking.png");

        buildUI();
        loadDataAndSort();
    }

    // 패널 배경에 ranking.png 깔기
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g.setColor(new Color(180, 210, 230));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        setOpaque(false);

        // ===== 상단 : 오른쪽 위에 "뒤로" 버튼만 =====
        JPanel top = new JPanel(null);
        top.setOpaque(false);
        top.setPreferredSize(new Dimension(1280, 120));

        // ← GUIDE의 Back 투명 버튼과 같은 코드
        JButton back = new JButton("BACK"); // 접근성용 라벨 (실제로는 안 보이게)
        styleTransparent(back);             // ★ 아래에 새로 추가할 메서드 호출
        back.setBounds(28, 24, 160, 80);    // ★ GUIDE와 동일한 좌표/크기
        back.setBorder(new EmptyBorder(0, 0, 0, 0));
        back.addActionListener(e -> router.show(ScreenId.START));
        top.add(back);

        add(top, BorderLayout.NORTH);

        // ===== 테이블 생성(4 컬럼) =====
        tableModel = new RankingTableModel(List.of());
        table = new JTable(tableModel);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        table.setFillsViewportHeight(true);
        table.setRowHeight(32);
        table.setAutoCreateRowSorter(true);

        // ★ 여기 추가: 랭킹 셀에 쓸 폰트 (HUD 폰트에서 조금 키워서)
        Font cellFont = HUDRenderer.HUD_FONT.deriveFont(17f);


        // 헤더는 ranking.png 안에 이미 그려져 있으니 숨김
        table.setTableHeader(null);

        // 그리드 라인 제거 (이미지에 있는 선만 사용)
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        // 투명 처리: 배경 이미지를 그대로 보이게
        table.setOpaque(false);

        // 공통 렌더러(흰 글씨, 투명 배경)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setForeground(Color.WHITE);
                setFont(cellFont);          // ★ 게임 폰트 적용
                setOpaque(false);  // 배경 투명
                setBorder(new EmptyBorder(0, 10, 0, 10)); // ★ 좌우 padding 살짝
                return this;
            }
        };

        // 점수/정확도는 숫자니까 오른쪽 정렬로 약간 변경
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.RIGHT);
                setForeground(Color.WHITE);
                setFont(cellFont);          // ★ 동일 폰트
                setOpaque(false);
                setBorder(new EmptyBorder(0, 0, 0, 18));  // 오른쪽 여백 조금
                return this;
            }
        };

        TableColumnModel cols = table.getColumnModel();
        // 컬럼 폭은 ranking.png 안의 표에 맞게 대략 조정 (필요하면 수치 조금씩 수정)
        cols.getColumn(0).setPreferredWidth(200);   // rank
        cols.getColumn(1).setPreferredWidth(200);  // NAME
        cols.getColumn(2).setPreferredWidth(225);  // SCORE
        cols.getColumn(3).setPreferredWidth(220);  // accuracy

        cols.getColumn(0).setCellRenderer(centerRenderer);
        cols.getColumn(1).setCellRenderer(centerRenderer);
        cols.getColumn(2).setCellRenderer(centerRenderer);
        cols.getColumn(3).setCellRenderer(centerRenderer);

        // 스크롤 패널도 투명 처리
        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);

        // ranking.png 안의 회색 큰 박스 위치에 맞게 여백 조정
        // 필요하면 값(위, 왼쪽, 아래, 오른쪽)을 조금씩 바꿔서 맞추면 돼.
        scroll.setBorder(BorderFactory.createEmptyBorder(130, 200, 120, 200));

        add(scroll, BorderLayout.CENTER);
    }

    private void loadDataAndSort() {
        RankingCsvRepository repo = new RankingCsvRepository();
        List<RankingRecord> list = repo.loadAll();

        // 점수↓ → 정확도↓ → 남은시간↓ → 이름↑ 정렬
        list.sort(Comparator
                .comparingInt(RankingRecord::getScore).reversed()
                .thenComparing(Comparator.comparingDouble(RankingRecord::getAccuracy).reversed())
                .thenComparing(Comparator.comparingInt(RankingRecord::getTimeLeft).reversed())
                .thenComparing(RankingRecord::getName)
        );

        tableModel.setData(list);

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        sorter.setComparator(0, Comparator.comparingInt(o -> (Integer) o));   // rank
        sorter.setComparator(2, Comparator.comparingInt(o -> (Integer) o));   // score
        sorter.setComparator(3, Comparator.comparingDouble(o -> (Double) o)); // accuracy
        table.setRowSorter(sorter);

        List<SortKey> keys = new ArrayList<>();
        keys.add(new SortKey(2, SortOrder.DESCENDING)); // SCORE
        keys.add(new SortKey(3, SortOrder.DESCENDING)); // accuracy
        sorter.setSortKeys(keys);
        sorter.sort();
    }

    @Override
    public void onShown() {
        loadDataAndSort();  // 매번 들어올 때 최신 CSV 다시 읽기
    }

    @Override
    public void onHidden() {
    }

    // ★ GUIDE 화면과 동일한 투명 버튼 스타일
    private void styleTransparent(JButton b) {
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setForeground(new Color(0, 0, 0, 0)); // 텍스트도 안 보이게
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}


