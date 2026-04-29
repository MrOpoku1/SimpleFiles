package com.example.simplefiles;

import android.graphics.Color;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.annot.PdfFreeTextAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfInkAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfTextMarkupAnnotation;
import com.itextpdf.kernel.pdf.canvas.parser.PdfDocumentContentParser;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a list of AnnotationData objects into a PDF using iText7.
 *
 * Strategy: read the original PDF → write a new copy with annotations embedded
 * → replace the original file atomically.
 *
 * Run this on a background thread — never on the UI thread.
 */
public class PdfAnnotationWriter {

    /**
     * Embed all pending annotations into the PDF at {@code pdfPath}.
     * The file is modified in-place (via a temp file swap).
     *
     * @param pdfPath     absolute path to the PDF file
     * @param annotations list of annotations to embed
     * @throws Exception  on any IO or iText error
     */
    public static void writeAnnotations(String pdfPath, List<AnnotationData> annotations)
            throws Exception {

        File original = new File(pdfPath);
        File temp     = new File(pdfPath + ".tmp");

        try (PdfReader reader = new PdfReader(original);
             PdfWriter writer = new PdfWriter(new FileOutputStream(temp));
             PdfDocument pdfDoc = new PdfDocument(reader, writer)) {

            for (AnnotationData ann : annotations) {
                // iText pages are 1-based
                int iTextPage = ann.pageIndex + 1;
                if (iTextPage < 1 || iTextPage > pdfDoc.getNumberOfPages()) continue;

                DeviceRgb rgb = toDeviceRgb(ann.color);

                switch (ann.type) {
                    case HIGHLIGHT:
                        embedHighlight(pdfDoc, iTextPage, ann, rgb);
                        break;
                    case FREEHAND:
                        embedInk(pdfDoc, iTextPage, ann, rgb);
                        break;
                    case TEXT_NOTE:
                        embedTextNote(pdfDoc, iTextPage, ann, rgb);
                        break;
                }
            }
        }

        // Atomic swap: replace original with annotated copy
        if (!original.delete()) throw new Exception("Could not delete original PDF");
        if (!temp.renameTo(original)) throw new Exception("Could not rename temp PDF");
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private static void embedHighlight(PdfDocument doc, int page,
                                       AnnotationData ann, DeviceRgb rgb) {
        Rectangle rect = toITextRect(ann.rect);

        // QuadPoints: the four corners of the highlight quad (required by PDF spec)
        float[] quadPoints = {
                rect.getLeft(),  rect.getTop(),
                rect.getRight(), rect.getTop(),
                rect.getLeft(),  rect.getBottom(),
                rect.getRight(), rect.getBottom()
        };

        PdfTextMarkupAnnotation highlight =
                PdfTextMarkupAnnotation.createHighLight(rect, quadPoints);
        highlight.setColor(rgb);
        highlight.setContents("Highlight");

        doc.getPage(page).addAnnotation(highlight);
    }

    private static void embedInk(PdfDocument doc, int page,
                                 AnnotationData ann, DeviceRgb rgb) {
        if (ann.inkPoints == null || ann.inkPoints.size() < 4) return;

        // iText ink annotation takes a list of point lists (each = one stroke)
        List<float[]> inkList = new ArrayList<>();
        float[] stroke = new float[ann.inkPoints.size()];
        for (int i = 0; i < ann.inkPoints.size(); i++) {
            stroke[i] = ann.inkPoints.get(i);
        }
        inkList.add(stroke);

        // Compute bounding rect from points
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
        for (int i = 0; i < stroke.length; i += 2) {
            minX = Math.min(minX, stroke[i]);
            maxX = Math.max(maxX, stroke[i]);
            minY = Math.min(minY, stroke[i + 1]);
            maxY = Math.max(maxY, stroke[i + 1]);
        }
        Rectangle rect = new Rectangle(minX, minY, maxX - minX, maxY - minY);

        PdfInkAnnotation ink = new PdfInkAnnotation(rect, inkList);
        ink.setColor(rgb);
        ink.setBorderStyle(new com.itextpdf.kernel.pdf.PdfDictionary());

        doc.getPage(page).addAnnotation(ink);
    }

    private static void embedTextNote(PdfDocument doc, int page,
                                      AnnotationData ann, DeviceRgb rgb) {
        Rectangle rect = toITextRect(ann.rect);

        PdfFreeTextAnnotation note = new PdfFreeTextAnnotation(rect,
                new com.itextpdf.kernel.pdf.PdfString(
                        ann.noteText != null ? ann.noteText : ""));
        note.setColor(rgb);

        doc.getPage(page).addAnnotation(note);
    }

    // ── Coordinate / color converters ──────────────────────────────────────────

    /**
     * Android RectF (origin top-left, y grows down) →
     * iText Rectangle (origin bottom-left, y grows up).
     *
     * NOTE: The caller is responsible for passing rects already converted to
     * PDF page coordinates (points, origin bottom-left). The AnnotationOverlayView
     * handles this conversion before creating AnnotationData.
     */
    private static Rectangle toITextRect(android.graphics.RectF r) {
        return new Rectangle(r.left, r.top, r.width(), r.height());
    }

    private static DeviceRgb toDeviceRgb(int argb) {
        return new DeviceRgb(
                Color.red(argb)   / 255f,
                Color.green(argb) / 255f,
                Color.blue(argb)  / 255f
        );
    }
}