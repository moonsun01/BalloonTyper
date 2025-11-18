package com.balloon.ui;

import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;
import com.balloon.core.Showable;

import com.balloon.ranking.RankingCsvRepository;
import com.balloon.ranking.RankingRecord;
import com.balloon.ranking.RankingTableModel;

import com.balloon.ui.assets.ImageAssets;
import com.balloon.ui.hud.HUDRenderer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.Adjustable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 랭킹 화면 UI
 * - 배경: Ranking_.png
 * - 가운데 투명한 픽셀 카드 안에 테이블 표시
 */
public class RankingScreenUI extends JPanel implements Showable {

    private final ScreenRouter router;

    private JTable table;
    private RankingTableModel tableModel;

    private final Image background;

    // 마우스가 올라간 행(뷰 인덱스), 없으면 -1
    private int hoverRow = -1;

    public RankingScreenUI(ScreenRouter router) {
        this.router = router;
        this.background = ImageAssets.load("Ranking_.png");

        setLayout(new BorderLayout());
        setOpaque(false);

        buildUI();
        loadDataAndSort();
    }

    // 배경 그리기
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), this);
        }
    }

    private void buildUI() {
        // 상단 Back 버튼 레이어
        JPanel top = new JPanel(null);
        top.setOpaque(false);
        top.setPreferredSize(new Dimension(1280, 120));

        JButton back = new JButton("BACK");
        styleTransparent(back);
        back.setBounds(30, 15, 200, 80);
        back.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                router.show(ScreenId.START);
            }
        });
        top.add(back);

        add(top, BorderLayout.NORTH);

        // 가운데 카드 래퍼
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel card = new PixelCardPanel();
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(950, 520));
        card.setBorder(new EmptyBorder(20, 24, 22, 24));

        // 테이블 + 모델
        tableModel = new RankingTableModel(new ArrayList<>());
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        table.setRowHeight(36);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoCreateRowSorter(true);
        table.setOpaque(false);

        // 선택 색 없애기 (파란 테두리, 파란 배경 제거)
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(false);
        table.setSelectionBackground(new Color(0, 0, 0, 0));
        table.setSelectionForeground(table.getForeground());

        // 기본 폰트 (HUD 폰트 있으면 사용)
        final Font baseFont;
        Font tmp = table.getFont();
        try {
            if (HUDRenderer.HUD_FONT != null) {
                tmp = HUDRenderer.HUD_FONT.deriveFont(22f);
            }
        } catch (Throwable ignore) {
        }
        baseFont = tmp;
        table.setFont(baseFont);

        // 헤더 스타일 (흰색, 모두 대문자 텍스트)
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setOpaque(true);
        header.setBackground(Color.WHITE);
        header.setForeground(Color.BLACK);
        header.setBorder(BorderFactory.createMatteBorder(
                0, 0, 1, 0, new Color(180, 200, 230)));

        // 헤더 렌더러: 가운데 정렬, 글씨 약간 작게
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                super.getTableCellRendererComponent(tbl, value, isSelected, false, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
                setFont(baseFont.deriveFont(Font.BOLD, 20f));
                setBorder(BorderFactory.createMatteBorder(
                        0, 0, 1, 0, new Color(180, 200, 230)));
                return this;
            }
        };

        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setHeaderValue("RANK");
        colModel.getColumn(1).setHeaderValue("NAME");
        colModel.getColumn(2).setHeaderValue("SCORE");
        colModel.getColumn(3).setHeaderValue("ACCURACY");

        for (int i = 0; i < colModel.getColumnCount(); i++) {
            colModel.getColumn(i).setHeaderRenderer(headerRenderer);
        }

        // 컬럼 폭
        colModel.getColumn(0).setPreferredWidth(80);
        colModel.getColumn(1).setPreferredWidth(260);
        colModel.getColumn(2).setPreferredWidth(220);
        colModel.getColumn(3).setPreferredWidth(220);

        // 공통 하이라이트 색
        final Color highlight = new Color(255, 255, 200, 180);

        // 기본 가운데 정렬 렌더러 (hover 색 처리 포함)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                super.getTableCellRendererComponent(tbl, value, false, false, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(baseFont);
                setBorder(null);

                if (row == hoverRow) {
                    setOpaque(true);
                    setBackground(highlight);
                } else {
                    setOpaque(false);
                    setBackground(new Color(0, 0, 0, 0));
                }
                return this;
            }
        };

        // accuracy 전용 렌더러 (정수면 정수, 아니면 소수 둘째자리, 가운데 정렬 + hover)
        DefaultTableCellRenderer accuracyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                super.getTableCellRendererComponent(tbl, value, false, false, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(baseFont);
                setBorder(null);

                if (value instanceof Number) {
                    double d = ((Number) value).doubleValue();
                    long rounded = Math.round(d);
                    String text;
                    if (Math.abs(d - rounded) < 1e-9) {
                        text = String.valueOf(rounded);
                    } else {
                        text = String.format(Locale.US, "%.2f", d);
                    }
                    setText(text);
                }

                if (row == hoverRow) {
                    setOpaque(true);
                    setBackground(highlight);
                } else {
                    setOpaque(false);
                    setBackground(new Color(0, 0, 0, 0));
                }
                return this;
            }
        };

        // 렌더러 적용 (모든 컬럼 가운데, accuracy는 전용 렌더러)
        colModel.getColumn(0).setCellRenderer(centerRenderer);
        colModel.getColumn(1).setCellRenderer(centerRenderer);
        colModel.getColumn(2).setCellRenderer(centerRenderer);
        colModel.getColumn(3).setCellRenderer(accuracyRenderer);

        // 마우스 hover 로우 계산
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r != hoverRow) {
                    hoverRow = r;
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverRow != -1) {
                    hoverRow = -1;
                    table.repaint();
                }
            }
        });

        // 스크롤팬
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        // 커스텀 스크롤바 (픽셀 느낌)
        JScrollBar vBar = scroll.getVerticalScrollBar();
        vBar.setOpaque(false);
        vBar.setUnitIncrement(16);
        vBar.setUI(new PixelScrollBarUI());

        JScrollBar hBar = scroll.getHorizontalScrollBar();
        hBar.setOpaque(false);
        hBar.setUnitIncrement(16);
        hBar.setUI(new PixelScrollBarUI());

        card.add(scroll, BorderLayout.CENTER);
        centerWrapper.add(card);

        add(centerWrapper, BorderLayout.CENTER);
    }

    private void loadDataAndSort() {
        RankingCsvRepository repo = new RankingCsvRepository();
        List<RankingRecord> list = repo.loadAll();

        // 점수 ↓, 정확도 ↓, 남은 시간 ↓, 이름 ↑
        list.sort(
                Comparator.comparingInt(RankingRecord::getScore).reversed()
                        .thenComparingDouble(RankingRecord::getAccuracy).reversed()
                        .thenComparingInt(RankingRecord::getTimeLeft).reversed()
                        .thenComparing(RankingRecord::getName)
        );

        tableModel.setData(list);
    }

    @Override
    public void onShown() {
        loadDataAndSort();
    }

    @Override
    public void onHidden() {
    }

    // 투명 Back 버튼 스타일
    private void styleTransparent(JButton b) {
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setForeground(new Color(0, 0, 0, 0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * 가운데 카드 패널: 반투명 픽셀 테두리
     */
    private static class PixelCardPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 안쪽 밝은 틴트
            g2.setColor(new Color(255, 255, 255, 150));
            g2.fillRect(6, 6, w - 12, h - 12);

            // 바깥 회색 픽셀 테두리
            g2.setColor(new Color(200, 210, 230, 220));
            g2.drawRect(4, 4, w - 9, h - 9);
            g2.drawRect(5, 5, w - 11, h - 11);

            g2.dispose();
        }
    }

    /**
     * 스크롤바 UI (픽셀 느낌 + 살짝 투명)
     */
    private static class PixelScrollBarUI extends BasicScrollBarUI {

        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(180, 200, 230, 190);
            this.trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            Dimension d = super.getPreferredSize(c);
            if (scrollbar.getOrientation() == Adjustable.VERTICAL) {
                return new Dimension(14, d.height);
            } else {
                return new Dimension(d.width, 14);
            }
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }
    }
}
