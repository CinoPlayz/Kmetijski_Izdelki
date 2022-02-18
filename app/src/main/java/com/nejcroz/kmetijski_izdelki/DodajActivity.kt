package com.nejcroz.kmetijski_izdelki

import android.R
import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.gson.Gson
import com.nejcroz.kmetijski_izdelki.databinding.ActivityDodajBinding
import com.nejcroz.kmetijski_izdelki.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.lang.reflect.Array.set
import java.text.SimpleDateFormat
import java.util.*

class DodajActivity : AppCompatActivity() {
    var url: Config = Config("")
    var token : Token = Token("")
    lateinit var binding: ActivityDodajBinding
    var datumIzbran: Date = Calendar.getInstance().time

    override fun onCreate(savedInstanceState: Bundle?) {
        //Dobi ali je dark mode enablan, če je da drugo themo
        when (this.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {setTheme(com.nejcroz.kmetijski_izdelki.R.style.AppThemeDodajDark) }
            Configuration.UI_MODE_NIGHT_NO -> {setTheme(com.nejcroz.kmetijski_izdelki.R.style.AppThemeDodaj)}
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {setTheme(com.nejcroz.kmetijski_izdelki.R.style.AppThemeDodaj)}
        }

        super.onCreate(savedInstanceState)
        binding = ActivityDodajBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Dobimo podatke URL in Token iz json datoteki
        val dobipodatke = CoroutineScope(Dispatchers.Default).async {
            val tokenInUrl = BranjeTokenInConfig(this@DodajActivity)

            url = tokenInUrl[0] as Config
            token = tokenInUrl[1] as Token

        }

        CoroutineScope(Dispatchers.IO).launch {
            dobipodatke.await()

            if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                //Dobimo vse stranke
                val res = Jsoup.connect(url.URL + "branje.php?tabela=Stranka").timeout(5000)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Authorization", "Bearer " + token.token)
                    .header("Accept", "application/json")
                    .method(Connection.Method.POST)
                    .execute()

                val datastranke = Gson().fromJson(res.body(), Data_Stranke::class.java)


                //Dobimo vse izdelke
                val resIzdelek = Jsoup.connect(url.URL + "branje.php?tabela=Izdelek").timeout(5000)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Authorization", "Bearer " + token.token)
                    .header("Accept", "application/json")
                    .method(Connection.Method.POST)
                    .execute()

                val datazdelek = Gson().fromJson(resIzdelek.body(), Data_Izdelki::class.java)

                var stranke = mutableListOf<String>()
                var izdelki = mutableListOf<String>()

                if(!datastranke.data.isNullOrEmpty()){
                    for (podatek in datastranke.data){
                        stranke.add(podatek.Priimek + " " + podatek.Ime + " - " + podatek.id_stranke)
                    }
                }

                if(!datazdelek.data.isNullOrEmpty()){
                    for (podatek in datazdelek.data){
                        izdelki.add(podatek.Izdelek)
                    }
                }

                CoroutineScope(Dispatchers.Main).launch {
                    val spinner: Spinner = binding.spinnerStranka
                    spinner.adapter = ArrayAdapter<String>(this@DodajActivity, R.layout.simple_list_item_1, stranke)

                    val spinnerIzdelki: Spinner = binding.spinnerIzdelek
                    spinnerIzdelki.adapter = ArrayAdapter<String>(this@DodajActivity, R.layout.simple_list_item_1, izdelki)
                }
            }
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    NapakaAlert("Ni povezave s strežnikom", this@DodajActivity)
                }
            }
        }

        binding.calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->

            val calander = Calendar.getInstance()

            calander.set(year, month, dayOfMonth, 1, 0, 0)

            datumIzbran = calander.time

        }


    }

    fun poslji(view: View) {

        CoroutineScope(Dispatchers.IO).launch {

            if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                //Dobimo podatke iz vnosnih polj
                var id_stranke: String = ""
                if(binding.spinnerStranka.selectedItem != null){
                    id_stranke = binding.spinnerStranka.selectedItem.toString().substringAfterLast(" - ")
                }


                var izdelek: String = ""

                if(binding.spinnerIzdelek.selectedItem != null){
                    izdelek = binding.spinnerIzdelek.selectedItem.toString()
                }

                val kolicina = binding.editTextNumberKolicina.text

                val datumzdajinstance = Calendar.getInstance().time
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val datum = format.format(datumzdajinstance)
                val datumIzbrani = format.format(datumIzbran)

                //Preveri podatke, da niso prazni
                var naprej = true
                if(id_stranke.isNullOrEmpty()){
                    CoroutineScope(Dispatchers.Main).launch {
                        NapakaAlert("Izberite stranko", this@DodajActivity)
                    }
                    naprej = false
                }

                if(kolicina.isNullOrEmpty()){
                    CoroutineScope(Dispatchers.Main).launch {
                        NapakaAlert("Vpišite količino", this@DodajActivity)
                    }
                    naprej = false
                }

                if(izdelek.isNullOrEmpty()){
                    CoroutineScope(Dispatchers.Main).launch {
                        NapakaAlert("Izdeleka ni", this@DodajActivity)
                    }
                    naprej = false
                }

                if(naprej){
                    //Ustvari podatke za poslat v JSON formatu
                    val podatkiVJson = CoroutineScope(Dispatchers.Default).async {
                        //Ustvari data class prodajaPoslat
                        val podatki = prodajaPoslat(datumIzbrani, datum, kolicina.toString(), id_stranke, izdelek)

                        //Kliče metodo JsonUstvarjanjeProdaja, toliko, da vrne ta courutineScope neke podatke
                        JsonUstvarjanjeProdaja(podatki)
                    }

                    //Se poveže in ustvari Podatke
                    CoroutineScope(Dispatchers.IO).launch {
                        val podatkiZaPoslat = podatkiVJson.await()

                        if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                            val res =
                                Jsoup.connect(url.URL + "ustvarjanje.php?tabela=Prodaja").timeout(5000)
                                    .ignoreHttpErrors(true)
                                    .ignoreContentType(true)
                                    .header("Content-Type", "application/json;charset=UTF-8")
                                    .header("Authorization", "Bearer " + token.token)
                                    .header("Accept", "application/json")
                                    .requestBody(podatkiZaPoslat)
                                    .method(Connection.Method.POST)
                                    .execute()

                            //Uspešno doda se to izpiše
                            CoroutineScope(Dispatchers.Main).launch {
                                UspehAlert("Uspešno dodano", this@DodajActivity)
                            }

                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                NapakaAlert("Ni povezave s strežnikom", this@DodajActivity)
                            }
                        }

                    }



                }

            }
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    NapakaAlert("Ni povezave s strežnikom", this@DodajActivity)
                }
            }
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

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

}

