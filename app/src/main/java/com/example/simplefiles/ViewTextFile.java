package com.example.simplefiles;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewTextFile extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.file_view_text);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        TextView fileContent = findViewById(R.id.fileContentText);
        ScrollView scrollView = findViewById(R.id.scrollView);

        String filepath = getIntent().getStringExtra("FILE_PATH");
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
}