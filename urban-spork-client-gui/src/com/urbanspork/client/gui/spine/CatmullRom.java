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
    /**
     * Calculate Catmull-Rom for a sequence of initial points and return the combined curve.
     *
     * @param points  Base points from which the quadruples for the algorithm are taken
     * @param segment The number of points include in each curve segment
     * @return The combined curve
     */
    public Point2D[] interpolate(Point2D[] points, int segment) {
        if (points.length < 2) {
            throw new IllegalArgumentException("The points parameter must be greater than 2");
        }
        if (points.length < 3) {
            return points;
        }
        Point2D[] controls = getControls(points);
        Point2D[] curve = new Point2D[points.length + (points.length - 1) * segment];
        for (int i = 0; i < controls.length - 3; i++) {
            curve[i * (segment + 1)] = controls[i + 1];
            Point2D[] temp = interpolate(controls[i], controls[i + 1], controls[i + 2], controls[i + 3], segment);
            System.arraycopy(temp, 0, curve, i * (segment + 1) + 1, segment);
        }
        curve[curve.length - 1] = (controls[controls.length - 2]); // last
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
     * Calculate Catmull-Rom spline curve points starts with p1 and ends with p2
     *
     * @param p0      The (x,y) point pairs that define the Catmull-Rom spline
     * @param p1      The (x,y) point pairs that define the Catmull-Rom spline
     * @param p2      The (x,y) point pairs that define the Catmull-Rom spline
     * @param p3      The (x,y) point pairs that define the Catmull-Rom spline
     * @param segment The number of points to include in each curve segment
     * @return points for the resulting curve
     */
    public Point2D[] interpolate(Point2D p0, Point2D p1, Point2D p2, Point2D p3, int segment) {
        // calculate knots
        double t0 = 0;
        double t1 = getT(t0, p0, p1);
        double t2 = getT(t1, p1, p2);
        double t3 = getT(t2, p2, p3);
        double step = (t2 - t1) / (segment - 1);
        // evaluate the point
        Point2D[] curve = new Point2D[segment];
        curve[0] = p1;
        for (int i = 1; i < segment - 1; i++) {
            double t = t1 + i * step;
            Point2D a1 = p0.multiply((t1 - t) / (t1 - t0)).add(p1.multiply((t - t0) / (t1 - t0)));
            Point2D a2 = p1.multiply((t2 - t) / (t2 - t1)).add(p2.multiply((t - t1) / (t2 - t1)));
            Point2D a3 = p2.multiply((t3 - t) / (t3 - t2)).add(p3.multiply((t - t2) / (t3 - t2)));
            Point2D b1 = a1.multiply((t2 - t) / (t2 - t0)).add(a2.multiply((t - t0) / (t2 - t0)));
            Point2D b2 = a2.multiply((t3 - t) / (t3 - t1)).add(a3.multiply((t - t1) / (t3 - t1)));
            curve[i] = b1.multiply((t2 - t) / (t2 - t1)).add(b2.multiply((t - t1) / (t2 - t1)));
        }
        curve[curve.length - 1] = p2;
        return curve;
    }

    // calculate knots
    private double getT(double t, Point2D p0, Point2D p1) {
        Point2D d = p1.subtract(p0);
        return Math.pow(d.dotProduct(d), alpha * .5) + t;
    }
}
