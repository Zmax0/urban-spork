package com.urbanspork.client.gui.traffic;

import com.urbanspork.client.gui.spine.CatmullRom;
import io.netty.handler.traffic.TrafficCounter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.util.Duration;

import java.util.ArrayList;

public class TrafficCounterLineChartBackstage {
    private static final int WINDOW = 60;
    private final Timeline timeline = new Timeline();
    private final ObservableList<XYChart.Data<Number, Number>> write = FXCollections.observableArrayList(new ArrayList<>());
    private final ObservableList<XYChart.Data<Number, Number>> read = FXCollections.observableArrayList(new ArrayList<>());
    private XYChart.Series<Number, Number> writeSeries;
    private XYChart.Series<Number, Number> readSeries;

    public TrafficCounterLineChartBackstage(ObjectProperty<TrafficCounter> trafficCounter) {
        timeline.setCycleCount(Timeline.INDEFINITE);
        trafficCounter.addListener((observable, oldValue, newValue) -> {
            if (newValue != null && timeline.getStatus() == Animation.Status.RUNNING) {
                refresh(newValue);
            }
        });
    }

    public LineChart<Number, Number> newLineChart() {
        NumberAxis xAxis = new NumberAxis(0, WINDOW, WINDOW);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelsVisible(false);
        yAxis.setTickMarkVisible(false);
        yAxis.lookup(".axis-minor-tick-mark").setVisible(false);
        LineChart<Number, Number> lineChart = new TrafficCounterLineChart(xAxis, yAxis);
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(false);
        ObservableList<XYChart.Series<Number, Number>> list = lineChart.getData();
        write.clear();
        read.clear();
        for (int i = 0; i <= WINDOW; i++) {
            write.add(new XYChart.Data<>(i, 0));
            read.add(new XYChart.Data<>(i, 0));
        }
        writeSeries = new XYChart.Series<>(write);
        readSeries = new XYChart.Series<>(read);
        writeSeries.setName("0 KB/s");
        readSeries.setName("0 KB/s");
        list.add(writeSeries);
        list.add(readSeries);
        return lineChart;
    }

    public void refresh(TrafficCounter counter) {
        ObservableList<KeyFrame> keyFrames = timeline.getKeyFrames();
        keyFrames.clear();
        keyFrames.add(new KeyFrame(Duration.millis(counter.checkInterval()), event -> {
            long writeBytes = counter.lastWrittenBytes() / 1024;
            long readBytes = counter.lastReadBytes() / 1024;
            writeSeries.setName(writeBytes + " KB/s");
            readSeries.setName(readBytes + " KB/s");
            scroll(write, writeBytes);
            scroll(read, readBytes);
        }));
        timeline.playFromStart();
    }

    public void stop() {
        timeline.stop();
    }

    private void scroll(ObservableList<XYChart.Data<Number, Number>> dataList, Number y) {
        for (int i = 0; i < dataList.size() - 1; i++) {
            dataList.get(i).setYValue(dataList.get(i + 1).getYValue());
        }
        dataList.getLast().setYValue(y);
    }

    private static class TrafficCounterLineChart extends LineChart<Number, Number> {
        public TrafficCounterLineChart(Axis<Number> xAxis, Axis<Number> yAxis) {
            super(xAxis, yAxis);
        }

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
    }
}
