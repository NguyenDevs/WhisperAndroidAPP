package com.nguyendevs.stkh.Manager;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.nguyendevs.stkh.Database.DatabaseHelper;
import com.nguyendevs.stkh.MainActivity;
import com.nguyendevs.stkh.R;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

public class HistoryManager {
    private final Context context;
    private final TextToSpeechManager textToSpeechManager;
    private String editingLang = "unknown";
    private final DatabaseHelper dbHelper;
    private final ListView historyListView;
    private final EditText txtResult;
    private final ImageView saveButton;
    private final ArrayList<HistoryItem> historyList;
    private final CustomHistoryAdapter historyAdapter;
    private final DrawerLayout drawerLayout;
    private final PermissionManager permissionManager;
    private final TranslateManager translateManager;

    public HistoryManager(TextToSpeechManager textToSpeechManager, TranslateManager translateManager, Context context, DatabaseHelper dbHelper, ListView historyListView, EditText txtResult, ImageView saveButton) {
        this.textToSpeechManager = textToSpeechManager;
        this.translateManager = translateManager;
        this.context = context;
        this.dbHelper = dbHelper;
        this.historyListView = historyListView;
        this.txtResult = txtResult;
        this.saveButton = saveButton;
        this.drawerLayout = ((MainActivity) context).findViewById(R.id.drawer_layout);
        this.historyList = new ArrayList<>();
        this.historyAdapter = new CustomHistoryAdapter(context, historyList);
        this.permissionManager = new PermissionManager(context);
        historyListView.setAdapter(historyAdapter);
        historyListView.setOnItemClickListener((parent, view, position, id) -> {
            HistoryItem item = historyList.get(position);
            showHistoryPopup(item.getContent(), item.getDate(), item.getLang());
            drawerLayout.closeDrawer(GravityCompat.START); }); }
    public void loadHistory() {
        historyList.clear();
        Cursor cursor = dbHelper.getAllHistory();
        if (cursor.moveToFirst()) {
            do {
                String content = cursor.getString(cursor.getColumnIndex("content"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                String lang = cursor.getColumnIndex("lang") != -1 ?
                        cursor.getString(cursor.getColumnIndex("lang")) : "unknown";
                historyList.add(new HistoryItem(content, date, lang));
            } while (cursor.moveToNext()); }
        cursor.close();
        historyAdapter.notifyDataSetChanged(); }
    public void saveEditedContent() {
        translateManager.resetOriginalTextSnapshot();
        String editedContent = txtResult.getText().toString().trim();
        if (!editedContent.isEmpty()) {
            dbHelper.addHistory(editedContent, editingLang);
            loadHistory();
            Toast.makeText(context, "Đã lưu nội dung chỉnh sửa!", Toast.LENGTH_SHORT).show();
            ((MainActivity) context).findViewById(R.id.btnRepeat).setVisibility(View.VISIBLE);
            ((MainActivity) context).findViewById(R.id.btnPause).setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(context, "Nội dung trống, không thể lưu!", Toast.LENGTH_SHORT).show(); }
        txtResult.setFocusable(false);
        txtResult.setFocusableInTouchMode(false);
        txtResult.setCursorVisible(false);
        txtResult.setLongClickable(false);
        txtResult.setKeyListener(null);
        saveButton.setVisibility(View.GONE); }
    private void showHistoryPopup(String content, String timestamp, String lang) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Chi tiết");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String formattedDate = sdf.format(new Date(Long.parseLong(timestamp)));
        String message = content + "\n\nNgôn ngữ: " + lang + "\nLưu lúc: " + formattedDate;
        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show(); }
    public void destroy() {
        if (dbHelper != null) {
            dbHelper.close(); } }
    private static class HistoryItem {
        private final String content;
        private final String date;
        private final String lang;
        public HistoryItem(String content, String date, String lang) {
            this.content = content;
            this.date = date;
            this.lang = lang; }
        public String getContent() {
            return content; }
        public String getDate() {
            return date; }
        public String getLang() {
            return lang; } }
    private class CustomHistoryAdapter extends ArrayAdapter<HistoryItem> {
        public CustomHistoryAdapter(Context context, ArrayList<HistoryItem> items) {
            super(context, 0, items); }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.history_item_layout, parent, false); }
            HistoryItem item = getItem(position);
            TextView textView = convertView.findViewById(R.id.history_text);
            ImageView menuButton = convertView.findViewById(R.id.menu_button);
            String content = item.getContent();
            String displayText = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            textView.setText(displayText);
            menuButton.setOnClickListener(v -> {
                PopupMenu popupMenu = new PopupMenu(getContext(), menuButton);
                popupMenu.getMenu().add(0, 1, 0, "Copy");
                popupMenu.getMenu().add(0, 2, 0, "Xem");
                popupMenu.getMenu().add(0, 3, 0, "Sửa");
                popupMenu.getMenu().add(0, 4, 0, "Xóa");
                popupMenu.getMenu().add(0, 5, 0, "Xuất");
                popupMenu.setOnMenuItemClickListener(menuItem -> {
                    switch (menuItem.getItemId()) {
                        case 1:
                            ((MainActivity) context).copyToClipboard(item.getContent());
                            return true;
                        case 2:
                            translateManager.resetOriginalTextSnapshot();
                            txtResult.setText(item.getContent());
                            txtResult.setEnabled(true);
                            txtResult.setFocusable(false);
                            txtResult.setFocusableInTouchMode(false);
                            txtResult.setCursorVisible(false);
                            txtResult.setLongClickable(false);
                            txtResult.setKeyListener(null);
                            ((MainActivity) context).findViewById(R.id.btnRepeat).setVisibility(View.VISIBLE);
                            ((MainActivity) context).findViewById(R.id.btnPause).setVisibility(View.VISIBLE);
                            drawerLayout.closeDrawer(GravityCompat.START);
                            editingLang = item.getLang();
                            textToSpeechManager.setLanguage(item.getLang());
                            return true;
                        case 3:
                            translateManager.resetOriginalTextSnapshot();
                            txtResult.setText(item.getContent());
                            txtResult.setEnabled(true);
                            txtResult.setFocusable(true);
                            txtResult.setFocusableInTouchMode(true);
                            txtResult.setCursorVisible(true);
                            txtResult.setLongClickable(true);
                            txtResult.setKeyListener(new EditText(getContext()).getKeyListener());
                            txtResult.requestFocus();
                            saveButton.setVisibility(View.VISIBLE);
                            drawerLayout.closeDrawer(GravityCompat.START);
                            editingLang = item.getLang();
                            textToSpeechManager.setLanguage(item.getLang());
                            return true;
                        case 4:
                            dbHelper.deleteHistory(item.getContent(), item.getDate());
                            loadHistory();
                            Toast.makeText(getContext(), "Đã xóa lịch sử!", Toast.LENGTH_SHORT).show();
                            return true;
                        case 5:
                            permissionManager.checkWriteStoragePermission(() -> showExportFormatDialog(item.getContent(), item.getDate()));
                            return true;
                        default:
                            return false; } });
                popupMenu.show(); });
            return convertView; }
        private void showExportFormatDialog(String content, String timestamp) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Chọn định dạng xuất file?");
            String[] formats = {"Text (.txt)", "Word (.docx)"};
            builder.setItems(formats, (dialog, which) -> {
                String fileName = "History_" + timestamp + (which == 0 ? ".txt" : ".docx");
                File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!directory.exists()) {
                    directory.mkdirs(); }
                File file = new File(directory, fileName);
                if (which == 0) {
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(content.getBytes());
                        Toast.makeText(getContext(), "Đã xuất file: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Lỗi khi xuất file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (SecurityException e) {
                        Toast.makeText(getContext(), "Quyền ghi bộ nhớ bị từ chối!", Toast.LENGTH_LONG).show();
                    } } else {
                    XWPFDocument document = new XWPFDocument();
                    XWPFParagraph paragraph = document.createParagraph();
                    XWPFRun run = paragraph.createRun();
                    run.setText(content);
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        document.write(fos);
                        Toast.makeText(getContext(), "Đã xuất file: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        Toast.makeText(getContext(), "Lỗi khi xuất file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    } catch (SecurityException e) {
                        Toast.makeText(getContext(), "Quyền ghi bộ nhớ bị từ chối!", Toast.LENGTH_LONG).show();
                    } finally {
                        try {
                            document.close();
                        } catch (IOException e) {
                            e.printStackTrace(); } } } });
            builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
            builder.show(); } } }