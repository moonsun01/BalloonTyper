package balloon.app;

import javax.swing.*;

public class Launcher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Balloon Typer (WIP)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(960, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
