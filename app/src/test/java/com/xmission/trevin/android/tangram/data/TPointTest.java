/*
 * This class created by Claude Code (Opus 4.8);
 * later additions by Trevin Beattie and Claude.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.xmission.trevin.android.tangram.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.util.Locale;

/**
 * Unit tests for {@link TPoint} geometry.
 *
 * @author Claude Opus 4.8
 * @author Trevin Beattie
 */
public class TPointTest {

    private static final double DELTA = 1e-4;

    /**
     * Every 45&deg; step of {@link TPoint#coarseRotate(int)} must agree
     * with an analytic rotation of the point&rsquo;s real coordinates.
     * (Guards against per-case transcription errors like the one that once
     * collapsed the 225&deg; case onto the x=y diagonal.)  This runs the
     * test against the {@link ImmutableTPoint} code, where the origin
     * point never changes.
     */
    @Test
    public void testCoarseRotateMatchesAnalyticRotationImmutable() {
        TPoint p = new ImmutableTPoint(3, 0, 1, 2);
        // An asymmetric point so x != y and the cases can't alias.
        double px = p.getX(), py = p.getY();
        for (int steps = 0; steps < 8; steps++) {
            double theta = Math.toRadians(45.0 * steps);
            double cos = Math.cos(theta), sin = Math.sin(theta);
            double expectedX = px * cos - py * sin;
            double expectedY = px * sin + py * cos;
            TPoint rotated = p.coarseRotate(steps);
            assertEquals("x after " + steps + " steps",
                    expectedX, rotated.getX(), DELTA);
            assertEquals("y after " + steps + " steps",
                    expectedY, rotated.getY(), DELTA);
        }
    }

    /**
     * Every 45&deg; step of {@link TPoint#coarseRotate(int)} must agree
     * with an analytic rotation of the point&rsquo;s real coordinates.
     * (Guards against per-case transcription errors like the one that once
     * collapsed the 225&deg; case onto the x=y diagonal.)  This runs the
     * test against the {@link MutableTPoint} code, where the
     * {@link TPoint#coarseRotate(int)} method may modify the point in-place.
     */
    @Test
    public void testCoarseRotateMatchesAnalyticRotationMutable() {
        for (int steps = 0; steps < 8; steps++) {
            TPoint p = new MutableTPoint(3, 0, 1, 2);
            double px = p.getX(), py = p.getY();
            double theta = Math.toRadians(45.0 * steps);
            double cos = Math.cos(theta), sin = Math.sin(theta);
            double expectedX = px * cos - py * sin;
            double expectedY = px * sin + py * cos;
            TPoint rotated = p.coarseRotate(steps);
            assertEquals("x after " + steps + " steps",
                    expectedX, rotated.getX(), DELTA);
            assertEquals("y after " + steps + " steps",
                    expectedY, rotated.getY(), DELTA);
        }
    }

    /** A rotation must never collapse a point onto the x=y diagonal. */
    @Test
    public void testCoarseRotatePreservesDistinctCoordinatesImmutable() {
        TPoint p = new ImmutableTPoint(3, 0, 1, 2);
        final double expected = Math.hypot(p.getX(), p.getY());
        for (int steps = 0; steps < 8; steps++) {
            TPoint rotated = p.coarseRotate(steps);
            double radius = Math.hypot(rotated.getX(), rotated.getY());
            assertEquals("radius preserved after " + steps + " steps",
                    expected, radius, DELTA);
        }
    }

    /** A rotation must never collapse a point onto the x=y diagonal. */
    @Test
    public void testCoarseRotatePreservesDistinctCoordinatesMutable() {
        for (int steps = 0; steps < 8; steps++) {
            TPoint p = new ImmutableTPoint(3, 0, 1, 2);
            double expected = Math.hypot(p.getX(), p.getY());
            TPoint rotated = p.coarseRotate(steps);
            double radius = Math.hypot(rotated.getX(), rotated.getY());
            assertEquals("radius preserved after " + steps + " steps",
                    expected, radius, DELTA);
        }
    }

