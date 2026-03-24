package com.example.simplefiles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class NavItemSettingsDialog extends BottomSheetDialogFragment {

    public interface OnSettingsSavedListener {
        void onSaved(int position);
    }

    private static final String ARG_POSITION = "position";
    private NavItem item;
    private int position;
    private OnSettingsSavedListener listener;

    public static NavItemSettingsDialog newInstance(NavItem item, int position,
                                                    OnSettingsSavedListener listener) {
        NavItemSettingsDialog dialog = new NavItemSettingsDialog();
        dialog.item = item;
        dialog.position = position;
        dialog.listener = listener;
        Bundle args = new Bundle();
        args.putInt(ARG_POSITION, position);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_nav_item_settings, container, false);

        EditText etLabel       = view.findViewById(R.id.etLabel);
        SeekBar  seekTextSize  = view.findViewById(R.id.seekTextSize);
        TextView tvTextSizeVal = view.findViewById(R.id.tvTextSizeVal);
        Switch   switchIcon    = view.findViewById(R.id.switchShowIcon);
        Button   btnSave       = view.findViewById(R.id.btnDialogSave);
        Button   btnCancel     = view.findViewById(R.id.btnDialogCancel);

        // Pre-fill current values
        etLabel.setText(item.getLabel());

        int minSize = 8, maxSize = 24;
        seekTextSize.setMax(maxSize - minSize);
        seekTextSize.setProgress(item.getTextSize() - minSize);
        tvTextSizeVal.setText(item.getTextSize() + "sp");

        switchIcon.setChecked(item.isShowIcon());

        // Live text size preview
        seekTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                tvTextSizeVal.setText((progress + minSize) + "sp");
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        btnSave.setOnClickListener(v -> {
            String newLabel = etLabel.getText().toString().trim();
            if (!newLabel.isEmpty()) item.setLabel(newLabel);
            item.setTextSize(seekTextSize.getProgress() + minSize);
            item.setShowIcon(switchIcon.isChecked());
            if (listener != null) listener.onSaved(position);
            dismiss();
        });

        btnCancel.setOnClickListener(v -> dismiss());

        return view;
    }
}