package com.xmission.trevin.android.tangram.data;

import androidx.annotation.Nullable;

/**
 * Container for a Tangram.  This names a puzzle and provides the
 * placement of its pieces.
 */
public class TangramPuzzle {

    protected @Nullable String name;
    /**
     * The larger of width or height of the puzzle.
     * The value must be at least 12, which is the size of the
     * square the Tangram pieces are cut from.
     */
    protected float size = 12;
    protected TangramPiece[] pieces;

    /**
     * Default constructor.  The puzzle is unnamed and will have
     * the seven standard pieces placed outside the bounds.
     */
    public TangramPuzzle() {
        // Set the default bounds of the puzzle
        // to the size of 9 (3×3) compact squares.
        size = 36;
        pieces = new TangramPiece[7];
        pieces[0] = new TangramSmallTriangle();
        pieces[1] = new TangramSmallTriangle();
        pieces[2] = new TangramSquare();
        pieces[3] = new TangramParallelogram();
        pieces[4] = new TangramMediumTriangle();
        pieces[5] = new TangramLargeTriangle();
        pieces[6] = new TangramLargeTriangle();
        for (int i = 0; i < pieces.length; i++)
            pieces[i].setPosition(new TPoint(12*i - 36, 0, 24, 0));
    }

    // To Do: Construct a puzzle from a text description.

    /** @return the number of pieces in the puzzle (normally 7) */
    public int getPieceCount() {
        return pieces.length;
    }

    /**
     * Get one of the pieces in this puzzle.
     *
     * @param index the index of the piece to retrieve.
     *
     * @return the puzzle piece at the given index
     */
    public TangramPiece getPiece(int index) {
        return pieces[index];
    }

    /**
     * @return the size of the puzzle (in Tangram piece units).
     */
    public float getSize() {
        return size;
    }

}