    /**
     * Simple test of {@link TPoint#nearestQ2GridPoint()} where the
     * point&rsquo;s coefficients are already integers.  The test
     * will start from the {@link ImmutableTPoint#ORIGIN} and work
     * its way out from there alternating between positive and negative
     * coefficients, taking advantage of the fact that the X and Y
     * coordinates are calculated independently.
     */
    @Test
    public void testNearestQ2GridPointAlreadyOnQ2Grid() {
        TPoint actual = ImmutableTPoint.ORIGIN.nearestQ2GridPoint();
        assertEquals("Nearest point to the origin",
                ImmutableTPoint.ORIGIN, actual);
        // We're going to re-use a mutable TPoint
        // to avoid tens of thousands of allocations.
        MutableTPoint expected = new MutableTPoint(0, 0, 0, 0);
        for (int a = 1; a <= 100; a++) {
            for (int b = 0; b <= 100 - a; b++) {
                // First pass: both positive on X, both negative on Y
                expected.setXa(a);
                expected.setXb(b);
                expected.setYa(-a);
                expected.setYb(-b);
                actual = expected.nearestQ2GridPoint();
                assertEquals("Nearest point", expected, actual);
                // Second pass: both a coefficients positive, both b negative
                expected.setXb(-b);
                expected.setYa(a);
                actual = expected.nearestQ2GridPoint();
                assertEquals("Nearest point", expected, actual);
            }
        }
    }

    /**
     * Test {@link TPoint#nearestQ2GridPoint()} where the <i>b</i>
     * coefficient has been collapsed into <i>a</i> (i.e. <i>a</i>&#8242;
     * = <i>a</i> + <i>b</i>&#8730;2&#773;, <i>b</i>&#8242; = 0).  This
     * takes advantage of the fact that the X and Y coordinates are
     * calculated independently by testing alternating positive and
     * negative values in each.
     * <p>
     * <b>Caveat:</b> The calculation breaks down where the coefficients
     * have opposite signs and
     * </p>
     */
    @Test
    public void testNearestQ2GridPointCollapsedBCoefficient() {
        // Re-use mutable TPoints to avoid tens of thousands of allocations.
        MutableTPoint collapsed = new MutableTPoint(0, 0, 0, 0);
        MutableTPoint expected = new MutableTPoint(0, 0, 0, 0);
        // 70 is the limit at which the algorithm appears to work.
        for (int a = 0; a <= 70; a++) {
            for (int b = 1; b <= 70 - a; b ++) {
                // First pass: both positive on X, both negative on Y
                collapsed.setXa((float) (a + b * TPoint.SQRT2));
                collapsed.setYa((float) (-a - b * TPoint.SQRT2));
                expected.setXa(a);
                expected.setXb(b);
                expected.setYa(-a);
                expected.setYb(-b);
                TPoint actual = collapsed.nearestQ2GridPoint();
                assertEquals("Nearest point to " + collapsed,
                        expected, actual);
                // Second pass: a positive, b negative on X;
                //              a negative, b positive on Y.
                collapsed.setXa((float) (a - b * TPoint.SQRT2));
                collapsed.setYa((float) (-a + b * TPoint.SQRT2));
                expected.setXb(-b);
                expected.setYb(b);
                actual = collapsed.nearestQ2GridPoint();
                assertEquals("Nearest point to " + collapsed,
                        expected, actual);
            }
        }
    }

    /**
     * Test {@link TPoint#nearestQ2GridPoint()} where the <i>a</i>
     * coefficient has been collapsed into <i>b</i> (i.e. <i>a</i>&#8242;
     * = 0, <i>b</i>&#8242; = <i>a</i>&#247;&#8730;2&#773; + <i>b</i>).
     * This takes advantage of the fact that the X and Y coordinates are
     * calculated independently by testing alternating positive and
     * negative values in each.
     */
    @Test
    public void testNearestQ2GridPointCollapsedACoefficient() {
        // Re-use mutable TPoints to avoid tens of thousand of allocations.
        MutableTPoint collapsed = new MutableTPoint(0, 0, 0, 0);
        MutableTPoint expected = new MutableTPoint(0, 0, 0, 0);
        // 70 is the limit at which the algorithm appears to work.
        for (int b = 0; b <= 70; b ++) {
            for (int a = 1; a <= 70 - b; a++) {
                // First pass: both positive on X, both negative on Y
                collapsed.setXb((float) (a / TPoint.SQRT2 + b));
                collapsed.setYb((float) (-a / TPoint.SQRT2 - b));
                expected.setXa(a);
                expected.setXb(b);
                expected.setYa(-a);
                expected.setYb(-b);
                TPoint actual = collapsed.nearestQ2GridPoint();
                assertEquals("Nearest point to " + collapsed,
                        expected, actual);
                // Second pass: a positive, b negative on X;
                //              a negative, b positive on Y.
                collapsed.setXb((float) (a / TPoint.SQRT2 - b));
                collapsed.setYb((float) (-a / TPoint.SQRT2 + b));
                expected.setXb(-b);
                expected.setYb(b);
                actual = collapsed.nearestQ2GridPoint();
                assertEquals("Nearest point to " + collapsed,
                        expected, actual);
            }
        }
    }

