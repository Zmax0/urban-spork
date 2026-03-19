package com.urbanspork.client.gui.spine;

import javafx.geometry.Point2D;

/**
 * Java implement of <a href="https://en.wikipedia.org/wiki/Centripetal_Catmull–Rom_spline">Centripetal Catmull–Rom spline</a>
 *
 * @param alpha The alpha value ranges from 0 to 1
 *              <ul>
 *              <li>0.5 for the centripetal spline
 *              <li>0.0 for the uniform spline
 *              <li>1.0 for the chordal spline
 *              </ul>
 * @author Zmax0
 */
public record CatmullRom(double alpha) {
    private static final double EPSILON = 1e-10;

    public CatmullRom {
        if (!Double.isFinite(alpha) || alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha must be between 0 and 1");
        }
    }

    /**
     * Calculate Catmull-Rom for a sequence of initial points and return the combined curve.
     *
     * @param points  Base points from which the quadruples for the algorithm are taken
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
        if (points.length < 3) {
            return points;
        }
        Point2D[] controls = getControls(points);
        // Emit a single continuous point list so the caller can rebuild the chart path directly.
        Point2D[] curve = new Point2D[1 + (points.length - 1) * segment];
        curve[0] = points[0];
        int offset = 1;
        for (int i = 0; i < controls.length - 3; i++) {
            Point2D[] temp = interpolate(controls[i], controls[i + 1], controls[i + 2], controls[i + 3], segment);
            System.arraycopy(temp, 0, curve, offset, temp.length);
            offset += temp.length;
        }
        return curve;
    }

    private Point2D[] getControls(Point2D[] point) {
        Point2D[] controls = new Point2D[point.length + 2];
        System.arraycopy(point, 0, controls, 1, point.length);
        // set two control points at start C1 and end C2
        if (point[0].equals(point[point.length - 1])) { // if is close
            // C1 = P(n-1)
            controls[0] = point[point.length - 2];
            // C2 = P1
            controls[controls.length - 1] = point[1];
        } else {
            // C1 = P0 - (P1 - P0)
            controls[0] = point[0].subtract(point[1].subtract(point[0]));
            // C2 = Pn + (Pn - P(n-1))
            controls[controls.length - 1] = point[point.length - 1].add(point[point.length - 1].subtract(point[point.length - 2]));
        }
        return controls;
    }

    /**
     * Calculate the sampled points between p1 and p2.
     *
     * @param p0      The (x,y) point pairs that define the Catmull-Rom spline
     * @param p1      The (x,y) point pairs that define the Catmull-Rom spline
     * @param p2      The (x,y) point pairs that define the Catmull-Rom spline
     * @param p3      The (x,y) point pairs that define the Catmull-Rom spline
     * @param segment The number of samples to generate between p1 and p2
     * @return Sampled points after p1 and ending at p2
     */
    public Point2D[] interpolate(Point2D p0, Point2D p1, Point2D p2, Point2D p3, int segment) {
        if (segment < 1) {
            throw new IllegalArgumentException("The segment parameter must be greater than 0");
        }
        // calculate knots
        double dt0 = getDelta(p0, p1);
        double dt1 = getDelta(p1, p2);
        double dt2 = getDelta(p2, p3);
        if (dt1 < EPSILON) {
            // Consecutive identical samples are common in traffic charts; degrade to a straight segment.
            return sampleLinear(p1, p2, segment);
        }
        if (dt0 < EPSILON) {
            dt0 = dt1;
        }
        if (dt2 < EPSILON) {
            dt2 = dt1;
        }
        // evaluate the point
        CubicPolynomial xPolynomial = initNonUniformPolynomial(p0.getX(), p1.getX(), p2.getX(), p3.getX(), dt0, dt1, dt2);
        CubicPolynomial yPolynomial = initNonUniformPolynomial(p0.getY(), p1.getY(), p2.getY(), p3.getY(), dt0, dt1, dt2);
        Point2D[] curve = new Point2D[segment];
        double minX = Math.min(p1.getX(), p2.getX());
        double maxX = Math.max(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double maxY = Math.max(p1.getY(), p2.getY());
        boolean increasingX = p2.getX() >= p1.getX();
        for (int i = 0; i < segment; i++) {
            double t = (double) (i + 1) / segment;
            // Clamp each sample to the local segment box to avoid chart overshoot and fake spikes.
            double x = clamp(xPolynomial.at(t), minX, maxX);
            double y = clamp(yPolynomial.at(t), minY, maxY);
            if (i > 0) {
                // The traffic chart always moves forward in time, so keep sampled X monotonic.
                double previousX = curve[i - 1].getX();
                x = increasingX ? Math.max(x, previousX) : Math.min(x, previousX);
            }
            curve[i] = new Point2D(x, y);
        }
        curve[curve.length - 1] = p2;
        return curve;
    }

    private Point2D[] sampleLinear(Point2D p1, Point2D p2, int segment) {
        Point2D[] curve = new Point2D[segment];
        for (int i = 0; i < segment; i++) {
            double ratio = (double) (i + 1) / segment;
            curve[i] = p1.multiply(1 - ratio).add(p2.multiply(ratio));
        }
        curve[curve.length - 1] = p2;
        return curve;
    }

    private CubicPolynomial initNonUniformPolynomial(double x0, double x1, double x2, double x3, double dt0, double dt1, double dt2) {
        double t1 = (x1 - x0) / dt0 - (x2 - x0) / (dt0 + dt1) + (x2 - x1) / dt1;
        double t2 = (x2 - x1) / dt1 - (x3 - x1) / (dt1 + dt2) + (x3 - x2) / dt2;
        return new CubicPolynomial(x1, x2, t1 * dt1, t2 * dt1);
    }

    private double getDelta(Point2D p0, Point2D p1) {
        Point2D d = p1.subtract(p0);
        double lengthSquared = d.dotProduct(d);
        if (lengthSquared < EPSILON) {
            return 0;
        }
        return Math.pow(lengthSquared, alpha * .5);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class CubicPolynomial {
        private final double c0;
        private final double c1;
        private final double c2;
        private final double c3;

        private CubicPolynomial(double x0, double x1, double t0, double t1) {
            this.c0 = x0;
            this.c1 = t0;
            this.c2 = -3 * x0 + 3 * x1 - 2 * t0 - t1;
            this.c3 = 2 * x0 - 2 * x1 + t0 + t1;
        }

        private double at(double t) {
            return ((c3 * t + c2) * t + c1) * t + c0;
        }
    }
}
