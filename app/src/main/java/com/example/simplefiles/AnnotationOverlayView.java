package com.example.simplefiles;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Transparent overlay View drawn on top of the PDF page image.
 *
 * Responsibilities:
 *  - Capture touch input
 *  - Draw in-progress annotations live
 *  - Hold completed strokes / rects until the user saves
 *  - Convert screen coordinates → PDF page coordinates for AnnotationData
 *
 * Coordinate system:
 *  Screen coords  → this View's pixel space (origin top-left, y down)
 *  PDF coords     → PDF point space (origin bottom-left, y up)
 *  The conversion uses pdfPageWidthPt / pdfPageHeightPt set by the Activity.
 */
public class AnnotationOverlayView extends View {

    // ── Structs ────────────────────────────────────────────────────────────────

    private static class DrawnStroke {
        Path  path;
        Paint paint;
        DrawnStroke(Path p, Paint paint) { this.path = p; this.paint = paint; }
    }

    private static class DrawnRect {
        RectF  rect;
        Paint  paint;
        boolean isNote;
        String  noteText;
        DrawnRect(RectF r, Paint paint, boolean isNote, String note) {
            this.rect = r; this.paint = paint;
            this.isNote = isNote; this.noteText = note;
        }
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private AnnotationType   mode        = AnnotationType.FREEHAND;
    private int              strokeColor = Color.RED;
    private float            strokeWidth = 6f;

    // Completed annotations (drawn each frame)
    private final List<DrawnStroke> strokes = new ArrayList<>();
    private final List<DrawnRect>   rects   = new ArrayList<>();

    // In-progress
    private Path   currentPath;
    private RectF  currentRect;
    private float  startX, startY;
    private final List<Float> currentInkPoints = new ArrayList<>();

    // PDF coordinate mapping (set by PdfViewerActivity after page is rendered)
    private float pdfPageWidthPt  = 595f;  // A4 default
    private float pdfPageHeightPt = 842f;
    private int   currentPageIndex = 0;

    // Listener called when user lifts finger (annotation complete)
    public interface OnAnnotationCompleteListener {
        void onAnnotationComplete(AnnotationData data);
    }
    private OnAnnotationCompleteListener listener;

    // ── Constructors ───────────────────────────────────────────────────────────

    public AnnotationOverlayView(Context context) { super(context); init(); }
    public AnnotationOverlayView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        setWillNotDraw(false);
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public void setMode(AnnotationType mode)   { this.mode = mode; }
    public void setStrokeColor(int color)       { this.strokeColor = color; }
    public void setStrokeWidth(float width)     { this.strokeWidth = width; }
    public void setCurrentPageIndex(int index)  { this.currentPageIndex = index; }
    public void setOnAnnotationCompleteListener(OnAnnotationCompleteListener l) { this.listener = l; }

    /** Call this after a PDF page is rendered so coordinate mapping is accurate. */
    public void setPdfPageDimensions(float widthPt, float heightPt) {
        this.pdfPageWidthPt  = widthPt;
        this.pdfPageHeightPt = heightPt;
    }

    /** Remove all drawn annotations from the overlay (does NOT undo saved ones). */
    public void clearOverlay() {
        strokes.clear();
        rects.clear();
        currentPath = null;
        currentRect = null;
        currentInkPoints.clear();
        invalidate();
    }

    // ── Touch handling ─────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (mode) {
            case FREEHAND:  handleFreehand(event); break;
            case HIGHLIGHT: handleRect(event, false); break;
            case TEXT_NOTE: handleRect(event, true);  break;
        }
        return true;
    }

    private void handleFreehand(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                currentInkPoints.clear();
                currentInkPoints.add(screenToPdfX(x));
                currentInkPoints.add(screenToPdfY(y));
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                    currentInkPoints.add(screenToPdfX(x));
                    currentInkPoints.add(screenToPdfY(y));
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (currentPath != null) {
                    strokes.add(new DrawnStroke(currentPath, makePaint(false)));
                    List<Float> points = new ArrayList<>(currentInkPoints);
                    if (listener != null)
                        listener.onAnnotationComplete(
                                new AnnotationData(currentPageIndex, points, strokeColor));
                    currentPath = null;
                    currentInkPoints.clear();
                    invalidate();
                }
                break;
        }
    }

    private void handleRect(MotionEvent event, boolean isNote) {
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = x; startY = y;
                currentRect = new RectF(x, y, x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentRect != null) {
                    currentRect.set(Math.min(startX, x), Math.min(startY, y),
                                    Math.max(startX, x), Math.max(startY, y));
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (currentRect != null && currentRect.width() > 10) {
                    RectF pdfRect = screenRectToPdf(currentRect);
                    if (isNote) {
                        // Show dialog to collect note text
                        RectF finalRect = new RectF(currentRect);
                        rects.add(new DrawnRect(finalRect, makePaint(true), true, "Note"));
                        if (listener != null)
                            listener.onAnnotationComplete(
                                    new AnnotationData(currentPageIndex, pdfRect, "Note", strokeColor));
                    } else {
                        rects.add(new DrawnRect(new RectF(currentRect), makePaint(true), false, null));
                        if (listener != null)
                            listener.onAnnotationComplete(
                                    new AnnotationData(currentPageIndex, pdfRect, strokeColor));
                    }
                    currentRect = null;
                    invalidate();
                }
                break;
        }
    }

    // ── Drawing ────────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw completed strokes
        for (DrawnStroke s : strokes) canvas.drawPath(s.path, s.paint);

        // Draw completed rects
        for (DrawnRect r : rects) canvas.drawRect(r.rect, r.paint);

        // Draw in-progress freehand
        if (currentPath != null) canvas.drawPath(currentPath, makePaint(false));

        // Draw in-progress rect
        if (currentRect != null) canvas.drawRect(currentRect, makePaint(true));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Paint makePaint(boolean isRect) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        if (isRect && mode == AnnotationType.HIGHLIGHT) {
            // Semi-transparent yellow-ish highlight
            p.setStyle(Paint.Style.FILL);
            int base = strokeColor == Color.RED ? Color.YELLOW : strokeColor;
            p.setColor(Color.argb(80, Color.red(base), Color.green(base), Color.blue(base)));
        } else if (isRect && mode == AnnotationType.TEXT_NOTE) {
            p.setStyle(Paint.Style.STROKE);
            p.setColor(strokeColor);
            p.setStrokeWidth(3f);
            p.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 5}, 0));
        } else {
            p.setStyle(Paint.Style.STROKE);
            p.setColor(strokeColor);
            p.setStrokeWidth(strokeWidth);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }
        return p;
    }

    // Screen px → PDF points (origin bottom-left)
    private float screenToPdfX(float screenX) {
        return (screenX / getWidth()) * pdfPageWidthPt;
    }

    private float screenToPdfY(float screenY) {
        // Flip Y: PDF origin is bottom-left
        return pdfPageHeightPt - (screenY / getHeight()) * pdfPageHeightPt;
    }

    private RectF screenRectToPdf(RectF screenRect) {
        return new RectF(
                screenToPdfX(screenRect.left),
                screenToPdfY(screenRect.bottom),  // note: flipped
                screenToPdfX(screenRect.right),
                screenToPdfY(screenRect.top)
        );
    }
}
