package com.example.simplefiles;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ── Listener ───────────────────────────────────────────────────────────────

    public interface OnFileClickListener {
        void onFileClick(FileItem item);
        void onFileLongClick(FileItem item);
        void onShareClick(FileItem item);
        void onDeleteClick(FileItem item);
        void onStarClick(FileItem item);
    }

    private static final int VIEW_TYPE_FILE   = 0;
    private static final int VIEW_TYPE_FOLDER = 1;

    // ── State ──────────────────────────────────────────────────────────────────

    private List<FileItem> items = new ArrayList<>();
    private final OnFileClickListener listener;

    public FileAdapter(OnFileClickListener listener) {
        this.listener = listener;
    }

    // ── DiffUtil update ────────────────────────────────────────────────────────

    public void submitList(List<FileItem> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newItems.size(); }

            @Override
            public boolean areItemsTheSame(int o, int n) {
                String oldPath = items.get(o).getPath();
                String newPath = newItems.get(n).getPath();
                if (oldPath == null && newPath == null) return true;
                if (oldPath == null || newPath == null) return false;
                return oldPath.equals(newPath);
            }

            @Override
            public boolean areContentsTheSame(int o, int n) {
                FileItem a = items.get(o), b = newItems.get(n);
                return a.getName().equals(b.getName())
                        && a.getSizeBytes() == b.getSizeBytes()
                        && a.getLastModified() == b.getLastModified();
            }
        });
        items = new ArrayList<>(newItems);
        diff.dispatchUpdatesTo(this);
    }

    // ── Adapter ────────────────────────────────────────────────────────────────

    @Override
    public int getItemViewType(int position) {
        return items.get(position).isDirectory() ? VIEW_TYPE_FOLDER : VIEW_TYPE_FILE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_FOLDER) {
            View v = inflater.inflate(R.layout.item_folder, parent, false);
            return new FolderViewHolder(v);
        }
        View v = inflater.inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        FileItem item = items.get(position);
        if (holder instanceof FolderViewHolder) {
            ((FolderViewHolder) holder).bind(item);
        } else {
            ((FileViewHolder) holder).bind(item);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── FolderViewHolder ───────────────────────────────────────────────────────

    class FolderViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvIcon;
        private final TextView tvName;
        private final TextView tvSubtitle;

        FolderViewHolder(@NonNull View v) {
            super(v);
            tvIcon     = v.findViewById(R.id.tv_folder_icon);
            tvName     = v.findViewById(R.id.tv_folder_name);
            tvSubtitle = v.findViewById(R.id.tv_folder_subtitle);
        }

        void bind(FileItem item) {
            if (item.getPath() == null) {
                // Nav-up sentinel
                tvIcon.setText("↩");
                tvName.setText("Back");
                tvSubtitle.setText("Go up one folder");
            } else {
                tvIcon.setText("📁");
                tvName.setText(item.getName());
                tvSubtitle.setText(item.getFormattedSize());
            }

            itemView.setOnClickListener(v -> { if (listener != null) listener.onFileClick(item); });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onFileLongClick(item);
                return true;
            });
        }
    }

    // ── FileViewHolder ─────────────────────────────────────────────────────────

    class FileViewHolder extends RecyclerView.ViewHolder {

        private final ShapeableImageView thumbnail;
        private final TextView           tvIcon;
        private final TextView           tvName;
        private final TextView           tvDate;
        private final TextView           tvSize;
        private final ImageButton        btnShare;
        private final ImageButton        btnDelete;
        private final ImageButton        btnStar;

        FileViewHolder(@NonNull View v) {
            super(v);
            thumbnail = v.findViewById(R.id.thumbnail);
            tvIcon    = v.findViewById(R.id.tv_icon);
            tvName    = v.findViewById(R.id.tv_name);
            tvDate    = v.findViewById(R.id.tv_date);
            tvSize    = v.findViewById(R.id.tv_size);
            btnShare  = v.findViewById(R.id.btn_share);
            btnDelete = v.findViewById(R.id.btn_delete);
            btnStar   = v.findViewById(R.id.btn_star);
        }

        void bind(FileItem item) {
            tvName.setText(item.getName());
            tvDate.setText(item.getFormattedDate());
            tvSize.setText(item.getFormattedSize());

            if (item.getType() == FileItem.Type.IMAGE && item.getPath() != null) {
                android.graphics.Bitmap bmp = BitmapFactory.decodeFile(item.getPath());
                if (bmp != null) {
                    thumbnail.setVisibility(View.VISIBLE);
                    tvIcon.setVisibility(View.GONE);
                    thumbnail.setImageBitmap(bmp);
                } else {
                    showIcon(item);
                }
            } else {
                showIcon(item);
            }

            itemView.setOnClickListener(v -> { if (listener != null) listener.onFileClick(item); });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onFileLongClick(item);
                return true;
            });

            btnShare.setOnClickListener(v  -> { if (listener != null) listener.onShareClick(item); });
            btnDelete.setOnClickListener(v -> { if (listener != null) listener.onDeleteClick(item); });
            btnStar.setOnClickListener(v   -> { if (listener != null) listener.onStarClick(item); });
        }

        private void showIcon(FileItem item) {
            thumbnail.setVisibility(View.GONE);
            tvIcon.setVisibility(View.VISIBLE);
            tvIcon.setText(item.getIcon());
        }
    }
}
