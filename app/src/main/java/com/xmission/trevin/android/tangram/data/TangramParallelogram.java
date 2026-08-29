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

import com.xmission.trevin.android.tangram.R;

/**
 * A parallelogram which is the size of two {@link TangramSmallTriangle}s
 * placed with their short sides together with one rotated 180&deg;
 * relative to the other.  Its sides have lengths of 6&#8730;2&#773;
 * on the short edge and 12 on the long edge.  It&rsquo;s default
 * orientation is with the long edge on top and bottom, and angled
 * 45&deg; counter-clockwise where +Y is towards the bottom
 * matching the direction of the display coordinate system.
 *
 * @author Trevin Beattie
 */
public class TangramParallelogram extends TangramPiece {

    public static final String JSON_NAME = "parallelogram";

    public static final TPoint[] VERTICES = new TPoint[] {
            new ImmutableTPoint(-9f, 0, -3f, 0),
            new ImmutableTPoint( 3f, 0, -3f, 0),
            new ImmutableTPoint( 9f, 0,  3f, 0),
            new ImmutableTPoint(-3f, 0,  3f, 0)
    };

    public String getJsonName() {
        return JSON_NAME;
    }

    public boolean canFlip() {
        return true;
    }

    @Override
    protected TPoint[] getShapeVertices() {
        return VERTICES;
    }

    public double getMaxRadius() {
        return 9.486832980505137996;
    }

    @Override
    public int getDrawableId() {
        return R.drawable.tangram_parallelogram;
    }

    @Override
    public int getColorAttr() {
        return R.attr.tangramParallelogramColor;
    }

}
