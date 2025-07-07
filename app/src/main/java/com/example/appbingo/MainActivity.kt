package com.example.appbingo

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT // Importar WRAP_CONTENT directamente
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    // Variables globales, minimizando el lateinit donde sea posible
    private lateinit var tts: TextToSpeech
    private var playerUid: String = ""
    private var dimension: Int = 0

    // Vistas de la primera pantalla (se inicializan en setupInitialScreen)
    private var etDimension: EditText? = null
    private var tvPlayerUid: TextView? = null

    // Vistas de la segunda pantalla (se inicializan en setupBingoGameScreen)
    private var bingoGridLayout: GridLayout? = null
    private var tvPlayerUidDisplay: TextView? = null
    private var btnRegenerateCard: Button? = null

    // Datos del juego
    private var bingoNumbers = mutableListOf<Int>()
    private val selectedNumbers = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tts = TextToSpeech(this, this)
        setupInitialScreen()
    }

    private fun setupInitialScreen() {
        setContentView(R.layout.activity_main)

        applyWindowInsets(R.id.initial_screen_layout) // Función auxiliar para insets

        // Inicialización de vistas de la primera pantalla
        etDimension = findViewById(R.id.et_dimension)
        tvPlayerUid = findViewById(R.id.tv_player_uid)
        val btnGenerateBingo: Button = findViewById(R.id.btn_generate_bingo)

        playerUid = generatePlayerUid()
        tvPlayerUid?.text = "Player ID: $playerUid" // Usar el operador de seguridad '?.'

        btnGenerateBingo.setOnClickListener {
            val dimensionValue = etDimension?.text.toString().toIntOrNull() ?: 0 // Combinar nulo y default

            if (dimensionValue <= 0) {
                Toast.makeText(this, "Enter matrix size", Toast.LENGTH_SHORT).show()
            } else {
                dimension = dimensionValue
                setupBingoGameScreen()
            }
        }
    }

    // --- CAMBIO AQUÍ: Función generatePlayerUid simplificada ---
    private fun generatePlayerUid(): String {
        return "AE" + (1..6)
            .map { (('A'..'Z') + ('0'..'9')).random() } // Genera el conjunto de chars en línea y luego toma uno aleatorio
            .joinToString("") + "P"
    }

    private fun setupBingoGameScreen() {
        setContentView(R.layout.activity_main_2)

        applyWindowInsets(R.id.bingo_game_layout) // Función auxiliar para insets

        // Inicialización de vistas de la segunda pantalla
        bingoGridLayout = findViewById(R.id.bingo_grid_layout)
        tvPlayerUidDisplay = findViewById(R.id.tv_player_uid_display)
        btnRegenerateCard = findViewById(R.id.btn_regenerate_card)

        tvPlayerUidDisplay?.text = "Player ID: $playerUid" // Usar el operador de seguridad '?.'

        setupBingoCard()

        btnRegenerateCard?.setOnClickListener { regenerateBingoCard() } // Simplificar listener
    }

    //pa setear la voz al hacer bingo
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Idioma español no soportado. Usando predeterminado.")
                Toast.makeText(this, "Voz en español no disponible.", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.e("TTS", "Fallo al inicializar TTS: $status")
            Toast.makeText(this, "Error de voz. 'Bingo' no se cantará.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupBingoCard() {
        bingoGridLayout?.apply { // Usar 'apply' en el GridLayout para configurar sus propiedades
            columnCount = dimension
            rowCount = dimension
            removeAllViews()
        }
        selectedNumbers.clear()

        val numberOfCells = dimension * dimension
        bingoNumbers = (1..100).shuffled().take(numberOfCells).toMutableList()

        bingoNumbers.forEach { number -> // Iterar con forEach
            val textView = TextView(this).apply {
                text = number.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bingo_circle_background)
                setTextColor(resources.getColor(android.R.color.white, theme))
                setPadding(16, 16, 16, 16)

                layoutParams = GridLayout.LayoutParams().apply {
                    width = (resources.displayMetrics.density * 60).toInt()
                    height = (resources.displayMetrics.density * 60).toInt()
                    setMargins(8, 8, 8, 8)
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                setOnClickListener {
                    toggleNumberSelection(this, number) // Pasar 'this' (el TextView) directamente
                    checkBingo()
                }
            }
            bingoGridLayout?.addView(textView) // Usar el operador de seguridad '?.'
        }
    }

    private fun regenerateBingoCard() {
        setupBingoCard()
        Toast.makeText(this, "New Card Generated!", Toast.LENGTH_SHORT).show()
    }

    private fun toggleNumberSelection(textView: TextView, number: Int) {
        if (selectedNumbers.contains(number)) {
            selectedNumbers.remove(number)
            textView.setBackgroundResource(R.drawable.bingo_circle_background)
        } else {
            selectedNumbers.add(number)
            textView.setBackgroundResource(R.drawable.bingo_circle_selected_background)
        }
    }

    private fun checkBingo() {
        val bingoCardMatrix = Array(dimension) { r ->
            IntArray(dimension) { c -> bingoNumbers[r * dimension + c] }
        }

        when {
            checkRows(bingoCardMatrix) -> showBingoAlert()
            checkColumns(bingoCardMatrix) -> showBingoAlert()
            checkMainDiagonal(bingoCardMatrix) -> showBingoAlert()
            checkAntiDiagonal(bingoCardMatrix) -> showBingoAlert()
        }
    }

    // Funciones auxiliares para checkBingo (ya estaban bastante concisas)
    private fun checkRows(matrix: Array<IntArray>): Boolean = matrix.any { row -> row.all { selectedNumbers.contains(it) } }
    private fun checkColumns(matrix: Array<IntArray>): Boolean {
        for (j in 0 until dimension) {
            if ((0 until dimension).all { i -> selectedNumbers.contains(matrix[i][j]) }) return true
        }
        return false
    }
    private fun checkMainDiagonal(matrix: Array<IntArray>): Boolean = (0 until dimension).all { i -> selectedNumbers.contains(matrix[i][i]) }
    private fun checkAntiDiagonal(matrix: Array<IntArray>): Boolean = (0 until dimension).all { i -> selectedNumbers.contains(matrix[i][dimension - 1 - i]) }

    private fun showBingoAlert() {
        tts.speak("Bingo!", TextToSpeech.QUEUE_FLUSH, null, null)

        AlertDialog.Builder(this)
            .setTitle("BINGO!")
            .setMessage("Congratulations, you win!")
            .setPositiveButton("Accept") { dialog, _ -> dialog.dismiss() } // Simplificar listener
            .show()
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    // Función auxiliar para aplicar insets, para evitar duplicación
    private fun applyWindowInsets(layoutId: Int) {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(layoutId)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @Deprecated("Deprecated in API 33")
    override fun onBackPressed() {
        // Verificar qué layout está actualmente inflado para decidir la acción de retroceso
        if (findViewById<GridLayout?>(R.id.bingo_grid_layout) != null) {
            setupInitialScreen()
        } else {
            super.onBackPressed()
        }
    }
}