package com.fairgig.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;

/** Hand-drawn (code-drawn) clay illustrations. Reference art only. */
public final class Illustrations {

    private Illustrations() {}

    private static LinearGradient linear(String from, String to) {
        return new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web(from)), new Stop(1, Color.web(to)));
    }

    private static DropShadow clayShadow(double radius, double y, String color, double alpha) {
        DropShadow ds = new DropShadow(radius, 0, y, Color.web(color, alpha));
        ds.setInput(new InnerShadow(8, -3, -3, Color.web("#FFFFFF", 0.85)));
        return ds;
    }

    private static Rectangle rect(double x, double y, double w, double h, double arc, javafx.scene.paint.Paint fill) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setArcWidth(arc);
        r.setArcHeight(arc);
        r.setFill(fill);
        return r;
    }

    private static StackPane coin(double radius) {
        Circle disc = new Circle(radius);
        disc.setFill(new RadialGradient(0, 0, 0.3, 0.25, 0.9, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#FDE68A")), new Stop(1, Color.web("#F59E0B"))));
        Circle ring = new Circle(radius * 0.76);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#FFFFFF", 0.65));
        ring.setStrokeWidth(2);
        Label symbol = new Label();
        symbol.textProperty().bind(CurrencyState.symbolBinding());
        symbol.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: " + (radius * 0.6) + "px;");
        StackPane coin = new StackPane(disc, ring, symbol);
        coin.setEffect(clayShadow(20, 10, "#B45309", 0.30));
        coin.setPrefSize(radius * 2, radius * 2);
        coin.setMaxSize(radius * 2, radius * 2);
        return coin;
    }

    /** Big hero for the login page: phone with a dashboard, floating coin, shield and receipt. */
    public static Pane loginHero() {
        Pane p = new Pane();
        p.setPrefSize(470, 380);
        p.setMinSize(470, 380);
        p.setMaxSize(470, 380);

        Circle blob = new Circle(235, 195, 160);
        blob.setFill(new RadialGradient(0, 0, 0.3, 0.25, 0.95, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#FFFFFF", 0.9)), new Stop(1, Color.web("#7DE3B5", 0.45))));
        blob.setEffect(new DropShadow(30, 0, 14, Color.web("#0F172A", 0.10)));
        Circle blueBlob = new Circle(392, 80, 48);
        blueBlob.setFill(new RadialGradient(0, 0, 0.3, 0.25, 0.95, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#FFFFFF", 0.8)), new Stop(1, Color.web("#93C5FD", 0.55))));
        Circle coralBlob = new Circle(64, 300, 36);
        coralBlob.setFill(new RadialGradient(0, 0, 0.3, 0.25, 0.95, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#FFFFFF", 0.8)), new Stop(1, Color.web("#FDA4AF", 0.6))));
        p.getChildren().addAll(blob, blueBlob, coralBlob);

        // phone body + screen
        Rectangle body = rect(165, 18, 140, 330, 44, linear("#FFFFFF", "#E1E8F4"));
        body.setEffect(clayShadow(28, 16, "#0F172A", 0.20));
        Rectangle screen = rect(175, 30, 120, 306, 34, Color.web("#F5F8FD"));
        Rectangle notch = rect(213, 40, 44, 8, 8, Color.web("#CBD5E1"));

        Rectangle balance = rect(184, 62, 102, 76, 20, linear("#34D399", "#0EA371"));
        balance.setEffect(new DropShadow(16, 0, 7, Color.web("#10B981", 0.40)));
        Label cap = new Label("NET EARNINGS");
        cap.setStyle("-fx-text-fill: rgba(255,255,255,0.85); -fx-font-size: 8px; -fx-font-weight: bold;");
        cap.setLayoutX(194);
        cap.setLayoutY(70);
        Label amount = new Label();
        amount.textProperty().bind(CurrencyState.money("48,250"));
        amount.setStyle("-fx-text-fill: white; -fx-font-size: 17px; -fx-font-weight: bold;");
        amount.setLayoutX(194);
        amount.setLayoutY(86);
        Rectangle delta = rect(194, 118, 46, 10, 10, Color.web("#FFFFFF", 0.38));

        Rectangle chartWell = rect(184, 150, 102, 128, 18, Color.web("#E6ECF7"));
        p.getChildren().addAll(body, screen, notch, balance, cap, amount, delta, chartWell);
        String[] barColors = {"#10B981", "#3B82F6", "#F59E0B", "#F43F5E", "#10B981"};
        double[] heights = {44, 70, 38, 88, 58};
        for (int i = 0; i < barColors.length; i++) {
            Rectangle bar = rect(194 + i * 18, 268 - heights[i], 12, heights[i], 8, Color.web(barColors[i]));
            p.getChildren().add(bar);
        }
        for (int i = 0; i < 3; i++) {
            p.getChildren().add(rect(186 + i * 34, 292, 28, 10, 10, Color.web("#CBD5E1")));
        }
        p.getChildren().add(rect(186, 310, 98, 12, 12, Color.web("#D8E0EE")));

        // floating coin
        StackPane coin = coin(32);
        coin.setLayoutX(62);
        coin.setLayoutY(98);
        Anim.floatLoop(coin, -12, 2.4, 0);

        // floating shield
        Circle shieldDisc = new Circle(29);
        shieldDisc.setFill(linear("#60A5FA", "#2563EB"));
        StackPane shield = new StackPane(shieldDisc, Icons.of("check", 28, "icon-white"));
        shield.setEffect(clayShadow(18, 9, "#1D4ED8", 0.32));
        shield.setLayoutX(372);
        shield.setLayoutY(170);
        shield.setMaxSize(58, 58);
        Anim.floatLoop(shield, 10, 2.9, 0.4);

        // floating receipt
        Pane receipt = new Pane();
        receipt.getChildren().addAll(
            rect(0, 0, 100, 70, 20, linear("#FFFFFF", "#EEF2FA")),
            rect(14, 16, 54, 8, 8, Color.web("#CBD5E1")),
            rect(14, 32, 72, 8, 8, Color.web("#E0E7F3")),
            rect(14, 48, 38, 8, 8, Color.web("#E0E7F3")));
        Circle tick = new Circle(82, 18, 9, Color.web("#10B981"));
        receipt.getChildren().add(tick);
        receipt.setEffect(clayShadow(20, 10, "#0F172A", 0.16));
        receipt.setLayoutX(338);
        receipt.setLayoutY(268);
        Anim.floatLoop(receipt, -9, 3.1, 0.9);
        p.getChildren().addAll(coin, shield, receipt);
        return p;
    }

    /** Friendly "coming soon" art for empty tabs: clay disc with an icon and floating dots. */
    public static Node emptyState(String iconName, String title, String subtitle) {
        Circle halo = new Circle(88);
        halo.getStyleClass().add("halo");
        Circle halo2 = new Circle(58);
        halo2.getStyleClass().add("halo-2");
        halo2.setTranslateX(70);
        halo2.setTranslateY(40);

        StackPane disc = new StackPane(Icons.of(iconName, 44, "icon-accent"));
        disc.getStyleClass().add("clay-disc");
        disc.setPrefSize(104, 104);
        disc.setMinSize(104, 104);
        disc.setMaxSize(104, 104);

        StackPane art = new StackPane(halo2, halo, disc);
        art.setPrefSize(260, 190);
        art.setMaxSize(260, 190);

        Label t = Widgets.label(title, "h-card");
        Label s = Widgets.label(subtitle, "h-sub");
        s.setWrapText(true);
        s.setTextAlignment(TextAlignment.CENTER);
        s.setMaxWidth(420);

        VBox box = new VBox(10, art, t, s);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new javafx.geometry.Insets(24, 20, 30, 20));
        return box;
    }
}
