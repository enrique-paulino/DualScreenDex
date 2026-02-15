package com.enrpau.dualscreendex

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.enrpau.dualscreendex.data.RomManager
import com.enrpau.dualscreendex.data.RomProfile
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream

class CreateProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var txtDexFile: TextView
    private lateinit var txtRegFile: TextView
    private lateinit var txtMatchFile: TextView
    private lateinit var radioGroup: RadioGroup

    private var dexUri: Uri? = null
    private var regUri: Uri? = null
    private var matchUri: Uri? = null

    private var activeRequest = ""

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val name = getFileName(it)
            when (activeRequest) {
                "dex" -> {
                    dexUri = it
                    txtDexFile.text = name
                }
                "reg" -> {
                    regUri = it
                    txtRegFile.text = name
                }
                "match" -> {
                    matchUri = it
                    txtMatchFile.text = name
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.loadTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_profile)

        etName = findViewById(R.id.etProfileName)
        txtDexFile = findViewById(R.id.txtDexFile)
        txtRegFile = findViewById(R.id.txtRegFile)
        txtMatchFile = findViewById(R.id.txtMatchFile)
        radioGroup = findViewById(R.id.radioMechanics)


        val mimeTypes = arrayOf("*/*") // */* is safer so that csv isn't disallowed

        findViewById<MaterialButton>(R.id.btnPickDex).setOnClickListener {
            activeRequest = "dex"
            filePickerLauncher.launch(mimeTypes)
        }

        findViewById<MaterialButton>(R.id.btnPickReg).setOnClickListener {
            activeRequest = "reg"
            filePickerLauncher.launch(mimeTypes)
        }

        findViewById<MaterialButton>(R.id.btnPickMatch).setOnClickListener {
            activeRequest = "match"
            filePickerLauncher.launch(mimeTypes)
        }

        // Save
        findViewById<MaterialButton>(R.id.btnSaveProfile).setOnClickListener { saveProfile() }

        // Cancel / Back
        findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener { finish() }
    }

    private fun saveProfile() {
        val name = etName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            return
        }
        if (dexUri == null) {
            Toast.makeText(this, "Pokedex CSV is required", Toast.LENGTH_SHORT).show()
            return
        }

        val dexFile = copyToInternal(dexUri!!, "dex_${System.currentTimeMillis()}.csv")
        val regFile = if (regUri != null) copyToInternal(regUri!!, "reg_${System.currentTimeMillis()}.csv") else null
        val matchFile = if (matchUri != null) copyToInternal(matchUri!!, "match_${System.currentTimeMillis()}.csv") else null

        // 2. Determine Mechanics
        val mechanics = when (radioGroup.checkedRadioButtonId) {
            R.id.rbGen1 -> RomProfile.Mechanics.GEN_1
            R.id.rbGen25 -> RomProfile.Mechanics.GEN_2_TO_5
            else -> RomProfile.Mechanics.GEN_6_PLUS
        }

        // 3. Save via RomManager
        RomManager.saveCustomProfile(this, name, dexFile, regFile, matchFile, mechanics)

        Toast.makeText(this, "Profile Saved!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun copyToInternal(uri: Uri, filename: String): File {
        val destFile = File(filesDir, filename)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor.use { c ->
                if (c != null && c.moveToFirst()) {
                    val index = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = c.getString(index)
                }
            }
        }
        return result ?: "Unknown File"
    }
}