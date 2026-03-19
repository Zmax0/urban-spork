package com.urbanspork.client.gui.traffic;

import com.urbanspork.client.gui.interpolation.MonotoneCubic;
import com.urbanspork.client.gui.util.HumanReadable;
import io.netty.handler.traffic.TrafficCounter;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.PathElement;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

public class TrafficCounterLineChartBackstage {
    private static final int WINDOW = 60;
    private static final int FPS = 60;
    private final Timeline sampleTimeline = new Timeline();
    private final Timeline renderTimeline = new Timeline(new KeyFrame(Duration.millis((double) 1000 / FPS), _ -> slide()));
    private final ArrayList<Double> samplesAt = new ArrayList<>();
    private final XYChart.Data<Number, Number> writeLeft = new XYChart.Data<>(-WINDOW, 0);
    private final XYChart.Data<Number, Number> writeRight = new XYChart.Data<>(0, 0);
    private final XYChart.Data<Number, Number> readLeft = new XYChart.Data<>(-WINDOW, 0);
    private final XYChart.Data<Number, Number> readRight = new XYChart.Data<>(0, 0);
    private final ObservableList<XYChart.Data<Number, Number>> write = FXCollections.observableArrayList(new ArrayList<>());
    private final ObservableList<XYChart.Data<Number, Number>> read = FXCollections.observableArrayList(new ArrayList<>());
    private XYChart.Series<Number, Number> writeSeries;
    private XYChart.Series<Number, Number> readSeries;
    private long startedAt;

    public TrafficCounterLineChartBackstage() {
        sampleTimeline.setCycleCount(Animation.INDEFINITE);
        renderTimeline.setCycleCount(Animation.INDEFINITE);
    }

    public LineChart<Number, Number> newLineChart() {
        NumberAxis xAxis = new NumberAxis(-WINDOW, 0, WINDOW);
        xAxis.setTickLabelsVisible(false);
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
        samplesAt.clear();
        startedAt = System.nanoTime();
        writeLeft.setXValue(-WINDOW);
        writeLeft.setYValue(0);
        writeRight.setXValue(0);
        writeRight.setYValue(0);
        readLeft.setXValue(-WINDOW);
        readLeft.setYValue(0);
        readRight.setXValue(0);
        readRight.setYValue(0);
        write.add(writeLeft);
        read.add(readLeft);
        for (int i = 0; i <= WINDOW; i++) {
            double x = i - WINDOW;
            samplesAt.add(x);
            write.add(new XYChart.Data<>(x, 0));
            read.add(new XYChart.Data<>(x, 0));
        }
        write.add(writeRight);
        read.add(readRight);
        writeSeries = new XYChart.Series<>(write);
        readSeries = new XYChart.Series<>(read);
        writeSeries.setName("0 KB/s");
        readSeries.setName("0 KB/s");
        list.add(writeSeries);
        list.add(readSeries);
        return lineChart;
    }

    public void refresh(TrafficCounter counter) {
        ObservableList<KeyFrame> keyFrames = sampleTimeline.getKeyFrames();
        keyFrames.setAll(new KeyFrame(Duration.millis(counter.checkInterval()), _ -> sample(counter)));
        sample(counter);
        sampleTimeline.playFromStart();
        renderTimeline.playFromStart();
    }

    public void stop() {
        sampleTimeline.stop();
        renderTimeline.stop();
    }

    private void sample(TrafficCounter counter) {
        double now = (System.nanoTime() - startedAt) / 1_000_000_000.0;
        long writeBytes = counter.lastWrittenBytes();
        long readBytes = counter.lastReadBytes();
        writeSeries.setName(HumanReadable.byteCountSI(writeBytes));
        readSeries.setName(HumanReadable.byteCountSI(readBytes));
        samplesAt.add(now);
        write.add(write.size() - 1, new XYChart.Data<>(0, writeBytes));
        read.add(read.size() - 1, new XYChart.Data<>(0, readBytes));
        while (samplesAt.size() > 1 && samplesAt.getFirst() < now - WINDOW) {
            samplesAt.removeFirst();
            write.remove(1);
            read.remove(1);
        }
        slide();
    }

    private void slide() {
        double now = (System.nanoTime() - startedAt) / 1_000_000_000.0;
        for (int i = 0; i < samplesAt.size(); i++) {
            double x = samplesAt.get(i) - now;
            write.get(i + 1).setXValue(x);
            read.get(i + 1).setXValue(x);
        }
        writeLeft.setYValue(write.get(1).getYValue());
        writeRight.setYValue(write.get(write.size() - 2).getYValue());
        readLeft.setYValue(read.get(1).getYValue());
        readRight.setYValue(read.get(read.size() - 2).getYValue());
    }

    private static class TrafficCounterLineChart extends LineChart<Number, Number> {
        private static final int SMOOTH_SEGMENTS = 12;
        private static final Point2D[] EMPTY_POINTS = new Point2D[0];
        private static final MonotoneCubic CURVE = new MonotoneCubic();
        private final Map<Series<Number, Number>, CurveCache> curveCache = new IdentityHashMap<>();

        public TrafficCounterLineChart(Axis<Number> xAxis, Axis<Number> yAxis) {
            super(xAxis, yAxis);
        }

        @Override
        protected void layoutPlotChildren() {
            super.layoutPlotChildren();
            ObservableList<Series<Number, Number>> data = getData();
            curveCache.keySet().retainAll(data);
            data.forEach(this::curve);
        }

        private void curve(Series<Number, Number> series) {
            if (!(series.getNode() instanceof Path path)) {
                return;
            }
            Point2D[] points = points(path.getElements());
            if (points.length < 2) {
                curveCache.remove(series);
                return;
            }
            CurveCache cache = curveCache.computeIfAbsent(series, _ -> new CurveCache());
            Point2D[] interpolate = CURVE.interpolate(points, SMOOTH_SEGMENTS);
            cache.rebuild(interpolate);
            path.getElements().setAll(cache.elements);
        }

        private Point2D[] points(ObservableList<PathElement> elements) {
            if (elements.size() < 2) {
                return EMPTY_POINTS;
            }
            PathElement first = elements.getFirst();
            if (!(first instanceof MoveTo moveTo)) {
                return EMPTY_POINTS;
            }
            Point2D[] points = new Point2D[elements.size()];
            points[0] = new Point2D(moveTo.getX(), moveTo.getY());
            for (int i = 1; i < points.length; i++) {
                PathElement element = elements.get(i);
                if (!(element instanceof LineTo lineTo)) {
                    return EMPTY_POINTS;
                }
                points[i] = new Point2D(lineTo.getX(), lineTo.getY());
            }
            return points;
        }

        private static final class CurveCache {
            private final ArrayList<PathElement> elements = new ArrayList<>();

            private void rebuild(Point2D[] interpolate) {
                if (elements.size() != interpolate.length) {
                    elements.clear();
                    elements.add(new MoveTo(interpolate[0].getX(), interpolate[0].getY()));
                    for (int i = 1; i < interpolate.length; i++) {
                        elements.add(new LineTo(interpolate[i].getX(), interpolate[i].getY()));
                    }
                    return;
                }
                update(elements.getFirst(), interpolate[0]);
                for (int i = 1; i < interpolate.length; i++) {
                    update(elements.get(i), interpolate[i]);
                }
            }

            private void update(PathElement element, Point2D point) {
                if (element instanceof MoveTo moveTo) {
                    moveTo.setX(point.getX());
                    moveTo.setY(point.getY());
                } else if (element instanceof LineTo lineTo) {
                    lineTo.setX(point.getX());
                    lineTo.setY(point.getY());
                }
            }
        }
    }
}
