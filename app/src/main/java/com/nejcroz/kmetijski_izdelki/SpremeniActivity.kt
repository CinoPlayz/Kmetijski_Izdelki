package com.nejcroz.kmetijski_izdelki

import android.R
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.nejcroz.kmetijski_izdelki.databinding.ActivitySpremeniBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class SpremeniActivity : AppCompatActivity() {
    var url: Config = Config("")
    var token : Token = Token("")
    lateinit var binding: ActivitySpremeniBinding
    var datumIzbran: Date = Calendar.getInstance().time

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySpremeniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val IDProdaje = intent.getStringExtra("IDProdaje").toString()
        val Datum_prodaje = intent.getStringExtra("Datum_prodaje").toString()
        val Kolicina = intent.getStringExtra("Kolicina").toString()
        val Priimek = intent.getStringExtra("Priimek").toString()
        val Ime = intent.getStringExtra("Ime").toString()
        val IDStranke = intent.getStringExtra("IDStranke").toString()
        val Izdelek = intent.getStringExtra("Izdelek").toString()
        val Vpisal = intent.getStringExtra("Vpisal").toString()

        val spinnerStrankaValue = "$Priimek $Ime - $IDStranke"

        val formatevropski = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val datum = formatevropski.parse(Datum_prodaje)

        binding.calendarView.date = datum.time.toLong()

        binding.editTextNumberKolicina.setText(Kolicina.toString())

        binding.textViewIDProdajeSpremeni.text = IDProdaje

        datumIzbran = datum

        println(IDProdaje)
        println(Datum_prodaje)
        println(Kolicina)
        println(Priimek)
        println(Ime)
        println(IDStranke)
        println(Izdelek)
        println(Vpisal)


        val dobipodatke = CoroutineScope(Dispatchers.Default).async {
            val tokenInUrl = BranjeTokenInConfig(this@SpremeniActivity)

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
                    //Spinerju nastavi vrednosti in selecta tisto za urejanje podatkov
                    val spinner: Spinner = binding.spinnerStranka
                    val adapter = ArrayAdapter<String>(this@SpremeniActivity, R.layout.simple_list_item_1, stranke)

                    var pozicija = adapter.getPosition(spinnerStrankaValue)
                    spinner.adapter = adapter
                    spinner.setSelection(pozicija)



                    val spinnerIzdelki: Spinner = binding.spinnerIzdelek
                    val adapterIzdelki = ArrayAdapter<String>(this@SpremeniActivity, R.layout.simple_list_item_1, izdelki)

                    pozicija = adapterIzdelki.getPosition(Izdelek)
                    spinnerIzdelki.adapter = adapterIzdelki
                    spinnerIzdelki.setSelection(pozicija)

                }





            }
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    NapakaAlert("Ni povezave s strežnikom", this@SpremeniActivity)
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
                val id_prodaje = binding.textViewIDProdajeSpremeni.text

                val datumzdajinstance = Calendar.getInstance().time
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val datum = format.format(datumzdajinstance)
                val datumIzbrani = format.format(datumIzbran)

                //Preveri podatke, da niso prazni
                var naprej = true
                if(id_stranke.isNullOrEmpty()){
                    NapakaAlert("Izberite stranko", this@SpremeniActivity)
                    naprej = false
                }

                if(kolicina.isNullOrEmpty()){
                    NapakaAlert("Vpišite količino", this@SpremeniActivity)
                    naprej = false
                }

                if(izdelek.isNullOrEmpty()){
                    NapakaAlert("Izdeleka ni", this@SpremeniActivity)
                    naprej = false
                }

                if(naprej){
                    //Ustvari podatke za poslat v JSON formatu
                    val podatkiVJson = CoroutineScope(Dispatchers.Default).async {
                        //Ustvari data class prodajaPoslat
                        val podatki = prodajaSpreminjanje(id_prodaje.toString(), datumIzbrani, datum, kolicina.toString(), id_stranke, izdelek)

                        //Kliče metodo JsonUstvarjanjeProdaja, toliko, da vrne ta courutineScope neke podatke
                        JsonUstvarjanjeProdajaSpreminjanje(podatki)
                    }

                    //Se poveže in ustvari Podatke
                    CoroutineScope(Dispatchers.IO).launch {
                        val podatkiZaPoslat = podatkiVJson.await()

                        if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                            val res =
                                Jsoup.connect(url.URL + "spreminjanje.php?tabela=Prodaja").timeout(5000)
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

                                val builder = AlertDialog.Builder(this@SpremeniActivity)

                                builder.setTitle("Uspeh")
                                builder.setMessage("Uspešno spremenjeno")
                                builder.setPositiveButton("OK", null)

                                val alertDialog = builder.create()
                                alertDialog.show()

                                //Dobi ok gumb iz alertDialog ter mu nastavi lastnost, width tako, da je na sredini
                                val okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                val layoutParams = okButton.layoutParams as LinearLayout.LayoutParams
                                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                                okButton.layoutParams = layoutParams

                                okButton.setOnClickListener{
                                    alertDialog.dismiss()
                                    val intent = Intent(this@SpremeniActivity, PogledActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    startActivity(intent)

                                }

                            }

                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                NapakaAlert("Ni povezave s strežnikom", this@SpremeniActivity)
                            }
                        }

                    }



                }

            }
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    NapakaAlert("Ni povezave s strežnikom", this@SpremeniActivity)
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
        val intent = Intent(this, PogledActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}