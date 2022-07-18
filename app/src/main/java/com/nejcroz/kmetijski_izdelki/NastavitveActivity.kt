package com.nejcroz.kmetijski_izdelki

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.io.FileWriter

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
        SkupnaOdjava(context)
    }

    fun nazaj(view: View) {
        val intent = Intent(this, GlavniActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        var url: Config = Config("")
        var token : Token = Token("")

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            //Dobi prefrence kolikovrstic ter spremeni tipkovnico toliko, da so samo številke
            val prefrencekolikovrstic = preferenceManager.findPreference<EditTextPreference>("kolikovrstic")

            prefrencekolikovrstic?.setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER
            }

            //Dobi prefrence kolikovrstic ter spremeni tipkovnico toliko, da so samo številke
            val prefrencekolikotednov = preferenceManager.findPreference<EditTextPreference>("kolikotednov")

            prefrencekolikotednov?.setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER
            }

            //Dobi prefrence URL ter izpiše v ta prefrance, tisto kar je shranjeno v config.json za URL, dobi tudi Token ter ga shrani v spremenljivko token
            val prefrenceURL = preferenceManager.findPreference<EditTextPreference>("URL")

            var datoteka = File(requireContext().filesDir, "config.json")

            if (datoteka.exists()) {
                val napisano = File(requireContext().filesDir.absolutePath + "/config.json").readText(Charsets.UTF_8)
                url = Gson().fromJson(napisano, Config::class.java)

                val napisano2 = File(requireContext().filesDir.absolutePath + "/Login_Token.json").readText(Charsets.UTF_8)

                token = Gson().fromJson(napisano2, Token::class.java)
            }

            prefrenceURL?.text = url.URL

            //Spremlja za event PreferenceChangeListener, ko se to zgodi spremeni URL v config.json na nov url, če lahko s tem novim url naredi povezavo,
            // drugače samo nastavi na prejšni url

            datoteka = File(requireContext().filesDir, "config.json")

            prefrenceURL?.setOnPreferenceChangeListener { preference, newValue ->

                CoroutineScope(Dispatchers.IO).launch {
                    if (PovezavaObstajaStreznik(newValue.toString())) {

                        //Se proba povezati z novim URL-jem
                        val res = Jsoup.connect(newValue.toString() + "branje.php?tabela=Prodaja&omejitev=1").timeout(5000)
                            .ignoreHttpErrors(true)
                            .ignoreContentType(true)
                            .header("Content-Type", "application/json;charset=UTF-8")
                            .header("Authorization", "Bearer " + token.token)
                            .header("Accept", "application/json")
                            .method(Connection.Method.POST)
                            .execute()

                        if(res.statusCode() == 404){
                            CoroutineScope(Dispatchers.Main).launch {
                                NapakaAlert(
                                    "URL je narobe",
                                    requireContext()
                                )

                                prefrenceURL?.text = url.URL
                            }

                        }else{
                            //Če vrne 401 pomeni da se lahko poveže vendar bo potrebna nova prijava
                            if(res.statusCode() == 401){
                                CoroutineScope(Dispatchers.Main).launch {

                                    val builder = AlertDialog.Builder(requireContext())

                                    builder.setTitle("Napaka")
                                    builder.setMessage("Token je neveljaven potrebna bo ponovna prijava.")
                                    builder.setPositiveButton("OK", null)

                                    val alertDialog = builder.create()
                                    alertDialog.show()

                                    //Dobi ok gumb iz alertDialog ter mu nastavi lastnost, width tako, da je na sredini
                                    val okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    val layoutParams = okButton.layoutParams as LinearLayout.LayoutParams
                                    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                                    okButton.layoutParams = layoutParams

                                    okButton.setOnClickListener {
                                        val context = requireContext()

                                        var datoteka = File(context.filesDir, "Login_Token.json")

                                        if (datoteka.exists()){
                                            datoteka.delete()
                                        }

                                        datoteka = File(context.filesDir, "config.json")

                                        if (datoteka.exists()){
                                            datoteka.delete()
                                        }

                                        val intent = Intent(requireContext(), LoginActivity::class.java)
                                        intent.putExtra("URL", newValue.toString())
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                        startActivity(intent)
                                    }


                                }
                            }
                            else{
                                //Če je res vse vredu in če je token veljaven spremeni URL
                                val ConfigObjekt = Config(newValue.toString())

                                val configJson = Gson().toJson(ConfigObjekt)

                                try {
                                    FileWriter(datoteka).use { it.write(configJson) }
                                } catch (t: Throwable) {
                                    println(t.message)
                                }
                            }


                        }


                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            NapakaAlert(
                                "Ni povezave s strežnikom (preverite, da je URL v pravilnem formatu \" [IP Naslov oz. domena]/[pot do mape api] \" )",
                                requireContext()
                            )
                            prefrenceURL?.text = url.URL
                        }

                    }
                }

                true
            }

        }
    }
}