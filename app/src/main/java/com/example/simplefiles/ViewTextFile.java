package com.example.simplefiles;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ViewTextFile extends AppCompatActivity {

    private static final String NAV_PREFS = "nav_prefs";

    private TextView    fileContent;
    private ScrollView  scrollView;
    private DrawerLayout drawerLayout;
    private String      filepath;
    private float       currentTextSizeSp = 14f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_view_text);

        fileContent  = findViewById(R.id.fileContentText);
        scrollView   = findViewById(R.id.scrollView);
        drawerLayout = findViewById(R.id.drawerLayout);
        filepath     = getIntent().getStringExtra("FILE_PATH");

        // ── Top toolbar buttons ───────────────────────────────────────────
        Button btnBack  = findViewById(R.id.button3);
        Button btnShare = findViewById(R.id.button);
        Button btnMenu  = findViewById(R.id.button2);

        btnBack.setOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> shareFile(filepath));
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END))
                drawerLayout.closeDrawer(GravityCompat.END);
            else
                drawerLayout.openDrawer(GravityCompat.END);
        });

        // ── Build drawer from saved nav prefs ─────────────────────────────
        buildDrawer();

        // ── Load file ─────────────────────────────────────────────────────
        if (filepath == null) return;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler         handler  = new Handler(Looper.getMainLooper());
        File            file     = new File(filepath);
        String          name     = file.getName().toLowerCase();

        if (name.endsWith(".pdf")) {
            fileContent.setVisibility(android.view.View.GONE);
            executor.execute(() -> {
                try {
                    ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                            file, ParcelFileDescriptor.MODE_READ_ONLY);
                    PdfRenderer renderer  = new PdfRenderer(pfd);
                    int         pageCount = renderer.getPageCount();

                    handler.post(() -> {
                        LinearLayout container = new LinearLayout(this);
                        container.setOrientation(LinearLayout.VERTICAL);
                        scrollView.removeAllViews();
                        scrollView.addView(container);

                        executor.execute(() -> {
                            for (int i = 0; i < pageCount; i++) {
                                PdfRenderer.Page page = renderer.openPage(i);
                                int sw = getResources().getDisplayMetrics().widthPixels - 64;
                                int sh = (int) ((float) page.getHeight() / page.getWidth() * sw);
                                Bitmap bmp = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
                                bmp.eraseColor(Color.WHITE);
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                                page.close();
                                final Bitmap finalBmp = bmp;
                                handler.post(() -> {
                                    ImageView pv = new ImageView(this);
                                    pv.setImageBitmap(finalBmp);
                                    pv.setAdjustViewBounds(true);
                                    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT);
                                    p.setMargins(0, 0, 0, 16);
                                    pv.setLayoutParams(p);
                                    container.addView(pv);
                                });
                            }
                            try { renderer.close(); pfd.close(); } catch (IOException ignored) {}
                        });
                    });
                } catch (IOException e) { e.printStackTrace(); }
            });

        } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")
                || name.endsWith(".png")  || name.endsWith(".webp")
                || name.endsWith(".gif")) {
            fileContent.setVisibility(android.view.View.GONE);
            executor.execute(() -> {
                Bitmap bmp = android.graphics.BitmapFactory.decodeFile(filepath);
                handler.post(() -> {
                    ImageView iv = new ImageView(this);
                    iv.setImageBitmap(bmp);
                    iv.setAdjustViewBounds(true);
                    iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    scrollView.removeAllViews();
                    scrollView.addView(iv);
                });
            });

        } else {
            executor.execute(() -> {
                try (FileInputStream fis = new FileInputStream(file)) {
                    StringBuilder sb = new StringBuilder();
                    int ch;
                    while ((ch = fis.read()) != -1) sb.append((char) ch);
                    final String text = sb.toString();
                    handler.post(() -> fileContent.setText(text));
                } catch (IOException e) { e.printStackTrace(); }
            });
        }
    }

    // ── Drawer builder ────────────────────────────────────────────────────

    private void buildDrawer() {
        LinearLayout container = findViewById(R.id.drawerContent);
        container.removeAllViews();

        int marginPx = dp(8);

        for (NavItem item : loadNavItems()) {
            if (!item.isVisible()) continue;

            Button btn = new Button(this);
            btn.setText(item.getLabel());
            btn.setTextSize(item.getTextSize());
            btn.setTextColor(0xFFF7CFD4);       // lightPink
            btn.setBackgroundColor(0xFF3A3736); // dark brown-grey

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, marginPx);
            btn.setLayoutParams(lp);

            String id = item.getId();
            btn.setOnClickListener(v -> executeNavAction(id));
            container.addView(btn);
        }
    }

    // ── Action execution ──────────────────────────────────────────────────

    private void executeNavAction(String id) {
        switch (id) {
            case "font_up":
                currentTextSizeSp = Math.min(currentTextSizeSp + 2f, 32f);
                fileContent.setTextSize(currentTextSizeSp);
                break;
            case "font_down":
                currentTextSizeSp = Math.max(currentTextSizeSp - 2f, 8f);
                fileContent.setTextSize(currentTextSizeSp);
                break;
            case "scroll_top":
                scrollView.smoothScrollTo(0, 0);
                drawerLayout.closeDrawer(GravityCompat.END);
                break;
            case "scroll_bottom":
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                drawerLayout.closeDrawer(GravityCompat.END);
                break;
            case "page_up":
                scrollView.smoothScrollBy(0, -scrollView.getHeight());
                break;
            case "page_down":
                scrollView.smoothScrollBy(0, scrollView.getHeight());
                break;
            case "go_back":
                finish();
                break;
            case "share":
                shareFile(filepath);
                drawerLayout.closeDrawer(GravityCompat.END);
                break;
            case "close_menu":
                drawerLayout.closeDrawer(GravityCompat.END);
                break;
        }
    }

    // ── Nav item loading (mirrors CustomizeNavActivity.loadFromPrefs) ─────

    private List<NavItem> loadNavItems() {
        SharedPreferences prefs = getSharedPreferences(NAV_PREFS, MODE_PRIVATE);
        int count = prefs.getInt("nav_count", -1);
        if (count <= 0) return defaultNavItems();

        List<NavItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String  id      = prefs.getString("nav_id_"       + i, "");
            String  label   = prefs.getString("nav_label_"    + i, "");
            boolean visible = prefs.getBoolean("nav_visible_" + i, true);
            int     size    = prefs.getInt("nav_textsize_"    + i, 13);
            boolean icon    = prefs.getBoolean("nav_icon_"    + i, true);
            NavItem item = new NavItem(id, label, visible);
            item.setTextSize(size);
            item.setShowIcon(icon);
            items.add(item);
        }
        return items;
    }

    // Must stay in sync with CustomizeNavActivity.buildDefaults()
    private static List<NavItem> defaultNavItems() {
        List<NavItem> list = new ArrayList<>();
        list.add(new NavItem("font_up",       "Font +",        true));
        list.add(new NavItem("font_down",     "Font -",        true));
        list.add(new NavItem("scroll_top",    "Scroll to Top", true));
        list.add(new NavItem("scroll_bottom", "Scroll to End", true));
        list.add(new NavItem("page_up",       "Page Up",       true));
        list.add(new NavItem("page_down",     "Page Down",     true));
        list.add(new NavItem("share",         "Share File",    true));
        list.add(new NavItem("go_back",       "Close Viewer",  false));
        list.add(new NavItem("close_menu",    "Close Menu",    false));
        return list;
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void shareFile(String path) {
        if (path == null) {
            Toast.makeText(this, "No file to share", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    new File(path));
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share file"));
        } catch (Exception e) {
            Toast.makeText(this, "Cannot share this file", Toast.LENGTH_SHORT).show();
        }
    }
}
