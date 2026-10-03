package com.example.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class JarvisVoiceService : Service(), TextToSpeech.OnInitListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null

    private val handler = Handler(Looper.getMainLooper())

    private val channelId = "jarvis_background_channel"
    private val notificationId = 1001

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = Notification.Builder(this, channelId)
            .setContentTitle("JARVIS")
            .setContentText("JARVIS background listening is active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        startForeground(notificationId, notification)

        tts = TextToSpeech(this, this)

        startListening()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    private fun startListening() {
        handler.post {
            try {
                speechRecognizer?.destroy()

                if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                    return@post
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

                speechRecognizer?.setRecognitionListener(
                    object : RecognitionListener {

                        override fun onReadyForSpeech(params: android.os.Bundle?) {}

                        override fun onBeginningOfSpeech() {}

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            restartListening()
                        }

                        override fun onResults(
                            results: android.os.Bundle?
                        ) {
                            val matches =
                                results?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )

                            val text = matches
                                ?.firstOrNull()
                                ?.lowercase(Locale.getDefault())
                                ?: ""

                            processCommand(text)

                            restartListening()
                        }

                        override fun onPartialResults(
                            partialResults: android.os.Bundle?
                        ) {}

                        override fun onEvent(
                            eventType: Int,
                            params: android.os.Bundle?
                        ) {}
                    }
                )

                val intent = Intent(
                    RecognizerIntent.ACTION_RECOGNIZE_SPEECH
                ).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE,
                        Locale.getDefault()
                    )

                    putExtra(
                        RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                        false
                    )
                }

                speechRecognizer?.startListening(intent)

            } catch (_: Exception) {
                restartListening()
            }
        }
    }

    private fun restartListening() {
        handler.removeCallbacksAndMessages(null)

        handler.postDelayed(
            {
                startListening()
            },
            1000
        )
    }

    private fun processCommand(text: String) {

        if (text.isBlank()) {
            return
        }

        val command = text
            .replace("hey jarvis", "")
            .replace("hey jarvis", "")
            .trim()

        if (text.contains("hey jarvis")) {

            if (command.isBlank()) {
                speak("Yes, I am listening.")
                return
            }

            executeCommand(command)
        }
    }

    private fun executeCommand(command: String) {

        when {

            command.contains("hello") ||
            command.contains("hi") -> {
                speak("Hello. How can I help you?")
            }

            command.contains("time") -> {
                val time = java.text.SimpleDateFormat(
                    "hh:mm a",
                    Locale.getDefault()
                ).format(java.util.Date())

                speak("The time is $time")
            }

            command.contains("date") -> {
                val date = java.text.SimpleDateFormat(
                    "dd MMMM yyyy",
                    Locale.getDefault()
                ).format(java.util.Date())

                speak("Today is $date")
            }

            command.contains("youtube") -> {
                openUrl("https://www.youtube.com")
            }

            command.contains("google") ||
            command.contains("search") -> {
                openUrl(
                    "https://www.google.com/search?q=" +
                            android.net.Uri.encode(command)
                )
            }

            else -> {
                speak("I heard you say $command")
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                android.net.Uri.parse(url)
            )

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            startActivity(intent)

        } catch (_: Exception) {
            speak("I couldn't open that.")
        }
    }

    private fun speak(text: String) {
        handler.post {
            tts?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS_RESPONSE"
            )
        }
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                channelId,
                "JARVIS Background",
                NotificationManager.IMPORTANCE_LOW
            )

            channel.description =
                "JARVIS background voice assistant"

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {

        handler.removeCallbacksAndMessages(null)

        speechRecognizer?.destroy()
        speechRecognizer = null

        tts?.stop()
        tts?.shutdown()
        tts = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
