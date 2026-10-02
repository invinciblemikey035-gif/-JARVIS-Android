package com.example.jarvis

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val bg = Color.rgb(7, 13, 30)
    private val cyan = Color.rgb(0, 220, 255)
    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var statusText: TextView
    private lateinit var transcriptText: TextView
    private lateinit var commandInput: EditText
    private lateinit var listenButton: Button
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        tts = TextToSpeech(this, this)
        buildUi()
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    statusText.text = "Listening… / Sun raha hoon"
                }
                override fun onBeginningOfSpeech() { statusText.text = "Bolte raho…" }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { statusText.text = "Processing…" }
                override fun onError(error: Int) {
                    statusText.text = "Voice samajh nahi aayi. Dobara try karo."
                    listenButton.isEnabled = true
                }
                override fun onResults(results: Bundle?) {
                    listenButton.isEnabled = true
                    val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    commandInput.setText(words)
                    if (words.isNotBlank()) handleCommand(words)
                    else statusText.text = "Koi command nahi mili."
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } else {
            statusText.text = "Is phone par speech recognition available nahi."
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(bg)
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 0, 0, 12)
        }

        val title = TextView(this).apply {
            text = "J.A.R.V.I.S"
            textSize = 32f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            letterSpacing = 0.16f
        }
        content.addView(title, matchWrap())

        val subtitle = TextView(this).apply {
            text = "PERSONAL VOICE ASSISTANT"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 28)
            letterSpacing = 0.12f
        }
        content.addView(subtitle, matchWrap())

        val orb = TextView(this).apply {
            text = "◉"
            textSize = 92f
            setTextColor(cyan)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 4)
        }
        content.addView(orb, matchWrap())

        statusText = TextView(this).apply {
            text = "Ready. Press the microphone."
            textSize = 16f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 20)
        }
        content.addView(statusText, matchWrap())

        listenButton = Button(this).apply {
            text = "🎙  TALK TO JARVIS"
            textSize = 16f
            setTextColor(Color.BLACK)
            setBackgroundColor(cyan)
            setOnClickListener { startListening() }
        }
        content.addView(listenButton, fullWidth(58))

        commandInput = EditText(this).apply {
            hint = "Or type a command…"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            textSize = 16f
            setSingleLine(true)
            setPadding(14, 10, 14, 10)
        }
        val inputParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 18 }
        content.addView(commandInput, inputParams)

        val send = Button(this).apply {
            text = "SEND COMMAND"
            setOnClickListener {
                val command = commandInput.text.toString()
                if (command.isNotBlank()) handleCommand(command)
            }
        }
        content.addView(send, fullWidth(48))

        transcriptText = TextView(this).apply {
            text = "JARVIS: Systems online. I am ready."
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(0, 24, 0, 12)
        }
        content.addView(transcriptText, matchWrap())

        val help = TextView(this).apply {
            text = "Try: “What time is it?”, “Open YouTube”, “Search cats”, “Open WhatsApp”"
            textSize = 13f
            setTextColor(Color.rgb(150, 180, 195))
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 20)
        }
        content.addView(help, matchWrap())

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        setContentView(root)
    }

    private fun startListening() {
        if (speechRecognizer == null) {
            statusText.text = "Speech service unavailable. Type a command instead."
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
            return
        }
        try {
            listenButton.isEnabled = false
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to JARVIS")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            listenButton.isEnabled = true
            statusText.text = "Could not start microphone: ${e.localizedMessage}"
        }
    }

    private fun handleCommand(raw: String) {
        val command = raw.trim()
        val c = command.lowercase(Locale.ROOT)
        transcriptText.text = "YOU: $command"
        val response = when {
            c.contains("time") || c.contains("waqt") ->
                "The time is " + SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
            c.contains("date") || c.contains("tareekh") || c.contains("today") ->
                "Today is " + SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
            c.contains("who are you") || c.contains("tum kon") || c.contains("your name") ->
                "I am JARVIS, your personal voice assistant."
            c.contains("hello") || c == "hi" || c.contains("salam") ->
                "Hello! Main JARVIS hoon. Batao, kya karna hai?"
            c.contains("open youtube") || c.contains("youtube kholo") ->
                openWeb("https://www.youtube.com", "Opening YouTube.")
            c.contains("open whatsapp") || c.contains("whatsapp kholo") ->
                openApp("com.whatsapp", "WhatsApp is not installed. Opening its website.", "https://www.whatsapp.com")
            c.startsWith("search ") || c.startsWith("google ") || c.startsWith("find ") ->
                searchWeb(command.substringAfter(' ').trim())
            c.contains("open settings") || c.contains("settings kholo") -> {
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                    "Opening phone settings."
                } catch (_: Exception) { "I could not open settings." }
            }
            c.contains("help") || c.contains("commands") ->
                "You can ask the time or date, say open YouTube, open WhatsApp, or say search followed by your topic."
            else ->
                "I heard: $command. This command is not built in yet. Try saying search followed by your topic."
        }
        transcriptText.text = "YOU: $command\n\nJARVIS: $response"
        statusText.text = "Ready for your next command."
        speak(response)
    }

    private fun searchWeb(query: String): String {
        if (query.isBlank()) return "Tell me what you want to search for."
        openWeb("https://www.google.com/search?q=" + Uri.encode(query), "Searching Google.")
        return "Searching Google for $query."
    }

    private fun openWeb(url: String, message: String): String {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            return "I could not open the browser."
        }
        return message
    }

    private fun openApp(packageName: String, fallbackMessage: String, fallbackUrl: String): String {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        return if (launch != null) {
            startActivity(launch)
            "Opening WhatsApp."
        } else {
            openWeb(fallbackUrl, fallbackMessage)
        }
    }

    private fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setSpeechRate(0.95f)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else if (requestCode == 100) {
            statusText.text = "Microphone permission is needed for voice commands."
            listenButton.isEnabled = true
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun fullWidth(heightDp: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, (heightDp * resources.displayMetrics.density).toInt()
    ).apply { topMargin = 8 }
}
