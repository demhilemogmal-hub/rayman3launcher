package com.example.rayman3launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Tiny launcher: remembers your Rayman 3 disc image and starts the official
 * Dolphin Android app with that game auto-started.
 *
 * Touch controls, save states, graphics settings etc. all live inside Dolphin.
 */
class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var status: TextView
    private lateinit var playButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("launcher", Context.MODE_PRIVATE)

        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(pad, pad * 2, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "Rayman 3"
            textSize = 30f
            gravity = Gravity.CENTER
        })

        status = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, pad, 0, pad)
        }
        root.addView(status)

        root.addView(makeButton("Choose disc image") { pickGame() })
        root.addView(makeButton("Enter file path instead") { askForPath() })
        playButton = makeButton("Play") { launchDolphin() }
        root.addView(playButton)

        setContentView(root)
        refresh()
    }

    private fun makeButton(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun savedGame(): String? = prefs.getString(KEY_GAME, null)

    private fun saveGame(game: String) {
        prefs.edit().putString(KEY_GAME, game).apply()
        refresh()
    }

    private fun refresh() {
        val game = savedGame()
        status.text = if (game == null) "No disc image selected" else displayName(game)
        playButton.isEnabled = game != null
    }

    private fun displayName(game: String): String {
        if (game.startsWith("content://")) {
            try {
                contentResolver.query(Uri.parse(game), null, null, null, null)?.use { c ->
                    val col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (col >= 0 && c.moveToFirst()) return c.getString(col)
                }
            } catch (e: Exception) {
                // fall through to the raw string
            }
        }
        return game.substringAfterLast('/')
    }

    // --- picking the game -------------------------------------------------

    private fun pickGame() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        startActivityForResult(intent, REQUEST_PICK_GAME)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK_GAME || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: SecurityException) {
            // Some providers don't allow persistable grants; the path option still works.
        }
        saveGame(uri.toString())
    }

    private fun askForPath() {
        val input = EditText(this).apply {
            hint = "/storage/emulated/0/Games/Rayman3.iso"
            setSingleLine()
            setText(savedGame()?.takeIf { it.startsWith("/") } ?: "")
        }
        AlertDialog.Builder(this)
            .setTitle("Path to disc image")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val path = input.text.toString().trim()
                if (path.isNotEmpty()) saveGame(path)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- launching Dolphin ------------------------------------------------

    private fun launchDolphin() {
        val game = savedGame() ?: return
        val intent = Intent().apply {
            component = ComponentName(DOLPHIN_PACKAGE, DOLPHIN_MAIN_ACTIVITY)
            // Older Dolphin builds read AutoStartFile, newer ones read AutoStartFiles.
            putExtra("AutoStartFile", game)
            putExtra("AutoStartFiles", arrayOf(game))
            if (game.startsWith("content://")) {
                // Extras aren't covered by URI grants, so also attach the URI as ClipData.
                clipData = ClipData.newRawUri("game", Uri.parse(game))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Dolphin isn't installed", Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Dolphin refused to launch: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private companion object {
        const val KEY_GAME = "game"
        const val REQUEST_PICK_GAME = 1
        const val DOLPHIN_PACKAGE = "org.dolphinemu.dolphinemu"
        const val DOLPHIN_MAIN_ACTIVITY = "org.dolphinemu.dolphinemu.ui.main.MainActivity"
    }
}
