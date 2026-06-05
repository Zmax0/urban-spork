package com.urbanspork.client.gui.console.widget;

import com.urbanspork.common.config.ServerConfig;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.control.TableRow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.List;

public class ActiveServerTableRow extends TableRow<ServerConfig> {
    public static final PseudoClass ACTIVE_SERVER = PseudoClass.getPseudoClass("active-server");

    private static final StyleablePropertyFactory<ActiveServerTableRow> STYLE = new StyleablePropertyFactory<>(TableRow.getClassCssMetaData());
    private static final CssMetaData<ActiveServerTableRow, Color> COLOR_START = STYLE.createColorCssMetaData("-fx-active-server-pulse-start", row -> row.pulseStart, null);
    private static final CssMetaData<ActiveServerTableRow, Color> COLOR_END = STYLE.createColorCssMetaData("-fx-active-server-pulse-end", row -> row.pulseEnd, null);

    private final StyleableObjectProperty<Color> pulseStart = new SimpleStyleableObjectProperty<>(COLOR_START, this, "activeServerPulseStart");
    private final StyleableObjectProperty<Color> pulseEnd = new SimpleStyleableObjectProperty<>(COLOR_END, this, "activeServerPulseEnd");
    private final ObjectProperty<Color> pulse = new SimpleObjectProperty<>();
    private final Timeline timeline = new Timeline();

    private boolean active;

    public ActiveServerTableRow() {
        timeline.setAutoReverse(true);
        timeline.setCycleCount(Timeline.INDEFINITE);
        pulseStart.addListener((_, _, _) -> refreshPulseTimeline());
        pulseEnd.addListener((_, _, _) -> refreshPulseTimeline());
        pulse.addListener((_, _, color) -> updateStyle(color));
        selectedProperty().addListener((_, _, _) -> updatePulseState());
        refreshPulseTimeline();
    }

    public void setActive(boolean active) {
        this.active = active;
        pseudoClassStateChanged(ACTIVE_SERVER, active);
        updatePulseState();
    }

    @Override
    protected void updateItem(ServerConfig item, boolean empty) {
        super.updateItem(item, empty);
        updatePulseState();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getControlCssMetaData() {
        return STYLE.getCssMetaData();
    }

    private void updatePulseState() {
        if (shouldPulse()) {
            if (timeline.getStatus() != Animation.Status.RUNNING) {
                timeline.play();
            }
            updateStyle(pulse.get());
            return;
        }
        timeline.stop();
        setStyle("");
    }

    private boolean shouldPulse() {
        return active && !isEmpty() && getItem() != null && !isSelected();
    }

    private void refreshPulseTimeline() {
        Color pulseStart = this.pulseStart.get();
        Color pulseEnd = this.pulseEnd.get();
        timeline.stop();
        if (pulseStart == null || pulseEnd == null) {
            pulse.set(null);
            return;
        }
        timeline.getKeyFrames().setAll(
            new KeyFrame(Duration.ZERO, new KeyValue(pulse, pulseStart)),
            new KeyFrame(Duration.millis(1854), new KeyValue(pulse, pulseEnd))
        );
        pulse.set(pulseStart);
        if (shouldPulse()) {
            timeline.play();
        }
    }

    private void updateStyle(Color color) {
        if (!active || isEmpty() || getItem() == null || isSelected() || color == null) {
            return;
        }
        setStyle("-fx-background-color: " + toCssRgba(color) + ";");
    }

    private String toCssRgba(Color color) {
        int red = (int) Math.round(color.getRed() * 255);
        int green = (int) Math.round(color.getGreen() * 255);
        int blue = (int) Math.round(color.getBlue() * 255);
        return "rgba(" + red + ", " + green + ", " + blue + ", " + color.getOpacity() + ")";
    }

}
