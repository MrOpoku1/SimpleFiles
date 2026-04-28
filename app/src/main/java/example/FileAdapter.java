package com.example.simplefiles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the file browser.
 *
 * Uses DiffUtil so only changed rows are redrawn — keeps scrolling smooth
 * even when the list is updated after every search keystroke.
 *
 * Each item displays:
 *   • emoji icon (file type)
 *   • file name
 *   • file size + last-modified date
 *
 * Layouts referenced:
 *   R.layout.item_file      — root item layout
 *   R.id.tv_icon            — TextView showing the emoji icon
 *   R.id.tv_name            — TextView showing the file name
 *   R.id.tv_meta            — TextView showing size + date
 */
public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    // ── Listener ───────────────────────────────────────────────────────────────

    public interface OnFileClickListener {
        void onFileClick(FileItem item);
        void onFileLongClick(FileItem item);
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private List<FileItem> items = new ArrayList<>();
    private final OnFileClickListener listener;

    public FileAdapter(OnFileClickListener listener) {
        this.listener = listener;
    }

    // ── Data update (DiffUtil) ─────────────────────────────────────────────────

    public void submitList(List<FileItem> newItems) {
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override public int getOldListSize() { return items.size(); }
            @Override public int getNewListSize() { return newItems.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                // Use path as a stable ID (unique per file on device)
                return items.get(oldPos).getPath().equals(newItems.get(newPos).getPath());
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                FileItem o = items.get(oldPos);
                FileItem n = newItems.get(newPos);
                return o.getName().equals(n.getName())
                        && o.getSizeBytes() == n.getSizeBytes()
                        && o.getLastModified() == n.getLastModified();
            }
        });

        items = new ArrayList<>(newItems);
        diff.dispatchUpdatesTo(this);
    }

    // ── RecyclerView.Adapter ───────────────────────────────────────────────────

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolder ─────────────────────────────────────────────────────────────

    class FileViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvIcon;
        private final TextView tvName;
        private final TextView tvMeta;

        FileViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_icon);
            tvName = itemView.findViewById(R.id.tv_name);
            tvMeta = itemView.findViewById(R.id.tv_meta);
        }

        void bind(FileItem item) {
            tvIcon.setText(item.getIcon());
            tvName.setText(item.getName());

            // e.g. "2.4 MB  •  Mar 5, 2024"
            String meta = item.isDirectory()
                    ? item.getFormattedDate()
                    : item.getFormattedSize() + "  •  " + item.getFormattedDate();
            tvMeta.setText(meta);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onFileClick(item);
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onFileLongClick(item);
                return true;
            });
        }
    }
}
