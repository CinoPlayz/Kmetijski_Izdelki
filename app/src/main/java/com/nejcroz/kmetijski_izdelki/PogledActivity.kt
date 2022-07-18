package com.nejcroz.kmetijski_izdelki

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.nejcroz.kmetijski_izdelki.databinding.ActivityPogledBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class PogledActivity : AppCompatActivity() {
    var url: Config = Config("")
    var token : Token = Token("")
    lateinit var binding: ActivityPogledBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        //Dobi ali je dark mode enablan, če je da drugo themo
        when (this.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {setTheme(R.style.AppThemeDodajDark) }
            Configuration.UI_MODE_NIGHT_NO -> {setTheme(R.style.AppThemeDodaj)}
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {setTheme(R.style.AppThemeDodaj)}
        }

        super.onCreate(savedInstanceState)
        binding = ActivityPogledBinding.inflate(layoutInflater)
        setContentView(binding.root)



        val dobipodatke = CoroutineScope(Dispatchers.Default).async {
            val tokenInUrl = BranjeTokenInConfig(this@PogledActivity)

            url = tokenInUrl[0] as Config
            token = tokenInUrl[1] as Token

        }

        CoroutineScope(Dispatchers.IO).launch {
            dobipodatke.await()

            if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                //Dobi koliko vrstic za prikaz iz nastavitev, ki so shranjene v shared prefrance
                val prefrencekolikovrstic = PreferenceManager.getDefaultSharedPreferences(this@PogledActivity)
                val kolikovrsticFilter = prefrencekolikovrstic.getString("kolikovrstic", null)
                var kolikovrstic = 0

                if(kolikovrsticFilter.isNullOrEmpty()){
                    kolikovrstic = 25
                }
                else{
                    if(kolikovrsticFilter.toInt() < 1){
                        kolikovrstic = 25
                    }
                    else{
                        kolikovrstic = kolikovrsticFilter.toInt()
                    }
                }

                //Dobimo prodaje glede na omejitev
                val res = Jsoup.connect(url.URL + "branje.php?tabela=Prodaja&omejitev=$kolikovrstic").timeout(5000)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Authorization", "Bearer " + token.token)
                    .header("Accept", "application/json")
                    .method(Connection.Method.POST)
                    .execute()

                val dataprodaja = Gson().fromJson(res.body(), Data_Prodaja::class.java)

                if(!dataprodaja.data.isNullOrEmpty()){

                    val tl = binding.tableLayout

                    for (podatek in dataprodaja.data){

                        val tablerow1 = TableRow(this@PogledActivity)

                        val tablerowparametri = TableRow.LayoutParams (TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)

                        tablerow1.setLayoutParams(tablerowparametri)


                        val textviewIDProdaje = TextView(this@PogledActivity)
                        textviewIDProdaje.setText(podatek.id_prodaje)
                        textviewIDProdaje.setGravity(Gravity.CENTER)

                        //Spremenim height elementa tako da so vsi textview-ji na sredini verticalnega prostora
                        val textviewIDProdajeparametri = TableRow.LayoutParams (TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT)

                        textviewIDProdaje.setLayoutParams(textviewIDProdajeparametri)

                        tablerow1.addView(textviewIDProdaje)



                        val textviewDatumProdaje = TextView(this@PogledActivity)

                        val cal = Calendar.getInstance().time
                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val datumparse =  format.parse(podatek.Datum_Prodaje)
                        val formatevropski = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
                        val datum = formatevropski.format(datumparse)

                        textviewDatumProdaje.setText(datum)
                        textviewDatumProdaje.setGravity(Gravity.CENTER)

                        tablerow1.addView(textviewDatumProdaje)


                        val textviewIDStranke = TextView(this@PogledActivity)
                        textviewIDStranke.setText(podatek.id_stranke)
                        textviewIDStranke.setGravity(Gravity.CENTER)

                        tablerow1.addView(textviewIDStranke)


                        val textviewPriimek = TextView(this@PogledActivity)
                        textviewPriimek.setText(podatek.Priimek)
                        textviewPriimek.setGravity(Gravity.CENTER)
                        textviewPriimek.setPadding(5, 0, 5, 0)

                        tablerow1.addView(textviewPriimek)


                        val textviewIme = TextView(this@PogledActivity)
                        textviewIme.setText(podatek.Ime)
                        textviewIme.setGravity(Gravity.CENTER);
                        textviewIme.setPadding(5, 0, 5, 0)

                        tablerow1.addView(textviewIme)


                        val textviewIzdelek = TextView(this@PogledActivity)
                        textviewIzdelek.setText(podatek.Izdelek)
                        textviewIzdelek.setGravity(Gravity.CENTER)

                        tablerow1.addView(textviewIzdelek)

                        val textviewKolicina = TextView(this@PogledActivity)
                        textviewKolicina.setText(podatek.Koliko)
                        textviewKolicina.setGravity(Gravity.CENTER)

                        tablerow1.addView(textviewKolicina)

                        val textviewMerskaEnota = TextView(this@PogledActivity)
                        textviewMerskaEnota.setText(podatek.Merska_enota)
                        textviewMerskaEnota.setGravity(Gravity.CENTER)

                        tablerow1.addView(textviewMerskaEnota)

                        val textviewVpisal = TextView(this@PogledActivity)
                        textviewVpisal.setText(podatek.Uporabnisko_ime)
                        textviewVpisal.setGravity(Gravity.CENTER)
                        textviewVpisal.setPadding(0, 0, 20, 0)

                        tablerow1.addView(textviewVpisal)


                        val buttonUredi =  ImageButton(this@PogledActivity)
                        buttonUredi.setImageResource(R.drawable.ic_uredi)
                        buttonUredi.setOnClickListener {

                            spremeni(textviewIDProdaje.text.toString(), textviewDatumProdaje.text.toString(), textviewKolicina.text.toString(),
                            textviewPriimek.text.toString(), textviewIme.text.toString(), textviewIDStranke.text.toString(),
                            textviewIzdelek.text.toString(), textviewVpisal.text.toString())

                        }


                        tablerow1.addView(buttonUredi)


                        val buttonIzbrisi =  ImageButton(this@PogledActivity)
                        buttonIzbrisi.setImageResource(R.drawable.ic_izbrisi)
                        buttonIzbrisi.setOnClickListener {

                            izbrisi(textviewIDProdaje.text.toString(), textviewDatumProdaje.text.toString(), textviewKolicina.text.toString(),
                                textviewPriimek.text.toString(), textviewIme.text.toString(), textviewIDStranke.text.toString(),
                                textviewIzdelek.text.toString())

                        }

                        //Če samo klikne na tablerow se mu odpre zaslon za spremenit
                        tablerow1.setOnClickListener {
                            spremeni(textviewIDProdaje.text.toString(), textviewDatumProdaje.text.toString(), textviewKolicina.text.toString(),
                                textviewPriimek.text.toString(), textviewIme.text.toString(), textviewIDStranke.text.toString(),
                                textviewIzdelek.text.toString(), textviewVpisal.text.toString())
                        }

                        //Če drži dle časa na tablerow se mu odpre zaslon za izbris
                        tablerow1.setOnLongClickListener {
                            izbrisi(textviewIDProdaje.text.toString(), textviewDatumProdaje.text.toString(), textviewKolicina.text.toString(),
                                textviewPriimek.text.toString(), textviewIme.text.toString(), textviewIDStranke.text.toString(),
                                textviewIzdelek.text.toString())

                            return@setOnLongClickListener true
                        }



                        tablerow1.addView(buttonIzbrisi)



                        CoroutineScope(Dispatchers.Main).launch {
                            tl.addView(
                                tablerow1,
                                TableLayout.LayoutParams(
                                    TableRow.LayoutParams.MATCH_PARENT,
                                    TableRow.LayoutParams.WRAP_CONTENT
                                )
                            )
                        }


                    }
                }

            }
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    NapakaAlert("Ni povezave s strežnikom", this@PogledActivity)
                }
            }
        }


    }

    fun spremeni(id_prodaje: String, Datum_prodaje: String, Kolicina: String, Priimek: String,
                 Ime: String, id_stranke: String, Izdelek: String, Vpisal: String){

        val intent = Intent(this, SpremeniActivity::class.java)
        intent.putExtra("IDProdaje", id_prodaje)
        intent.putExtra("Datum_prodaje", Datum_prodaje)
        intent.putExtra("Kolicina", Kolicina)
        intent.putExtra("Priimek", Priimek)
        intent.putExtra("Ime", Ime)
        intent.putExtra("IDStranke", id_stranke)
        intent.putExtra("Izdelek", Izdelek)
        intent.putExtra("Vpisal", Vpisal)

        startActivity(intent)
    }

    fun izbrisi(id_prodaje: String, Datum_prodaje: String, Kolicina: String, Priimek: String,
                 Ime: String, id_stranke: String, Izdelek: String){

        val intent = Intent(this, IzbrisActivity::class.java)
        intent.putExtra("IDProdaje", id_prodaje)
        intent.putExtra("Datum_prodaje", Datum_prodaje)
        intent.putExtra("Kolicina", Kolicina)
        intent.putExtra("Priimek", Priimek)
        intent.putExtra("Ime", Ime)
        intent.putExtra("IDStranke", id_stranke)
        intent.putExtra("Izdelek", Izdelek)

        startActivity(intent)
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
}