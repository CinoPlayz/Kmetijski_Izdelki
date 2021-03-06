package com.nejcroz.kmetijski_izdelki

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
import com.nejcroz.kmetijski_izdelki.databinding.ActivityNacrtovaniPrevzemiDodajBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class NacrtovaniPrevzemiDodajActivity : AppCompatActivity() {
    var url: Config = Config("")
    var token : Token = Token("")
    lateinit var binding: ActivityNacrtovaniPrevzemiDodajBinding
    var datumIzbran: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        //Dobi ali je dark mode enablan, če je da drugo themo
        when (this.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {setTheme(com.nejcroz.kmetijski_izdelki.R.style.AppThemeDodajDark) }
            Configuration.UI_MODE_NIGHT_NO -> {setTheme(com.nejcroz.kmetijski_izdelki.R.style.AppThemeDodaj)}
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {setTheme(com.nejcroz.kmetijski_izdelki.R.style.AppThemeDodaj)}
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nacrtovani_prevzemi_dodaj)
        binding = ActivityNacrtovaniPrevzemiDodajBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Dobimo podatke URL in Token iz json datoteki
        val dobipodatke = CoroutineScope(Dispatchers.Default).async {
            val tokenInUrl = BranjeTokenInConfig(this@NacrtovaniPrevzemiDodajActivity)

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
                    val spinner: Spinner = binding.spinnerStrankaNacrtovano
                    spinner.adapter = ArrayAdapter<String>(this@NacrtovaniPrevzemiDodajActivity, android.R.layout.simple_list_item_1, stranke)

                    val spinnerIzdelki: Spinner = binding.spinnerIzdelekNacrtovano
                    spinnerIzdelki.adapter = ArrayAdapter<String>(this@NacrtovaniPrevzemiDodajActivity, android.R.layout.simple_list_item_1, izdelki)

                    //Ustvari adapter iz arraeya dan_v_tednu_array ter izpiše to v spinner
                    val spinnerDanVTednu: Spinner = binding.spinnerDanNacrtovano
                    ArrayAdapter.createFromResource(
                        this@NacrtovaniPrevzemiDodajActivity,
                        R.array.dan_v_tednu_array,
                        android.R.layout.simple_spinner_item
                    ).also { adapter ->
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                        spinnerDanVTednu.adapter = adapter
                    }
                }
            }
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    NapakaAlert("Ni povezave s strežnikom", this@NacrtovaniPrevzemiDodajActivity)
                }
            }
        }

        binding.calendarViewNacrtovano.setOnDateChangeListener { view, year, month, dayOfMonth ->

            val calander = Calendar.getInstance()

            calander.set(year, month, dayOfMonth, 0, 0, 0)

            datumIzbran = calander

        }






    }

    fun poslji(view: View) {

        CoroutineScope(Dispatchers.IO).launch {

            if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                //Dobimo podatke iz vnosnih polj
                var id_stranke: String = ""
                if(binding.spinnerStrankaNacrtovano.selectedItem != null){
                    id_stranke = binding.spinnerStrankaNacrtovano.selectedItem.toString().substringAfterLast(" - ")
                }


                var izdelek: String = ""

                if(binding.spinnerIzdelekNacrtovano.selectedItem != null){
                    izdelek = binding.spinnerIzdelekNacrtovano.selectedItem.toString()
                }

                var dan: String = ""

                if(binding.spinnerDanNacrtovano.selectedItem != null){
                    dan = binding.spinnerDanNacrtovano.selectedItem.toString()
                }

                val kolicina = binding.editTextNumberKolicinaNacrtovano.text

                val datumzdajinstance = Calendar.getInstance()
                datumzdajinstance.set(Calendar.HOUR_OF_DAY, 0)
                datumzdajinstance.set(Calendar.MINUTE, 0)
                datumzdajinstance.set(Calendar.SECOND, 0)
                datumzdajinstance.set(Calendar.MILLISECOND, 0)

                datumIzbran.set(Calendar.HOUR_OF_DAY, 0)
                datumIzbran.set(Calendar.MINUTE, 0)
                datumIzbran.set(Calendar.SECOND, 0)
                datumIzbran.set(Calendar.MILLISECOND, 0)



                //Preveri podatke, da niso prazni
                var naprej = true

                var Cas_Enkrat = "ne"

                if(datumIzbran.timeInMillis != datumzdajinstance.timeInMillis){
                    if(DanVTednuVSlovenscini(datumIzbran.get(Calendar.DAY_OF_WEEK)) != dan){
                        CoroutineScope(Dispatchers.Main).launch {
                            NapakaAlert("Izbrani datum, ter dan v tednu se ne ojemata", this@NacrtovaniPrevzemiDodajActivity)
                        }
                        naprej = false
                    }
                    else{
                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        Cas_Enkrat = format.format(datumIzbran.time)
                    }
                }

                if(id_stranke.isNullOrEmpty()){
                    CoroutineScope(Dispatchers.Main).launch {
                        NapakaAlert("Izberite stranko", this@NacrtovaniPrevzemiDodajActivity)
                    }
                    naprej = false
                }

                if(kolicina.isNullOrEmpty()){
                    CoroutineScope(Dispatchers.Main).launch {
                        NapakaAlert("Vpišite količino", this@NacrtovaniPrevzemiDodajActivity)
                    }
                    naprej = false
                }

                if(izdelek.isNullOrEmpty()){
                    CoroutineScope(Dispatchers.Main).launch {
                        NapakaAlert("Izdeleka ni", this@NacrtovaniPrevzemiDodajActivity)
                    }
                    naprej = false
                }

                if(dan.isNullOrEmpty()){
                    CoroutineScope(Dispatchers.Main).launch {
                        NapakaAlert("Dneva ni", this@NacrtovaniPrevzemiDodajActivity)
                    }
                    naprej = false
                }

                if(naprej){
                    //Ustvari podatke za poslat v JSON formatu
                    val podatkiVJson = CoroutineScope(Dispatchers.Default).async {

                        if(Cas_Enkrat == "ne"){
                            Cas_Enkrat = ""
                        }

                        //Ustvari data class prodajaPoslat
                        val podatki = nacrtovani_prevzemiPoslati(kolicina.toString(), dan, "Cel", izdelek, id_stranke, Cas_Enkrat)

                        //Kliče metodo JsonUstvarjanjeProdaja, toliko, da vrne ta courutineScope neke podatke
                        JsonUstvarjanjeNacrtovaniPrevzemi(podatki)
                    }

                    //Se poveže in ustvari Podatke
                    CoroutineScope(Dispatchers.IO).launch {
                        val podatkiZaPoslat = podatkiVJson.await()

                        if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                            val res =
                                Jsoup.connect(url.URL + "ustvarjanje.php?tabela=Nacrtovani_Prevzemi").timeout(5000)
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
                                UspehAlert("Uspešno dodano", this@NacrtovaniPrevzemiDodajActivity)
                            }

                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                NapakaAlert("Ni povezave s strežnikom", this@NacrtovaniPrevzemiDodajActivity)
                            }
                        }

                    }



                }

            }
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    NapakaAlert("Ni povezave s strežnikom", this@NacrtovaniPrevzemiDodajActivity)
                }
            }
        }
    }

    fun odjava(view: View) {
        val context = this
        SkupnaOdjava(context)
    }

    fun nazaj(view: View) {
        val intent = Intent(this, DodajActivity::class.java)
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