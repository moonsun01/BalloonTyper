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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * RankingScreenUI (귀여운 풍선 버전)
 * - CsvRankingRepository / ScoreEntry 기반 랭킹 화면.
 * - CSV 컬럼: name, score, mode
 * - 화면 컬럼: 순위, 이름, 점수, 모드
 * - 기본 정렬: 점수 내림차순.
 */
public class RankingScreenUI extends JPanel implements Showable {

    private final ScreenRouter router;

    private JTable table;
    private DefaultTableModel tableModel;

    // 배경/풍선
    private BufferedImage bgImage;
    private final List<BalloonIcon> balloons = new ArrayList<>();
    private final Random rnd = new Random();

    // ===================== 폰트 (ResultScreen 과 동일 컨셉) =====================
    private static Font roundedBase;

    static {
        try {
            // resources/assets/NanumHand.ttf 기준
            InputStream is =
                    RankingScreenUI.class.getResourceAsStream("/assets/NanumHand.ttf");
            if (is != null) {
                roundedBase = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .registerFont(roundedBase);
            }
        } catch (Exception e) {
            roundedBase = null; // 실패하면 기본 폰트 사용
        }
    }

    private static Font roundedFont(float size, int style) {
        Font base = roundedBase;
        if (base == null) {
            base = UIManager.getFont("Label.font");
            if (base == null) base = new Font("Dialog", Font.PLAIN, 12);
        }
        return base.deriveFont(style, size);
    }

    private static Font fontTitle() {
        return roundedFont(32f, Font.BOLD);
    }

    private static Font fontHeader() {
        return roundedFont(18f, Font.BOLD);
    }

    private static Font fontCell() {
        return roundedFont(15f, Font.PLAIN);
    }

    private static Font fontButton() {
        return roundedFont(15f, Font.PLAIN);
    }

    // ===================== 생성자 =====================
    public RankingScreenUI(ScreenRouter router) {
        this.router = router;
        buildUI();
        loadDataAndSort(null); // 전체 모드
    }

