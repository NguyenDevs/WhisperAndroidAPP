package com.nguyendevs.stkh.Manager;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.nguyendevs.stkh.MainActivity;
import com.nguyendevs.stkh.R;

import java.io.File;
import java.io.IOException;

public class AudioRecorderManager {
    private static final String TAG = "AudioRecorderManager";
    private final Context context;
    private final EditText txtResult;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private File audioFile;
    private boolean isRecording = false;
    private final ProgressBar progressBar;

    public AudioRecorderManager(Context context, EditText txtResult, ProgressBar progressBar) {
        this.context = context;
        this.progressBar = progressBar;
        this.txtResult = txtResult;
    }
    public void startRecording() {
        if (mediaRecorder == null) {
            mediaRecorder = new MediaRecorder();
        }
        try {
            txtResult.setText("");
            audioFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recording.amr");
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            playSound(R.raw.on);
            String hintText = "Đang ghi âm...";
            progressBar.setVisibility(View.VISIBLE);
            SpannableString spannableHint = new SpannableString(hintText);
            spannableHint.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, hintText.length(), 0);
            txtResult.setHint(spannableHint);
        } catch (IOException e) {
            Toast.makeText(context, "Lỗi khi ghi âm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    public void stopRecordingAndSendToServer(ServerCommunicationManager serverCommunicationManager) {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
                playSound(R.raw.off);
                String hintText = "Đang chuyển đổi nội dung...";
                progressBar.setVisibility(View.GONE);
                SpannableString spannableHint = new SpannableString(hintText);
                spannableHint.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, hintText.length(), 0);
                txtResult.setHint(spannableHint);
                serverCommunicationManager.sendAudioToServer(audioFile);
            } catch (Exception e) {
                Toast.makeText(context, "Lỗi khi dừng ghi âm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                ((MainActivity) context).findViewById(R.id.btnMic).setEnabled(true);
            }
        }
    }
    public boolean isRecording() {
        return isRecording;
    }
    public void playSound(int resId) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = MediaPlayer.create(context, resId);
        mediaPlayer.start();
    }
    public void destroy() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}