/*
 * Copyright © 2026 Trevin Beattie
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

import org.junit.Test;

/**
 * Unit tests for {@link TPoint} geometry.
 */
public class TPointTest {

    private static final double DELTA = 1e-4;

    /**
     * Every 45&deg; step of {@link TPoint#coarseRotate(int)} must agree
     * with an analytic rotation of the point&rsquo;s real coordinates.
     * (Guards against per-case transcription errors like the one that once
     * collapsed the 225&deg; case onto the x=y diagonal.)
     */
    @Test
    public void coarseRotateMatchesAnalyticRotation() {
        // An asymmetric point so x != y and the cases can't alias.
        TPoint p = new TPoint(3, 0, 1, 0);
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

    /** A rotation must never collapse a point onto the x=y diagonal. */
    @Test
    public void coarseRotatePreservesDistinctCoordinates() {
        TPoint p = new TPoint(3, 0, 1, 0);
        for (int steps = 0; steps < 8; steps++) {
            TPoint rotated = p.coarseRotate(steps);
            double radius = Math.hypot(rotated.getX(), rotated.getY());
            assertEquals("radius preserved after " + steps + " steps",
                    Math.hypot(p.getX(), p.getY()), radius, DELTA);
        }
    }
}