    // ===================== UI 구성 =====================
    private void buildUI() {
        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(new Color(180, 220, 255)); // 하늘색 기본

        // ----- 상단: 타이틀 + 뒤로가기 -----
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));

        JLabel title = new JLabel("RANKING");
        title.setForeground(new Color(40, 40, 60));
        title.setFont(fontTitle());
        top.add(title, BorderLayout.WEST);

        JButton back = createPastelButton("← 뒤로", new Color(200, 210, 255));
        back.addActionListener(e -> router.show(ScreenId.START));
        top.add(back, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // ----- 중앙: 둥근 카드 + 테이블 -----
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

                g2.setColor(new Color(255, 255, 255, 235)); // 살짝 투명한 흰색
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

        // 테이블 모델: 순위, 이름, 점수, 모드
        String[] cols = {"순위", "이름", "점수", "모드"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 편집 불가
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 0, 2 -> Integer.class;   // 순위, 점수
                    default -> String.class;      // 이름, 모드
                };
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

    // 테이블 스타일 예쁘게
    private void styleTable(JTable table) {
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setAutoCreateRowSorter(true);
        table.setOpaque(false);
        table.setFont(fontCell());
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);

        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(60);   // 순위
        colModel.getColumn(1).setPreferredWidth(200);  // 이름
        colModel.getColumn(2).setPreferredWidth(120);  // 점수
        colModel.getColumn(3).setPreferredWidth(90);   // 모드

        // 헤더 스타일
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setForeground(new Color(60, 60, 80));
        header.setBackground(new Color(230, 240, 255));
        header.setFont(fontHeader());

        // 셀 렌더러 (지브라 + 정렬)
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color odd = new Color(245, 250, 255);
            private final Color even = new Color(232, 242, 255);

            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, col);
                c.setFont(fontCell());

                if (!isSelected) {
                    c.setBackground((row % 2 == 0) ? even : odd);
                    c.setForeground(new Color(40, 40, 60));
                } else {
                    c.setBackground(new Color(200, 220, 255));
                    c.setForeground(new Color(20, 20, 40));
                }

                // 정렬: 순위/점수/모드 = 가운데, 이름 = 왼쪽
                if (c instanceof JLabel label) {
                    if (col == 1) {
                        label.setHorizontalAlignment(SwingConstants.LEFT);
                    } else {
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                    }
                }
                return c;
            }
        });
    }

    // 파스텔 버튼
    private JButton createPastelButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(fontButton());
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));
        btn.setForeground(new Color(40, 40, 60));

        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();
                int arc = 22;

                g2.setColor(bg);
                g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

                g2.setColor(new Color(160, 170, 190));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

                super.paint(g2, c);
                g2.dispose();
            }
        });

        return btn;
    }

    // ===================== 데이터 로드 =====================

    /**
     * CSV를 읽고 점수 내림차순 정렬 후 테이블에 채운다.
     *
     * @param modeFilter null 이면 전체, 아니면 해당 모드만(SINGLE / VERSUS)
     */
    private void loadDataAndSort(GameMode modeFilter) {
        CsvRankingRepository repo = new CsvRankingRepository();
        List<ScoreEntry> list = repo.loadAll();

        // 모드 필터
        List<ScoreEntry> filtered = new ArrayList<>();
        for (ScoreEntry e : list) {
            if (modeFilter == null || e.getMode() == modeFilter) {
                filtered.add(e);
            }
        }

        // 점수 내림차순, 이름 오름차순
        filtered.sort(
                Comparator.comparingInt(ScoreEntry::getScore).reversed()
                        .thenComparing(ScoreEntry::getName)
        );

        // 테이블 데이터 갱신
        tableModel.setRowCount(0);
        int rank = 1;
        for (ScoreEntry e : filtered) {
            Object[] row = {
                    rank++,
                    e.getName(),
                    e.getScore(),
                    e.getMode().name()
            };
            tableModel.addRow(row);
        }

        // RowSorter 기본 정렬: 점수(2) ↓
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        sorter.setComparator(2, Comparator.comparingInt(o -> (Integer) o)); // 점수 컬럼
        table.setRowSorter(sorter);

        List<SortKey> keys = new ArrayList<>();
        keys.add(new SortKey(2, SortOrder.DESCENDING));
        sorter.setSortKeys(keys);
        sorter.sort();
    }

    // ===================== Showable 구현 =====================

    @Override
    public void onShown() {
        // 화면 들어올 때마다 최신 CSV 다시 읽기
        loadDataAndSort(null);
    }

    @Override
    public void onHidden() {
        // 특별히 할 일 없음
    }

    // ===================== 배경 + 풍선 =====================

    private static class BalloonIcon {
        BufferedImage img;
        int x, y, w, h;

        BalloonIcon(BufferedImage img, int x, int y, int w, int h) {
            this.img = img;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    // 패널 크기에 맞춰 풍선 랜덤 배치(한 번만 생성)
    private void ensureBalloonsLayout(int width, int height) {
        if (!balloons.isEmpty() || width <= 0 || height <= 0) return;

        BufferedImage[] imgs = new BufferedImage[]{
                ImageAssets.load("balloon_yellow.png"),
                ImageAssets.load("balloon_pink.png"),
                ImageAssets.load("balloon_purple.png"),
                ImageAssets.load("balloon_green.png"),
                ImageAssets.load("balloon_orange.png")
        };

        int count = 8;      // 귀엽게 여러 개
        int minSize = 40;   // 작게
        int maxSize = 70;

        for (int i = 0; i < count; i++) {
            BufferedImage img = imgs[rnd.nextInt(imgs.length)];
            if (img == null) continue;

            int size = minSize + rnd.nextInt(maxSize - minSize + 1);
            int x = rnd.nextInt(Math.max(1, width - size - 40)) + 20;
            int y = rnd.nextInt(Math.max(1, height - size - 80)) + 20;

            balloons.add(new BalloonIcon(img, x, y, size, size));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 하늘 배경
        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, w, h, this);
        } else {
            g2.setColor(new Color(180, 220, 255));
            g2.fillRect(0, 0, w, h);
        }

        // 풍선 배치
        ensureBalloonsLayout(w, h);

        for (BalloonIcon b : balloons) {
            if (b.img != null) {
                g2.drawImage(b.img, b.x, b.y, b.w, b.h, this);
            }
        }

        g2.dispose();
    }
}
