package com.nesworkbench.rebuild;

import com.nesworkbench.graphics.ChrTileEncoder;
import com.nesworkbench.sprite.ImportedArtwork;
import com.nesworkbench.sprite.SpriteComposition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Turns imported artwork for a composition into CHR byte edits. Cells hold original tile indices, so the result is
 * independent of any tile mapping. Only tiles whose pixels actually changed become edits. Anything ambiguous is refused
 * rather than guessed: a tile placed in several cells must look identical in all of them, and an empty cell must stay blank.
 */
public final class ArtworkChrEdits {
    private ArtworkChrEdits() {}

    public static ChrEdits from(SpriteComposition composition, List<int[][]> sourceTiles, ImportedArtwork artwork) throws RebuildException {
        Objects.requireNonNull(composition, "composition must not be null");
        Objects.requireNonNull(sourceTiles, "sourceTiles must not be null");
        Objects.requireNonNull(artwork, "artwork must not be null");
        if (artwork.widthPixels() != composition.widthPixels() || artwork.heightPixels() != composition.heightPixels())
            throw new RebuildException("Artwork is " + artwork.widthPixels() + "x" + artwork.heightPixels() + " pixels, but composition '"
                    + composition.name() + "' is " + composition.widthPixels() + "x" + composition.heightPixels() + ".");
        int maxTile = composition.maxSourceTile();
        if (maxTile >= sourceTiles.size())
            throw new RebuildException("Composition '" + composition.name() + "' references tile " + maxTile + ", but the ROM has " + sourceTiles.size() + " tiles.");

        Map<Integer, int[][]> firstSeen = new HashMap<>();
        Map<Integer, String> firstCell = new HashMap<>();
        Map<Integer, byte[]> edits = new TreeMap<>();
        for (int cy = 0; cy < composition.heightTiles(); cy++) for (int cx = 0; cx < composition.widthTiles(); cx++) {
            int[][] block = artwork.tile(cx, cy);
            int tile = composition.tileAt(cx, cy);
            if (tile == SpriteComposition.EMPTY) {
                for (int[] row : block) for (int v : row)
                    if (v != 0) throw new RebuildException("Cell (" + cx + ", " + cy + ") is empty, so it has no tile to write; pixels drawn there cannot be applied.");
                continue;
            }
            int[][] earlier = firstSeen.get(tile);
            if (earlier == null) {
                firstSeen.put(tile, block); firstCell.put(tile, "(" + cx + ", " + cy + ")");
                if (!Arrays.deepEquals(block, sourceTiles.get(tile))) edits.put(tile, ChrTileEncoder.encode(block));
            } else if (!Arrays.deepEquals(earlier, block)) {
                throw new RebuildException("Tile " + tile + " is placed in cells " + firstCell.get(tile) + " and (" + cx + ", " + cy
                        + ") but the artwork differs between them; one tile can only hold one image.");
            }
        }
        return ChrEdits.of(edits);
    }
}
