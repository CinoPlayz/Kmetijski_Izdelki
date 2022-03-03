package com.nejcroz.kmetijski_izdelki

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import java.io.File

class NastavitveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        //Dobi ali je dark mode enablan, če je da drugo themo
        when (this.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {setTheme(R.style.AppThemeDodajDark) }
            Configuration.UI_MODE_NIGHT_NO -> {setTheme(R.style.AppThemeDodaj)}
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {setTheme(R.style.AppThemeDodaj)}
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }


    }

    fun odjava(view: View) {

        val context = this

        var datoteka = File(context.filesDir, "Login_Token.json")

        if (datoteka.exists()){
            datoteka.delete()
        }

        datoteka = File(context.filesDir, "config.json")

        if (datoteka.exists()){
            datoteka.delete()
        }

        val intent = Intent(this, LoginActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun nazaj(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val prefrencekolikovrstic = preferenceManager.findPreference<EditTextPreference>("kolikovrstic")

            prefrencekolikovrstic?.setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER
            }
        }
    }
}