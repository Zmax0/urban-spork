package com.urbanspork.client.gui.interpolation;

import javafx.geometry.Point2D;

/**
 * Piecewise monotone cubic Hermite interpolation for charts whose x axis is ordered.
 */
public final class MonotoneCubic {
    private static final double EPSILON = 1e-10;

    /**
     * Calculate a monotone cubic interpolation for a sequence of initial points and return the combined curve.
     *
     * @param points  Base points from which the source segments are taken
     * @param segment The number of samples to generate for each source segment
     * @return The combined curve
     */
    public Point2D[] interpolate(Point2D[] points, int segment) {
        if (points.length < 2) {
            throw new IllegalArgumentException("The points parameter must be greater than 2");
        }
        if (segment < 1) {
            throw new IllegalArgumentException("The segment parameter must be greater than 0");
        }
        Point2D[] normalized = compact(points);
        if (normalized.length == 1) {
            return new Point2D[]{normalized[0], normalized[0]};
        }
        if (normalized.length < 3) {
            return normalized;
        }
        if (!isStrictlyIncreasingX(normalized)) {
            return samplePolyline(normalized, segment);
        }
        double[] slopes = slopes(normalized);
        double[] tangents = tangents(normalized, slopes);
        Point2D[] curve = new Point2D[1 + (normalized.length - 1) * segment];
        curve[0] = normalized[0];
        int offset = 1;
        for (int i = 0; i < normalized.length - 1; i++) {
            Point2D[] samples = interpolate(normalized[i], normalized[i + 1], tangents[i], tangents[i + 1], segment);
            System.arraycopy(samples, 0, curve, offset, samples.length);
            offset += samples.length;
        }
        return curve;
    }

    private Point2D[] compact(Point2D[] points) {
        Point2D[] compacted = new Point2D[points.length];
        int size = 0;
        for (Point2D point : points) {
            if (size > 0 && samePoint(compacted[size - 1], point)) {
                continue;
            }
            compacted[size++] = point;
        }
        Point2D[] normalized = new Point2D[size];
        System.arraycopy(compacted, 0, normalized, 0, size);
        return normalized;
    }

    private boolean samePoint(Point2D left, Point2D right) {
        return Math.abs(left.getX() - right.getX()) < EPSILON && Math.abs(left.getY() - right.getY()) < EPSILON;
    }

    private boolean isStrictlyIncreasingX(Point2D[] points) {
        for (int i = 1; i < points.length; i++) {
            if (points[i].getX() - points[i - 1].getX() <= EPSILON) {
                return false;
            }
        }
        return true;
    }

    private double[] slopes(Point2D[] points) {
        double[] slopes = new double[points.length - 1];
        for (int i = 0; i < slopes.length; i++) {
            Point2D left = points[i];
            Point2D right = points[i + 1];
            slopes[i] = (right.getY() - left.getY()) / (right.getX() - left.getX());
        }
        return slopes;
    }

    private double[] tangents(Point2D[] points, double[] slopes) {
        double[] tangents = new double[points.length];
        tangents[0] = endpointTangent(points[0], points[1], points[2], slopes[0], slopes[1]);
        for (int i = 1; i < points.length - 1; i++) {
            tangents[i] = tangent(points[i - 1], points[i], points[i + 1], slopes[i - 1], slopes[i]);
        }
        tangents[tangents.length - 1] = endpointTangent(
            points[points.length - 1],
            points[points.length - 2],
            points[points.length - 3],
            slopes[slopes.length - 1],
            slopes[slopes.length - 2]
        );
        return tangents;
    }

    private double tangent(Point2D previous, Point2D point, Point2D next, double previousSlope, double nextSlope) {
        if (Math.abs(previousSlope) < EPSILON || Math.abs(nextSlope) < EPSILON || previousSlope * nextSlope < 0) {
            return 0;
        }
        double previousWidth = point.getX() - previous.getX();
        double nextWidth = next.getX() - point.getX();
        double w1 = 2 * nextWidth + previousWidth;
        double w2 = nextWidth + 2 * previousWidth;
        return (w1 + w2) / (w1 / previousSlope + w2 / nextSlope);
    }

    private double endpointTangent(Point2D endpoint, Point2D next, Point2D nextNext, double slope, double neighborSlope) {
        if (Math.abs(slope) < EPSILON) {
            return 0;
        }
        double width = Math.abs(next.getX() - endpoint.getX());
        double neighborWidth = Math.abs(nextNext.getX() - next.getX());
        double tangent = ((2 * width + neighborWidth) * slope - width * neighborSlope) / (width + neighborWidth);
        if (diffSign(tangent, slope)) {
            return 0;
        }
        if (diffSign(slope, neighborSlope) && Math.abs(tangent) > Math.abs(3 * slope)) {
            return 3 * slope;
        }
        return tangent;
    }

    private boolean diffSign(double left, double right) {
        return left != 0 && right != 0 && Math.signum(left) != Math.signum(right);
    }

    private Point2D[] interpolate(Point2D left, Point2D right, double leftTangent, double rightTangent, int segment) {
        double width = right.getX() - left.getX();
        if (width <= EPSILON) {
            return sampleLinear(left, right, segment);
        }
        Point2D[] curve = new Point2D[segment];
        double minY = Math.min(left.getY(), right.getY());
        double maxY = Math.max(left.getY(), right.getY());
        for (int i = 0; i < segment; i++) {
            double t = (double) (i + 1) / segment;
            double x = left.getX() + width * t;
            double value = hermite(left.getY(), right.getY(), leftTangent * width, rightTangent * width, t);
            double y = Math.clamp(value, minY, maxY);
            curve[i] = new Point2D(x, y);
        }
        curve[curve.length - 1] = right;
        return curve;
    }

    private double hermite(double y0, double y1, double m0, double m1, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        return (2 * t3 - 3 * t2 + 1) * y0 + (t3 - 2 * t2 + t) * m0 + (-2 * t3 + 3 * t2) * y1 + (t3 - t2) * m1;
    }

    private Point2D[] samplePolyline(Point2D[] points, int segment) {
        Point2D[] curve = new Point2D[1 + (points.length - 1) * segment];
        curve[0] = points[0];
        int offset = 1;
        for (int i = 0; i < points.length - 1; i++) {
            Point2D[] samples = sampleLinear(points[i], points[i + 1], segment);
            System.arraycopy(samples, 0, curve, offset, samples.length);
            offset += samples.length;
        }
        return curve;
    }

    private Point2D[] sampleLinear(Point2D left, Point2D right, int segment) {
        Point2D[] curve = new Point2D[segment];
        for (int i = 0; i < segment; i++) {
            double ratio = (double) (i + 1) / segment;
            curve[i] = left.multiply(1 - ratio).add(right.multiply(ratio));
        }
        curve[curve.length - 1] = right;
        return curve;
    }
}
