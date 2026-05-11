package com.memorymate.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.memorymate.MainActivity;
import com.memorymate.R;
import com.memorymate.models.Person;
import com.memorymate.utils.DatabaseHelper;
import com.memorymate.utils.SharedPrefs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoiceAssistantFragment extends Fragment implements TextToSpeech.OnInitListener {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private Button btnListen;
    private TextView tvStatus, tvCommand;
    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private SharedPrefs sharedPrefs;
    private DatabaseHelper databaseHelper;
    private boolean ttsReady = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voice_assistant, container, false);

        btnListen = view.findViewById(R.id.btn_listen);
        tvStatus = view.findViewById(R.id.tv_status);
        tvCommand = view.findViewById(R.id.tv_command);

        sharedPrefs = SharedPrefs.getInstance(getContext());
        databaseHelper = new DatabaseHelper(getContext());

        textToSpeech = new TextToSpeech(getContext(), this);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());

        btnListen.setOnClickListener(v -> startListening());

        return view;
    }

    private void startListening() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command...");
        try {
            speechRecognizer.startListening(intent);
            tvStatus.setText("Listening...");
            btnListen.setEnabled(false);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show();
            btnListen.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(getContext(), "Microphone permission required", Toast.LENGTH_LONG).show();
                tvStatus.setText("Microphone permission denied.");
            }
        }
    }

    private void processCommand(String command) {
        command = command.toLowerCase().trim();
        tvCommand.setText("You said: " + command);
        speak("Processing...");

        // Debug: show what was heard
        Toast.makeText(getContext(), "Heard: " + command, Toast.LENGTH_SHORT).show();

        if (command.contains("who am i") || command.contains("what is my name") || command.contains("my name")) {
            String name = sharedPrefs.getUserName();
            if (name.isEmpty()) name = "User not set";
            speak("Your name is " + name);
            tvStatus.setText("Your name is " + name);
        }
        else if (command.contains("where is my home") || command.contains("home address") || command.contains("my address")) {
            String address = sharedPrefs.getUserAddress();
            if (address.isEmpty()) address = "No home address set";
            speak("Your home address is " + address);
            tvStatus.setText(address);
        }
        else if (command.contains("emergency number") || command.contains("emergency contact") || command.contains("emergency")) {
            String contact = sharedPrefs.getEmergencyContact();
            if (contact.isEmpty()) contact = "Not set";
            speak("Your emergency contact is " + contact);
            tvStatus.setText("Emergency contact: " + contact);
        }
        else if (command.contains("call my") || command.contains("call")) {
            // Extract name/relationship after "call"
            String contactKey = null;
            if (command.contains("call my")) {
                contactKey = command.substring(command.indexOf("call my") + 7).trim();
            } else if (command.contains("call")) {
                contactKey = command.substring(command.indexOf("call") + 4).trim();
            }
            if (contactKey != null && !contactKey.isEmpty()) {
                contactKey = contactKey.replace("please", "").replace("now", "").trim();
                Toast.makeText(getContext(), "Looking for: " + contactKey, Toast.LENGTH_SHORT).show();
                findAndCallContact(contactKey);
            } else {
                speak("Who do you want to call?");
            }
        }
        else if (command.contains("help") || command.contains("sos")) {
            speak("Triggering emergency SOS");
            tvStatus.setText("Triggering SOS...");
            triggerSOS();
        }
        else {
            speak("Unknown command. Try: who am i, home, emergency, call my son, help");
            tvStatus.setText("Unknown: " + command);
        }
    }

    private void findAndCallContact(String key) {
        // Show debug info
        Toast.makeText(getContext(), "Searching for: " + key, Toast.LENGTH_SHORT).show();

        List<Person> people = databaseHelper.getAllPeople();

        // Build list of available names and relationships
        StringBuilder available = new StringBuilder();
        for (Person p : people) {
            available.append(p.getName()).append("(").append(p.getRelationship()).append("), ");
        }
        Toast.makeText(getContext(), "Contacts: " + available.toString(), Toast.LENGTH_LONG).show();

        Person found = null;

        // 1. Try to match by RELATIONSHIP (e.g., "son", "daughter", "doctor")
        for (Person p : people) {
            if (p.getRelationship() != null && p.getRelationship().toLowerCase().contains(key.toLowerCase())) {
                found = p;
                break;
            }
        }

        // 2. If not found by relationship, try by NAME
        if (found == null) {
            for (Person p : people) {
                if (p.getName().toLowerCase().contains(key.toLowerCase())) {
                    found = p;
                    break;
                }
            }
        }

        // 3. If still not found, try exact match on name
        if (found == null) {
            for (Person p : people) {
                if (p.getName().toLowerCase().equals(key.toLowerCase())) {
                    found = p;
                    break;
                }
            }
        }

        if (found != null && found.getPhoneNumber() != null && !found.getPhoneNumber().isEmpty()) {
            speak("Calling " + found.getName());
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(android.net.Uri.parse("tel:" + found.getPhoneNumber()));
            startActivity(callIntent);
        } else {
            speak("Contact " + key + " not found");
            tvStatus.setText("Not found: " + key);
        }
    }

    private void triggerSOS() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).selectNavigationItem(R.id.nav_sos);
        }
    }

    private void speak(String text) {
        if (ttsReady) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            tvStatus.setText("Assistant: " + text);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.setLanguage(Locale.getDefault());
            ttsReady = true;
            speak("Voice assistant ready. Tap the microphone.");
        } else {
            Toast.makeText(getContext(), "Text-to-speech failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (textToSpeech != null) textToSpeech.stop();
        super.onDestroy();
    }

    private class SpeechRecognitionListener implements RecognitionListener {
        @Override public void onReadyForSpeech(Bundle params) {}
        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}
        @Override public void onEndOfSpeech() { tvStatus.setText("Processing..."); }
        @Override public void onError(int error) {
            Toast.makeText(getContext(), "Recognition error. Try again.", Toast.LENGTH_SHORT).show();
            tvStatus.setText("Error, try again");
            btnListen.setEnabled(true);
        }
        @Override public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String command = matches.get(0);
                Toast.makeText(getContext(), "Heard: " + command, Toast.LENGTH_LONG).show();
                processCommand(command);
            } else {
                speak("I didn't catch that");
            }
            btnListen.setEnabled(true);
        }
        @Override public void onPartialResults(Bundle partialResults) {}
        @Override public void onEvent(int eventType, Bundle params) {}
    }
}