    /**
     * Test {@link TPoint#nearestQ2GridPoint()} where the coordinates are
     * not on the grid.  Determining the expected value can be tricky
     * here, because the steps of the <i>a</i> and <i>b</i> coefficients
     * are uneven; e.g. 5&#8730;2&#773; is less than
     * <sup>1</sup>&#8260;<sub>14</sub> from the integer 7.
     */
    @Test
    public void testNearestQ2GridPointNotOnQ2Grid() {
        // Re-use mutable TPoints to avoid hundreds of allocations.
        MutableTPoint inexact = new MutableTPoint(0, 0, 0, 0);
        MutableTPoint[] candidates = new MutableTPoint[1 << 4];
        for (int i = 0; i < candidates.length; i++)
            candidates[i] = new MutableTPoint(0, 0, 0, 0);
        // Repeat this test a sufficient number of times
        // to establish confidence in the algorithm.
        for (int i = 0; i < 10000; i++) {
            float testXa = (float) (Math.random() * 70);
            float testXb = (float) (Math.random()
                    * (70 - testXa * TPoint.SQRT2));
            float testYa = (float) (Math.random() * 70);
            float testYb = (float) (Math.random()
                    * (70 - testXa * TPoint.SQRT2));
            inexact.setXa(testXa);
            inexact.setXb(testXb);
            inexact.setYa(testYa);
            inexact.setYb(testYb);
            /*
             * Do our own crude search by taking the floor and ceiling of
             * each coefficient.  This naive approach will *not* find the
             * best on-grid point; this merely serves to set the upper
             * bounds of an acceptable result.
             */
            int floorXa = (int) Math.floor(testXa);
            int ceilXa = (int) Math.ceil(testXa);
            int floorXb = (int) Math.floor(testXb);
            int ceilXb = (int) Math.ceil(testXb);
            int floorYa = (int) Math.floor(testYa);
            int ceilYa = (int) Math.ceil(testYa);
            int floorYb = (int) Math.floor(testYb);
            int ceilYb = (int) Math.ceil(testYb);
            MutableTPoint bestCandidate = null;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (int bits = 0; bits < candidates.length; bits++) {
                candidates[bits].setXa((bits & 1) == 0 ? floorXa : ceilXa);
                candidates[bits].setXb((bits & 2) == 0 ? floorXb : ceilXb);
                candidates[bits].setYa((bits & 4) == 0 ? floorYa : ceilYa);
                candidates[bits].setYb((bits & 8) == 0 ? floorYb : ceilYb);
                double d = inexact.distanceTo(candidates[bits]);
                if (d < bestDistance) {
                    bestCandidate = candidates[bits];
                    bestDistance = d;
                }
            }
            // Now check what the algorithm produced;
            // it may be better than any of our choices.
            TPoint actual = inexact.nearestQ2GridPoint();
            if (actual.equals(bestCandidate)) {
                // Our crude guess was right;
                continue;
            }
            // Check whether the actual result is better than our own guess.
            double actualDistance = inexact.distanceTo(actual);
            if (actualDistance <= bestDistance) {
                // If desired, log the result for mathematical interest.
                // Practically all iterations will not match our crude guess.
//                System.out.println(String.format(Locale.US,
//                        "Nearest point algorithm to %s found %s (at distance %f);"
//                                + " our expected result was %s (at distance %f)",
//                        inexact, actual, actualDistance,
//                        bestCandidate, bestDistance));
                continue;
            }
            // If the actual result is worse than our guess,
            // show our best guess as the expected result.
            fail(String.format(Locale.US, "Nearest point to %s expected:%s"
                    + " (at distance %f) but was:%s (at distance %f)",
                    inexact, bestCandidate, bestDistance,
                    actual, actualDistance));
        }
    }

}
