package com.example.simplefiles;

import android.graphics.RectF;
import java.util.List;

/**
 * Plain data object representing a single annotation on a PDF page.
 * Used as an intermediate between the drawing layer and iText7 embedding.
 */
public class AnnotationData {

    public final AnnotationType type;
    public final int            pageIndex;   // 0-based page number

    // Used by HIGHLIGHT and TEXT_NOTE (bounding box in PDF page coordinates)
    public final RectF          rect;

    // Used by FREEHAND (list of points as alternating x,y in PDF page coordinates)
    public final List<Float>    inkPoints;

    // Used by TEXT_NOTE
    public final String         noteText;

    // Colour as ARGB int (e.g. 0xFFFFFF00 = opaque yellow)
    public final int            color;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Highlight annotation */
    public AnnotationData(int pageIndex, RectF rect, int color) {
        this.type       = AnnotationType.HIGHLIGHT;
        this.pageIndex  = pageIndex;
        this.rect       = rect;
        this.inkPoints  = null;
        this.noteText   = null;
        this.color      = color;
    }

    /** Freehand ink annotation */
    public AnnotationData(int pageIndex, List<Float> inkPoints, int color) {
        this.type       = AnnotationType.FREEHAND;
        this.pageIndex  = pageIndex;
        this.rect       = null;
        this.inkPoints  = inkPoints;
        this.noteText   = null;
        this.color      = color;
    }

    /** Text note annotation */
    public AnnotationData(int pageIndex, RectF rect, String noteText, int color) {
        this.type       = AnnotationType.TEXT_NOTE;
        this.pageIndex  = pageIndex;
        this.rect       = rect;
        this.inkPoints  = null;
        this.noteText   = noteText;
        this.color      = color;
    }
}