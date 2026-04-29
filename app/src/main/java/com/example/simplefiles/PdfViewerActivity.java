package com.example.simplefiles;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PdfViewerActivity
 *
 * Displays a PDF page-by-page using Android's built-in PdfRenderer.
 * Overlays AnnotationOverlayView for touch-based annotation.
 * Saves annotations into the PDF using iText7 via PdfAnnotationWriter.
 *
 * Intent extras:
 *   "file_path" (String) — absolute path to the PDF file
 *
 * Layout: activity_pdf_viewer.xml
 */
public class PdfViewerActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────────────────
    private ImageView             ivPage;
    private AnnotationOverlayView overlayView;
    private TextView              tvPageInfo;
    private ImageButton           btnPrev, btnNext;
    private ImageButton           btnHighlight, btnFreehand, btnNote;
    private ImageButton           btnColorYellow, btnColorRed, btnColorBlue;
    private ImageButton           btnSave, btnUndo;
    private View                  toolbarAnnotation;

    // ── PDF state ──────────────────────────────────────────────────────────────
    private PdfRenderer            renderer;
    private PdfRenderer.Page       currentPage;
    private ParcelFileDescriptor   fileDescriptor;
    private int                    pageIndex    = 0;
    private int                    totalPages   = 0;
    private String                 filePath;

    // ── Annotation state ───────────────────────────────────────────────────────
    private final List<AnnotationData> pendingAnnotations = new ArrayList<>();
    private AnnotationType             activeMode = AnnotationType.FREEHAND;
    private int                        activeColor = Color.RED;

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewerbetter);

        filePath = getIntent().getStringExtra("file_path");
        if (filePath == null) { finish(); return; }

        bindViews();
        setupAnnotationOverlay();
        setupToolbarButtons();
        openPdf();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closePdf();
        ioExecutor.shutdown();
    }

    // ── PDF rendering ──────────────────────────────────────────────────────────

    private void openPdf() {
        try {
            File file = new File(filePath);
            fileDescriptor = ParcelFileDescriptor.open(file,
                    ParcelFileDescriptor.MODE_READ_ONLY);
            renderer   = new PdfRenderer(fileDescriptor);
            totalPages = renderer.getPageCount();
            renderPage(0);
        } catch (IOException e) {
            Toast.makeText(this, "Could not open PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void renderPage(int index) {
        if (index < 0 || index >= totalPages) return;

        if (currentPage != null) currentPage.close();
        currentPage = renderer.openPage(index);
        pageIndex   = index;

        // Scale page to view width
        int viewWidth  = ivPage.getWidth() > 0 ? ivPage.getWidth() : 1080;
        int viewHeight = (int) (viewWidth * currentPage.getHeight()
                                          / (float) currentPage.getWidth());

        Bitmap bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        ivPage.setImageBitmap(bitmap);
        tvPageInfo.setText((pageIndex + 1) + " / " + totalPages);

        // Update overlay coordinate mapping
        overlayView.setPdfPageDimensions(currentPage.getWidth(), currentPage.getHeight());
        overlayView.setCurrentPageIndex(pageIndex);
        overlayView.clearOverlay();

        btnPrev.setEnabled(pageIndex > 0);
        btnNext.setEnabled(pageIndex < totalPages - 1);
    }

    private void closePdf() {
        if (currentPage != null)    { currentPage.close();    currentPage = null; }
        if (renderer != null)       { renderer.close();       renderer = null; }
        if (fileDescriptor != null) {
            try { fileDescriptor.close(); } catch (IOException ignored) {}
            fileDescriptor = null;
        }
    }

    // ── Annotation overlay setup ───────────────────────────────────────────────

    private void setupAnnotationOverlay() {
        overlayView.setMode(activeMode);
        overlayView.setStrokeColor(activeColor);
        overlayView.setOnAnnotationCompleteListener(annotation -> {
            if (annotation.type == AnnotationType.TEXT_NOTE) {
                // Intercept text notes to show the input dialog
                showNoteDialog(annotation);
            } else {
                pendingAnnotations.add(annotation);
            }
        });
    }

    // ── Toolbar button setup ───────────────────────────────────────────────────

    private void setupToolbarButtons() {
        // Mode buttons
        btnHighlight.setOnClickListener(v -> setMode(AnnotationType.HIGHLIGHT, btnHighlight));
        btnFreehand .setOnClickListener(v -> setMode(AnnotationType.FREEHAND,  btnFreehand));
        btnNote     .setOnClickListener(v -> setMode(AnnotationType.TEXT_NOTE, btnNote));

        // Color buttons
        btnColorYellow.setOnClickListener(v -> setColor(Color.YELLOW));
        btnColorRed   .setOnClickListener(v -> setColor(Color.RED));
        btnColorBlue  .setOnClickListener(v -> setColor(Color.BLUE));

        // Navigation
        btnPrev.setOnClickListener(v -> renderPage(pageIndex - 1));
        btnNext.setOnClickListener(v -> renderPage(pageIndex + 1));

        // Save
        btnSave.setOnClickListener(v -> saveAnnotations());

        // Undo (removes last pending annotation from list + redraws overlay)
        btnUndo.setOnClickListener(v -> {
            if (!pendingAnnotations.isEmpty()) {
                pendingAnnotations.remove(pendingAnnotations.size() - 1);
                overlayView.clearOverlay();
                Toast.makeText(this, "Undone", Toast.LENGTH_SHORT).show();
            }
        });

        // Default highlight active
        setMode(AnnotationType.HIGHLIGHT, btnHighlight);
        setColor(Color.YELLOW);
    }

    private void setMode(AnnotationType mode, ImageButton activeBtn) {
        activeMode = mode;
        overlayView.setMode(mode);
        // Visual feedback — tint active button
        int active   = getColor(R.color.color_primary);
        int inactive = getColor(android.R.color.darker_gray);
        btnHighlight.setColorFilter(inactive);
        btnFreehand .setColorFilter(inactive);
        btnNote     .setColorFilter(inactive);
        activeBtn   .setColorFilter(active);
    }

    private void setColor(int color) {
        activeColor = color;
        overlayView.setStrokeColor(color);
        // Visual: tint the color buttons to show selection
        btnColorYellow.setAlpha(color == Color.YELLOW ? 1f : 0.4f);
        btnColorRed   .setAlpha(color == Color.RED    ? 1f : 0.4f);
        btnColorBlue  .setAlpha(color == Color.BLUE   ? 1f : 0.4f);
    }

    // ── Note dialog ────────────────────────────────────────────────────────────

    private void showNoteDialog(AnnotationData placeholder) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_text_note, null);
        EditText etNote = dialogView.findViewById(R.id.et_note_text);

        new AlertDialog.Builder(this)
                .setTitle("Add Note")
                .setView(dialogView)
                .setPositiveButton("Add", (d, w) -> {
                    String text = etNote.getText().toString().trim();
                    if (text.isEmpty()) text = "Note";
                    pendingAnnotations.add(new AnnotationData(
                            placeholder.pageIndex,
                            placeholder.rect,
                            text,
                            activeColor));
                    Toast.makeText(this, "Note added", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Save ───────────────────────────────────────────────────────────────────

    private void saveAnnotations() {
        if (pendingAnnotations.isEmpty()) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }

        // Must close PdfRenderer before iText can write the file
        closePdf();

        final List<AnnotationData> toSave = new ArrayList<>(pendingAnnotations);
        final String path = filePath;

        Toast.makeText(this, "Saving…", Toast.LENGTH_SHORT).show();

        ioExecutor.execute(() -> {
            try {
                PdfAnnotationWriter.writeAnnotations(path, toSave);
                runOnUiThread(() -> {
                    pendingAnnotations.clear();
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                    // Re-open the now-annotated PDF
                    openPdf();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    openPdf(); // reopen even on failure
                });
            }
        });
    }

    // ── View binding ───────────────────────────────────────────────────────────

    private void bindViews() {
        ivPage          = findViewById(R.id.iv_pdf_page);
        overlayView     = findViewById(R.id.annotation_overlay);
        tvPageInfo      = findViewById(R.id.tv_page_info);
        btnPrev         = findViewById(R.id.btn_prev);
        btnNext         = findViewById(R.id.btn_next);
        btnHighlight    = findViewById(R.id.btn_highlight);
        btnFreehand     = findViewById(R.id.btn_freehand);
        btnNote         = findViewById(R.id.btn_note);
        btnColorYellow  = findViewById(R.id.btn_color_yellow);
        btnColorRed     = findViewById(R.id.btn_color_red);
        btnColorBlue    = findViewById(R.id.btn_color_blue);
        btnSave         = findViewById(R.id.btn_save);
        btnUndo         = findViewById(R.id.btn_undo);
        toolbarAnnotation = findViewById(R.id.toolbar_annotation);

        // Render after layout so ivPage.getWidth() is valid
        ivPage.post(() -> { if (renderer != null) renderPage(pageIndex); });
    }
}
