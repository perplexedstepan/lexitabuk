package com.example.lexitabuk;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionToRecordAccepted = false;
    private final String[] permissions = {Manifest.permission.RECORD_AUDIO};

    private Button recordButton;
    private TextView statusTextView, phraseTextView;
    private ListView phrasesListView;
    private MediaRecorder recorder;
    private MediaPlayer player;
    private String fileName;
    private boolean isRecording = false;
    private TextToSpeech tts;
    private ArrayList<String> phraseList;
    private JSONArray phrasesJsonArray;
    private ArrayList<String> topicsList;
    private ArrayList<String> classList; // New list for classes

    // Firebase Analytics instance
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        recordButton = findViewById(R.id.button_record);
        Button listenButton = findViewById(R.id.button_listen);
        Button selectTopicButton = findViewById(R.id.button_select_topic);
        // Added selectClassButton
        Button selectClassButton = findViewById(R.id.button_select_class); // Initialize the selectClassButton
        phrasesListView = findViewById(R.id.listview_phrases);

        statusTextView = findViewById(R.id.textview_status);
        phraseTextView = findViewById(R.id.textview_phrase);

        fileName = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath() + "/audiorecordtest.3gp";
        phraseList = new ArrayList<>();
        topicsList = new ArrayList<>();
        classList = new ArrayList<>();

        loadClasses(); // Load classes

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, phraseList);
        phrasesListView.setAdapter(adapter);

        selectClassButton.setOnClickListener(v -> showClassSelectionDialog()); // Set click listener for selectClassButton

        showClassSelectionDialog(); // Show class selection on start

        phrasesListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPhrase = phraseList.get(position);
            phraseTextView.setText(selectedPhrase);
            speak(selectedPhrase);

            // Log event to Firebase Analytics
            Bundle params = new Bundle();
            params.putString("selected_phrase", selectedPhrase);
            mFirebaseAnalytics.logEvent("select_phrase", params);
        });

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });

        recordButton.setOnClickListener(v -> {
            Bundle analyticsBundle = new Bundle();

            if (isRecording) {
                stopRecording();
                analyticsBundle.putString("event_type", "stop_recording");
            } else {
                startRecording();
                analyticsBundle.putString("event_type", "start_recording");
            }

            // Log the event in Firebase Analytics
            mFirebaseAnalytics.logEvent("record_button_pressed", analyticsBundle);

            isRecording = !isRecording;
        });

        listenButton.setOnClickListener(v -> startPlaying());

        selectTopicButton.setOnClickListener(v -> showTopicSelectionDialog());
    }

    private void loadClasses() {
        classList.add("ELS 1101");
        classList.add("ELS 1103");
        classList.add("ELS 1104");
    }

    private void showClassSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Your Class");
        CharSequence[] classesArray = classList.toArray(new CharSequence[0]);
        builder.setItems(classesArray, (dialog, which) -> {
            String selectedClass = classList.get(which);

            // Log class selection to Firebase
            Bundle classParams = new Bundle();
            classParams.putString("selected_class", selectedClass);
            mFirebaseAnalytics.logEvent("select_class", classParams);

            loadJsonData(selectedClass); // Load JSON data based on selected class
        });
        builder.show();
    }

    private void loadJsonData(String selectedClass) {
        topicsList.clear(); // Clear existing topics
        String jsonFileName = selectedClass.replace("ELS ", "") + ".json"; // Construct JSON file name
        try {
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open(jsonFileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            phrasesJsonArray = new JSONArray(json);

            for (int i = 0; i < phrasesJsonArray.length(); i++) {
                JSONObject topicObject = phrasesJsonArray.getJSONObject(i);
                topicsList.add(topicObject.getString("topic"));
            }
        } catch (IOException | JSONException ex) {
            ex.printStackTrace();
        }
    }

    private void showTopicSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Topic");
        CharSequence[] topicsArray = topicsList.toArray(new CharSequence[0]);
        builder.setItems(topicsArray, (dialog, which) -> updatePhrasesList(which));
        builder.show();
    }

    private void updatePhrasesList(int topicIndex) {
        try {
            phraseList.clear();
            JSONObject topicObject = phrasesJsonArray.getJSONObject(topicIndex);
            JSONArray phrasesArray = topicObject.getJSONArray("phrases");

            for (int i = 0; i < phrasesArray.length(); i++) {
                phraseList.add(phrasesArray.getString(i));
            }

            ArrayAdapter<String> adapter = (ArrayAdapter<String>) phrasesListView.getAdapter();
            adapter.notifyDataSetChanged();

            // Log topic selection to Firebase Analytics
            String selectedTopic = topicObject.getString("topic");
            Bundle topicParams = new Bundle();
            topicParams.putString("selected_topic", selectedTopic);
            mFirebaseAnalytics.logEvent("select_topic", topicParams);
        } catch (JSONException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted) finish();
    }

    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e("AudioRecordTest", "prepare() failed");
        }

        recorder.start();
        statusTextView.setText("Recording Started");
        recordButton.setText("Stop Recording"); // Change text to Stop Recording
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
        statusTextView.setText("Recording Stopped");
        recordButton.setText("Record"); // Change text back to Record
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
            statusTextView.setText("Playing Audio");
        } catch (IOException e) {
            Log.e("AudioPlayTest", "prepare() failed");
        }
    }

    private void speak(String text) {
        if (text == null || text.isEmpty()) {
            statusTextView.setText("No text to speak");
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        statusTextView.setText("Speaking");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }

        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
