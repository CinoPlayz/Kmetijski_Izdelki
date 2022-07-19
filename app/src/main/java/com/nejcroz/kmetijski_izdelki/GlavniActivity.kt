package com.nejcroz.kmetijski_izdelki

import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.nejcroz.kmetijski_izdelki.databinding.ActivityGlavniBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class GlavniActivity : AppCompatActivity() {

    lateinit var binding: ActivityGlavniBinding
    var url: Config = Config("")
    var token : Token = Token("")

    override fun onCreate(savedInstanceState: Bundle?) {
        //Dobi ali je dark mode enablan, če je da drugo themo
        when (this.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {setTheme(R.style.AppThemeDodajDark) }
            Configuration.UI_MODE_NIGHT_NO -> {setTheme(R.style.AppThemeDodaj)}
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {setTheme(R.style.AppThemeDodaj)}
        }

        super.onCreate(savedInstanceState)
        binding = ActivityGlavniBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recyclerview.layoutManager = LinearLayoutManager(this@GlavniActivity)

        //Dobi vse kar je na zaslonu in nastavi da je nevidno dokler ne dobi odziva od streznika
        binding.buttonpogled.visibility = View.INVISIBLE
        binding.buttonposlji.visibility = View.INVISIBLE
        binding.buttonpozabe.visibility = View.INVISIBLE
        binding.recyclerview.visibility = View.INVISIBLE
        binding.textViewIzbrani.visibility = View.INVISIBLE
        binding.editTextNumberKolicina.visibility = View.INVISIBLE
        binding.editTextNumberKolicina.visibility = View.INVISIBLE
        binding.fab.hide()
        binding.fabNastavitve.hide()

        //Pridobi podatke shranjene v mapi login_token in config
        val context = this@GlavniActivity

        val dobipodatke = CoroutineScope(Dispatchers.Default).async {
            val tokenInUrl = BranjeTokenInConfig(context)

            url = tokenInUrl[0] as Config
            token = tokenInUrl[1] as Token

        }

        //Testira če je mogoče se povezati
        CoroutineScope(Dispatchers.IO).launch {
            dobipodatke.await()

            if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                //Nastavi da so vsi viewi vidni
                CoroutineScope(Dispatchers.Main).launch {
                    binding.buttonpogled.visibility = View.VISIBLE
                    binding.buttonposlji.visibility = View.VISIBLE
                    binding.buttonpozabe.visibility = View.VISIBLE
                    binding.recyclerview.visibility = View.VISIBLE
                    binding.textViewIzbrani.visibility = View.VISIBLE
                    binding.editTextNumberKolicina.visibility = View.VISIBLE
                    binding.editTextNumberKolicina.visibility = View.VISIBLE
                    binding.fab.show()
                    binding.fabNastavitve.show()
                    binding.progressBarZagon.visibility = View.GONE
                }

                //Dobi trenutni datum
                val datum = Calendar.getInstance()

                val dan = DanVTednuVSlovenscini(datum.get(Calendar.DAY_OF_WEEK))

                var lahkonaprej = true

                try {
                    val resTokenVeljaven = Jsoup.connect(url.URL + "branje.php?tabela=Nacrtovani_Prevzemi").timeout(5000)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .header("Authorization", "Bearer " + token.token)
                        .header("Accept", "application/json")
                        .method(Connection.Method.POST)
                        .execute()

                    if(resTokenVeljaven.statusCode() == 401){
                        lahkonaprej = false
                    }

                }
                catch (e: IOException){
                    lahkonaprej = false
                }

                if(lahkonaprej){
                    //Dobi podatke za prikaz kdo danes naj bi prevzel podatke
                    val res = Jsoup.connect(url.URL + "branje.php?tabela=Nacrtovani_Prevzemi&dan=$dan").timeout(5000)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .header("Authorization", "Bearer " + token.token)
                        .header("Accept", "application/json")
                        .method(Connection.Method.POST)
                        .execute()

                    var data = Data_Nacrtovani_Prevzemi(listOf(nacrtovani_prevzemi("Nobeden")))

                    if(!res.body().contains("{\"sporocilo\":\"Ni najdena tabela oz. tabela je prazna\"}")){
                        //Pretvori podatke iz spletne strani v data class Data_Nacrtovani_Prevzemi
                        data = Gson().fromJson(res.body(), Data_Nacrtovani_Prevzemi::class.java)
                    }


                    //Ustvari podatke za izpis na zaslon
                    var recylerpodatki = mutableListOf<String>()

                    //Da pondtke v ustrezen array
                    if(!data.data.isNullOrEmpty()){
                        if(data.data[0].id_nacrtovani_prevzem == "Nobeden"){
                            recylerpodatki.add("Nobeden")
                        }
                        else{
                            for (podatek in data.data){
                                if(podatek.Cas_Enkrat != "null" && podatek.Cas_Enkrat != null){

                                    //Datumparser spremeni podetek.Cas_Enkrat v SimpleDateFormat objekt
                                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val datumparser =  format.parse(podatek.Cas_Enkrat)

                                    //Tuki ustvari današnji datum
                                    val datumDanes = Calendar.getInstance()
                                    datumDanes.set(Calendar.HOUR_OF_DAY, 0)
                                    datumDanes.set(Calendar.MINUTE, 0)
                                    datumDanes.set(Calendar.SECOND, 0)
                                    datumDanes.set(Calendar.MILLISECOND, 0)

                                    if(datumparser.time == datumDanes.timeInMillis){
                                        if(podatek.Merska_enota.isNullOrEmpty()){
                                            recylerpodatki.add(podatek.Cas + ": " +  podatek.Priimek + " " + podatek.Ime + " (ID:" + podatek.id_stranke + ")" + " - " +  podatek.Kolicina + " - " + podatek.Izdelek)
                                        }
                                        else{
                                            recylerpodatki.add(podatek.Cas + ": " +  podatek.Priimek + " " + podatek.Ime + " (ID:" + podatek.id_stranke + ")" + " - " +  podatek.Kolicina + " " + podatek.Merska_enota + " - " + podatek.Izdelek)
                                        }
                                    }
                                }
                                else{
                                    if(podatek.Merska_enota.isNullOrEmpty()){
                                        recylerpodatki.add(podatek.Cas + ": " +  podatek.Priimek + " " + podatek.Ime + " (ID:" + podatek.id_stranke + ")" + " - " +  podatek.Kolicina + " - " + podatek.Izdelek)
                                    }
                                    else{
                                        recylerpodatki.add(podatek.Cas + ": " +  podatek.Priimek + " " + podatek.Ime + " (ID:" + podatek.id_stranke + ")" + " - " +  podatek.Kolicina + " " + podatek.Merska_enota + " - " + podatek.Izdelek)
                                    }
                                }



                            }
                        }

                    }


                    //Dejansko spremeni elemente activity v podatke
                    CoroutineScope(Dispatchers.Main).launch {

                        //Preveri, da ni prazen list za recyclerview
                        if(!recylerpodatki.isNullOrEmpty()){
                            val recylerpodatkiArray = recylerpodatki.toTypedArray()

                            binding.recyclerview.adapter = RecyclerNovAdapter(recylerpodatkiArray){
                                if(it != "Nobeden"){
                                    val stranka = it.substringAfter(":").substringBeforeLast(" - ")
                                    val izdelek = it.substring(it.lastIndexOf(" "),it.length)
                                    binding.textViewIzbrani.text = "$stranka - $izdelek"
                                }

                            }
                        }


                    }

                }
                else{
                    CoroutineScope(Dispatchers.Main).launch {
                        val builder = AlertDialog.Builder(context)

                        builder.setTitle("Napaka")
                        builder.setMessage("Token je neveljaven (Možno, da se je nekdo prijavil na drugi napravi z istim uporabniškim imenom)")
                        builder.setPositiveButton("OK", null)

                        val alertDialog = builder.create()
                        alertDialog.show()

                        //Dobi ok gumb iz alertDialog ter mu nastavi lastnost, width tako, da je na sredini
                        val okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        val layoutParams = okButton.layoutParams as LinearLayout.LayoutParams
                        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        okButton.layoutParams = layoutParams

                        //Odjavi se
                        okButton.setOnClickListener{
                            alertDialog.dismiss()
                            val context = this@GlavniActivity

                            var datoteka = File(context.filesDir, "Login_Token.json")

                            if (datoteka.exists()){
                                datoteka.delete()
                            }

                            datoteka = File(context.filesDir, "config.json")

                            if (datoteka.exists()){
                                datoteka.delete()
                            }

                            val intent = Intent(context, LoginActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)

                        }
                    }
                }


            }
            else {
                //Če ni povezave s strežnikom
                CoroutineScope(Dispatchers.Main).launch {
                    val builder = AlertDialog.Builder(context)

                    builder.setTitle("Napaka")
                    builder.setMessage("Ni povezave s strežnikom")
                    builder.setPositiveButton("Osveži", null)
                    builder.setNegativeButton("Zapri", null)
                    builder.setNeutralButton("Odjava", null)

                    val alertDialog = builder.create()
                    alertDialog.show()

                    //Dobi osveži in zapri gumb iz alertDialog
                    val osveziButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val zapriButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    val odjavaButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL)

                    //Ponovno se ustvari activity
                    osveziButton.setOnClickListener{
                        alertDialog.dismiss()
                        recreate()
                    }

                    //Ponovno se ustvari activity
                    zapriButton.setOnClickListener{
                        alertDialog.dismiss()
                        finish()
                    }

                    odjavaButton.setOnClickListener{
                        alertDialog.dismiss()
                        odjava(binding.root)
                    }


                }
            }
        }



    }

    fun poslji(view: View) {
        val textIzbrani = binding.textViewIzbrani.text.toString()

        if(textIzbrani != "Izbrani: Nobeden"){
            val id_stranke = textIzbrani.substringAfterLast("(ID:").substringBeforeLast(") - ")

            val kolicina = binding.editTextNumberKolicina.text
            val izdelek = textIzbrani.substring(textIzbrani.lastIndexOf(" - ") + 4, textIzbrani.length)

            val datumzdaj = Calendar.getInstance().time
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val datum = format.format(datumzdaj)

            //Preveri podatke, da niso prazni
            var naprej = true
            if(id_stranke.isNullOrEmpty()){
                NapakaAlert("Izberite stranko", this@GlavniActivity)
                naprej = false
            }

            if(kolicina.isNullOrEmpty()){
                NapakaAlert("Vpišite količino", this@GlavniActivity)
                naprej = false
            }

            if(izdelek.isNullOrEmpty()){
                NapakaAlert("Izdeleka ni", this@GlavniActivity)
                naprej = false
            }

            if(naprej){

                //Ustvari podatke za poslat v JSON formatu
                val podatkiVJson = CoroutineScope(Dispatchers.Default).async {
                    //Ustvari data class prodajaPoslat
                    val podatki = prodajaPoslat(datum, datum, kolicina.toString(), id_stranke, izdelek.toString())

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
                            UspehAlert("Uspešno dodano", this@GlavniActivity)
                        }

                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            NapakaAlert("Ni povezave s strežnikom", this@GlavniActivity)
                        }
                    }

                }
            }
        }
        else{
            NapakaAlert("Izberite načrtovani prevzem", this@GlavniActivity)
        }
    }

    fun pogled(view: View) {
        val intent = Intent(this, PogledActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun pozabe(view: View) {
        val intent = Intent(this, PozabeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun odjava(view: View) {
        val context = this
        SkupnaOdjava(context)
    }

    fun dodaj(view: View) {
        val intent = Intent(this, DodajActivity::class.java)
        startActivity(intent)
    }

    fun nastavitve(view: View) {
        val intent = Intent(this, NastavitveActivity::class.java)
        startActivity(intent)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }

    //Ustvarjen custom adapter za recyclerview
    class RecyclerNovAdapter(private val podatkiZbrika: Array<String>,
                             private val clickListener: (String) -> Unit) : RecyclerView.Adapter<RecyclerNovAdapter.ViewHolder>(){

        //Spodnji class je metadata za item v Recyclerview-ju, inicializira spremenljivko textview in ustvari setOnClickListiner za view
        class ViewHolder(view: View, clickPozicija: (Int) -> Unit) : RecyclerView.ViewHolder(view){
            val textView: TextView = view.findViewById(R.id.textViewItem)

            init {
                view.setOnClickListener {
                    clickPozicija(adapterPosition)
                }
            }
        }

        //Ko se viewholder ustvari, spremeni layout in doda ta item v recyclerview, poleg tega pa še nastavi spremenljivko clickListener na string s podatki
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_items, parent, false)){
                clickListener(podatkiZbrika[it])
            }

            return  view
        }

        //Nastavi text za textview
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = podatkiZbrika[position]
        }

        override fun getItemCount() = podatkiZbrika.size
    }




}