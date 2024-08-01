package com.urbanspork.client.gui.traffic;

import com.urbanspork.client.gui.spine.CatmullRom;
import io.netty.handler.traffic.TrafficCounter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.util.Duration;

public class TrafficCounterLineChartBuilder {
    private static final int WINDOW = 60;

    private final XYChart.Series<Number, Number> write = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> read = new XYChart.Series<>();
    private final Timeline timeline = new Timeline();

    public TrafficCounterLineChartBuilder(ObjectProperty<TrafficCounter> counter) {
        counter.addListener((observable, oldValue, newValue) -> refresh(newValue));
        timeline.setCycleCount(Animation.INDEFINITE);
    }

    public LineChart<Number, Number> build() {
        NumberAxis xAxis = new NumberAxis(0, WINDOW, WINDOW);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(false);
        yAxis.setTickMarkVisible(false);
        yAxis.lookup(".axis-minor-tick-mark").setVisible(false);
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis) {
            @Override
            protected void layoutPlotChildren() {
                super.layoutPlotChildren();
                ObservableList<Series<Number, Number>> data = getData();
                data.stream().map(Series::getNode).<Path>mapMulti((node, consumer) -> {
                    if (node instanceof Path path) {
                        consumer.accept(path);
                    }
                }).forEach(this::curve);
            }

            private void curve(Path path) {
                ObservableList<PathElement> elements = path.getElements();
                if (elements.size() > 3) {
                    curve(elements);
                }
            }

            private void curve(ObservableList<PathElement> elements) {
                int size = elements.size();
                Point2D[] points = new Point2D[size - 1];
                for (int i = 0; i < points.length; i++) {
                    PathElement e = elements.get(i + 1);
                    if (e instanceof LineTo lineTo) {
                        points[i] = new Point2D(lineTo.getX(), lineTo.getY());
                    }
                }
                Point2D[] interpolate = new CatmullRom(0.5).interpolate(points, 32);
                // update
                for (int i = 1; i < size; i++) {
                    PathElement e = elements.get(i);
                    if (e instanceof LineTo lineTo) {
                        lineTo.setX(interpolate[i - 1].getX());
                        lineTo.setY(interpolate[i - 1].getY());
                    }
                }
                // add
                for (int i = 0; i < interpolate.length - size; i++) {
                    elements.add(new LineTo(interpolate[i + size].getX(), interpolate[i + size].getY()));
                }
            }
        };
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(false);
        ObservableList<XYChart.Series<Number, Number>> data = lineChart.getData();
        write.setName("0 KB/s");
        read.setName("0 KB/s");
        ObservableList<XYChart.Data<Number, Number>> writeData = write.getData();
        ObservableList<XYChart.Data<Number, Number>> readData = read.getData();
        for (int i = 0; i <= WINDOW; i++) {
            writeData.add(new XYChart.Data<>(i, 0));
            readData.add(new XYChart.Data<>(i, 0));
        }
        data.add(write);
        data.add(read);
        return lineChart;
    }

    private void refresh(TrafficCounter counter) {
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
        keyFrames.clear();
        keyFrames.add(new KeyFrame(Duration.millis(counter.checkInterval()), event -> {
            ObservableList<XYChart.Data<Number, Number>> writeData = write.getData();
            ObservableList<XYChart.Data<Number, Number>> readData = read.getData();
            long writeBytes = counter.lastWrittenBytes() / 1024;
            long readBytes = counter.lastReadBytes() / 1024;
            write.setName(writeBytes + " KB/s");
            read.setName(readBytes + " KB/s");
            scroll(writeData, writeBytes);
            scroll(readData, readBytes);
        }));
        timeline.playFromStart();
    }

    private void scroll(ObservableList<XYChart.Data<Number, Number>> dataList, Number y) {
        for (int i = 0; i < dataList.size() - 1; i++) {
            dataList.get(i).setYValue(dataList.get(i + 1).getYValue());
        }
        dataList.getLast().setYValue(y);
    }
}
