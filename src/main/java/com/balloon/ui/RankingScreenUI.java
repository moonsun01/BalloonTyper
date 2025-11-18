package com.balloon.ui;

import com.balloon.core.GameMode;
import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;
import com.balloon.core.Showable;
import com.balloon.data.CsvRankingRepository;
import com.balloon.data.ScoreEntry;
import com.balloon.ui.assets.ImageAssets;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * RankingScreenUI (기본 폰트 + 고정 배치 풍선)
 */
public class RankingScreenUI extends JPanel implements Showable {

    private final ScreenRouter router;

    private JTable table;
    private DefaultTableModel tableModel;

    // 풍선 고정 배치
    private BalloonIcon[] balloons;

    // ===================== 생성자 =====================
    public RankingScreenUI(ScreenRouter router) {
        this.router = router;
        buildUI();
        initBalloons();
        loadDataAndSort(null);
    }

    // ===================== UI 구성 =====================
    private void buildUI() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(180, 220, 255));

        // ----- 상단: 타이틀 + 뒤로가기 -----
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));

        JLabel title = new JLabel("RANKING");
        title.setForeground(new Color(40, 40, 60));
        title.setFont(title.getFont().deriveFont(Font.BOLD, 28f));
        top.add(title, BorderLayout.WEST);

        JButton back = new JButton("← 뒤로");
        back.addActionListener(e -> router.show(ScreenId.START));
        top.add(back, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // ----- 중앙 카드 -----
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int arc = 35;
                Shape rect = new RoundRectangle2D.Float(
                        0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

                g2.setColor(new Color(255, 255, 255, 235));
                g2.fill(rect);

                g2.setStroke(new BasicStroke(2f));
                g2.setColor(new Color(170, 190, 230));
                g2.draw(rect);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        card.setPreferredSize(new Dimension(680, 420));

        // 테이블 모델
        String[] cols = {"순위", "이름", "점수", "모드"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }

            @Override public Class<?> getColumnClass(int col) {
                switch (col) {
                    case 0: case 2: return Integer.class;
                    default: return String.class;
                }
            }
        };

        table = new JTable(tableModel);
        styleTable(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        card.add(scroll, BorderLayout.CENTER);
        centerWrapper.add(card);

        add(centerWrapper, BorderLayout.CENTER);
    }

    // ===================== 테이블 스타일 =====================
    private void styleTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.setOpaque(false);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);

        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(60);
        colModel.getColumn(1).setPreferredWidth(220);
        colModel.getColumn(2).setPreferredWidth(120);
        colModel.getColumn(3).setPreferredWidth(90);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        header.setBackground(new Color(230, 240, 255));
        header.setForeground(new Color(60, 60, 80));

        // 줄무늬 렌더러
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {

            private final Color odd = new Color(245, 250, 255);
            private final Color even = new Color(232, 242, 255);

            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object v, boolean sel, boolean f, int row, int col) {

                Component c = super.getTableCellRendererComponent(tbl, v, sel, f, row, col);

                if (!sel) {
                    c.setBackground((row % 2 == 0) ? even : odd);
                    c.setForeground(new Color(40, 40, 60));
                }

                if (c instanceof JLabel) {
                    JLabel label = (JLabel) c;
                    if (col == 1)
                        label.setHorizontalAlignment(SwingConstants.LEFT);
                    else
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                }
                return c;
            }
        });
    }

    // ===================== 데이터 로드 =====================
    private void loadDataAndSort(GameMode modeFilter) {
        CsvRankingRepository repo = new CsvRankingRepository();
        List<ScoreEntry> list = repo.loadAll();

        List<ScoreEntry> filtered = new ArrayList<ScoreEntry>();
        for (ScoreEntry e : list) {
            if (modeFilter == null || e.getMode() == modeFilter)
                filtered.add(e);
        }

        filtered.sort(
                Comparator.comparingInt(ScoreEntry::getScore).reversed()
                        .thenComparing(ScoreEntry::getName)
        );

        tableModel.setRowCount(0);
        int rank = 1;
        for (ScoreEntry e : filtered) {
            tableModel.addRow(new Object[]{
                    rank++, e.getName(), e.getScore(), e.getMode().name()
            });
        }

        TableRowSorter<TableModel> sorter = new TableRowSorter<TableModel>(table.getModel());
        sorter.setComparator(2, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                return ((Integer) o1).compareTo((Integer) o2);
            }
        });
        table.setRowSorter(sorter);

        List<SortKey> keys = new ArrayList<SortKey>();
        keys.add(new SortKey(2, SortOrder.DESCENDING));
        sorter.setSortKeys(keys);
    }

    // ===================== Showable =====================
    @Override
    public void onShown() { loadDataAndSort(null); }
    @Override
    public void onHidden() {}

    // ===================== 풍선 고정 배치 =====================
    private static class BalloonIcon {
        BufferedImage img;
        int x,y,w,h;
        BalloonIcon(BufferedImage img, int x, int y, int w, int h) {
            this.img = img; this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    private void initBalloons() {
        BufferedImage yellow = ImageAssets.load("balloon_yellow.png");
        BufferedImage pink   = ImageAssets.load("balloon_pink.png");
        BufferedImage green  = ImageAssets.load("balloon_green.png");
        BufferedImage orange = ImageAssets.load("balloon_orange.png");
        BufferedImage purple = ImageAssets.load("balloon_purple.png");

        balloons = new BalloonIcon[]{
                new BalloonIcon(yellow,  60, 80, 70, 70),
                new BalloonIcon(pink,   220,140, 60, 60),
                new BalloonIcon(green,  420, 70, 80, 80),
                new BalloonIcon(orange, 620,150, 65, 65),
                new BalloonIcon(purple, 140,220, 70, 70)
        };
    }

    // ===================== 배경 + 풍선 =====================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(new Color(180, 220, 255));
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (balloons != null) {
            for (BalloonIcon b : balloons) {
                if (b.img != null) {
                    g2.drawImage(b.img, b.x, b.y, b.w, b.h, this);
                }
            }
        }
    }
}
