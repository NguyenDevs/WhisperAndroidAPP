package com.nguyendevs.stkh.Manager;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.EditText;
import android.widget.Toast;

public class UtilityManager {
    private final Context context;
    private final EditText txtResult;

    public UtilityManager(Context context, EditText txtResult) {
        this.context = context;
        this.txtResult = txtResult;
    }
    public void copyToClipboard(String text) {
        if (text != null && !text.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Đã sao chép vào clipboard!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Không có nội dung để sao chép!", Toast.LENGTH_SHORT).show();
        }
    }
    public void shareText() {
        String text = txtResult.getText().toString();
        if (!text.isEmpty()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ qua"));
        } else {
            Toast.makeText(context, "Không có nội dung để chia sẻ!", Toast.LENGTH_SHORT).show();
        }
    }
    public void searchOnGoogle() {
        String query = txtResult.getText().toString().trim();
        if (!query.isEmpty()) {
            if (query.contains(".") && !query.startsWith("http://") && !query.startsWith("https://")) {
                query = "https://" + query;
            } else if (!query.startsWith("http://") && !query.startsWith("https://")) {
                query = "https://www.google.com/search?q=" + Uri.encode(query);
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(query));
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "Không có nội dung để tìm kiếm!", Toast.LENGTH_SHORT).show();
        }
    }
}