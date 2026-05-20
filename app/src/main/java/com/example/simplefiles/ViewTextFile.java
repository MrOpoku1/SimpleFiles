package com.example.simplefiles;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewTextFile extends AppCompatActivity {

    private TextView fileContent;
    private float currentTextSizeSp = 14f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.file_view_text);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        fileContent = findViewById(R.id.fileContentText);
        ScrollView scrollView = findViewById(R.id.scrollView);
        DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);

        // ── Top navigation buttons ────────────────────────────────────────
        Button btnBack  = findViewById(R.id.button3);
        Button btnShare = findViewById(R.id.button);
        Button btnMenu  = findViewById(R.id.button2);

        btnBack.setText("Back");
        btnBack.setOnClickListener(v -> finish());

        btnMenu.setText("Menu");
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END))
                drawerLayout.closeDrawer(GravityCompat.END);
            else
                drawerLayout.openDrawer(GravityCompat.END);
        });

        // ── Drawer action buttons ─────────────────────────────────────────
        Button btnFontUp   = findViewById(R.id.button4);
        Button btnFontDown = findViewById(R.id.button5);
        Button btnScrollTop    = findViewById(R.id.button6);
        Button btnScrollBottom = findViewById(R.id.button7);
        Button btnCloseMenu    = findViewById(R.id.button8);

        btnFontUp.setText("Font +");
        btnFontDown.setText("Font -");
        btnScrollTop.setText("↑ Top");
        btnScrollBottom.setText("↓ Bottom");
        btnCloseMenu.setText("Close");

        btnFontUp.setOnClickListener(v -> {
            currentTextSizeSp = Math.min(currentTextSizeSp + 2f, 30f);
            fileContent.setTextSize(currentTextSizeSp);
        });
        btnFontDown.setOnClickListener(v -> {
            currentTextSizeSp = Math.max(currentTextSizeSp - 2f, 8f);
            fileContent.setTextSize(currentTextSizeSp);
        });
        btnScrollTop.setOnClickListener(v -> scrollView.smoothScrollTo(0, 0));
        btnScrollBottom.setOnClickListener(v ->
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN)));
        btnCloseMenu.setOnClickListener(v -> drawerLayout.closeDrawer(GravityCompat.END));

        // ── File path ─────────────────────────────────────────────────────
        String filepath = getIntent().getStringExtra("FILE_PATH");

        // Share button needs the filepath
        btnShare.setText("Share");
        btnShare.setOnClickListener(v -> shareFile(filepath));

        if (filepath == null) return;

        File file = new File(filepath);
        String name = file.getName().toLowerCase();

        if (name.endsWith(".pdf")) {
            // --- PDF Rendering ---
            fileContent.setVisibility(android.view.View.GONE); // hide text view

            executor.execute(() -> {
                try {
                    ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                            file, ParcelFileDescriptor.MODE_READ_ONLY);
                    PdfRenderer renderer = new PdfRenderer(pfd);
                    int pageCount = renderer.getPageCount();

                    handler.post(() -> {
                        LinearLayout container = new LinearLayout(this);
                        container.setOrientation(LinearLayout.VERTICAL);
                        scrollView.removeAllViews();
                        scrollView.addView(container);

                        // Render pages on background thread, post each to UI
                        executor.execute(() -> {
                            for (int i = 0; i < pageCount; i++) {
                                PdfRenderer.Page page = renderer.openPage(i);

                                // Scale to screen width
                                int screenWidth = getResources().getDisplayMetrics().widthPixels - 64;
                                int pageHeight = (int) ((float) page.getHeight() / page.getWidth() * screenWidth);

                                Bitmap bitmap = Bitmap.createBitmap(screenWidth, pageHeight, Bitmap.Config.ARGB_8888);
                                bitmap.eraseColor(android.graphics.Color.WHITE); // white background
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                                page.close();

                                final Bitmap finalBitmap = bitmap;
                                handler.post(() -> {
                                    ImageView pageView = new ImageView(this);
                                    pageView.setImageBitmap(finalBitmap);
                                    pageView.setAdjustViewBounds(true);

                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    params.setMargins(0, 0, 0, 16); // spacing between pages
                                    pageView.setLayoutParams(params);
                                    container.addView(pageView);
                                });
                            }
                            try { renderer.close(); pfd.close(); } catch (IOException ignored) {}
                        });
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png") || name.endsWith(".webp")
                || name.endsWith(".gif")) {
            // --- Image Rendering ---
            fileContent.setVisibility(android.view.View.GONE);

            executor.execute(() -> {
                Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(filepath);
                handler.post(() -> {
                    ImageView imageView = new ImageView(this);
                    imageView.setImageBitmap(bitmap);
                    imageView.setAdjustViewBounds(true);
                    imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    scrollView.removeAllViews();
                    scrollView.addView(imageView);
                });
            });

        } else {
            // --- Plain Text (your existing logic) ---
            executor.execute(() -> {
                try (FileInputStream fis = new FileInputStream(file)) {
                    StringBuilder sb = new StringBuilder();
                    int content;
                    while ((content = fis.read()) != -1) {
                        sb.append((char) content);
                    }
                    String finalResult = sb.toString();
                    handler.post(() -> fileContent.setText(finalResult));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void shareFile(String filepath) {
        if (filepath == null) {
            Toast.makeText(this, "No file to share", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            File file = new File(filepath);
            Uri uri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("*/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share file"));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot share this file", Toast.LENGTH_SHORT).show();
        }
    }
}