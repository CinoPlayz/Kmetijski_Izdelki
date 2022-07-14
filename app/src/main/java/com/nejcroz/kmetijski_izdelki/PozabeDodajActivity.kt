package com.nejcroz.kmetijski_izdelki

import android.content.Intent
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.nejcroz.kmetijski_izdelki.databinding.ActivityPozabeBinding
import com.nejcroz.kmetijski_izdelki.databinding.ActivityPozabeDodajBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PozabeDodajActivity : AppCompatActivity() {
    lateinit var binding: ActivityPozabeDodajBinding
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
        binding = ActivityPozabeDodajBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Dobimo podatke URL in Token iz json datoteki
        CoroutineScope(Dispatchers.Default).launch {
            val tokenInUrl = BranjeTokenInConfig(this@PozabeDodajActivity)

            url = tokenInUrl[0] as Config
            token = tokenInUrl[1] as Token

        }

        //Dobi podatke iz intenta in jih da v textboxe
        val Datum = intent.getStringExtra("Datum").toString()
        val Kolicina = intent.getStringExtra("Kolicina").toString()
        val Priimek = intent.getStringExtra("Priimek").toString()
        val Ime = intent.getStringExtra("Ime").toString()
        val IDStranke = intent.getStringExtra("IDStranke").toString()
        val Izdelek = intent.getStringExtra("Izdelek").toString()

        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val datumparser =  format.parse(Datum)
        val formatevropski = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val datum = formatevropski.format(datumparser)


        binding.textViewDatumPozabe.text = datum
        binding.textViewStrankaPozabe.text = "$Priimek $Ime (ID: $IDStranke)"
        binding.textViewIzdelekPozabe.text = Izdelek
        binding.editTextNumberKolicina.setText(Kolicina.toString())

    }

    fun odjava(view: View) {
        val context = this
        SkupnaOdjava(context)
    }

    fun nazaj(view: View) {
        val intent = Intent(this, PozabeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun poslji(view: View) {

        CoroutineScope(Dispatchers.IO).launch {

            if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                //Dobimo podatke iz textview
                val stranka =
                    binding.textViewStrankaPozabe.text.toString().substringAfterLast("(ID: ")
                        .replace(")", "")
                val izdelek = binding.textViewIzdelekPozabe.text.toString()

                val formatevropski = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                val datumparser = formatevropski.parse(binding.textViewDatumPozabe.text.toString())
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val datumpol = format.format(datumparser)
                val datum = "$datumpol 01:00:00"

                val datumzdajCal = Calendar.getInstance().time
                val formatzdaj = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val datumzdaj = formatzdaj.format(datumzdajCal)

                val kolicina = binding.editTextNumberKolicina.text.toString()

                //Preveri da ni kolicina prazna
                var naprej = true

                if (kolicina.isNullOrEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        NapakaAlert("Vpišite količino", this@PozabeDodajActivity)
                    }
                    naprej = false
                }

                if (naprej) {
                    //Ustvari podatke za poslat v JSON formatu
                    val podatkiVJson = CoroutineScope(Dispatchers.Default).async {
                        //Ustvari data class prodajaPoslat
                        val podatki = prodajaPoslat(datum, datumzdaj, kolicina, stranka, izdelek)

                        //Kliče metodo JsonUstvarjanjeProdaja, toliko, da vrne ta courutineScope neke podatke
                        JsonUstvarjanjeProdaja(podatki)
                    }

                    //Se poveže in ustvari Podatke
                    CoroutineScope(Dispatchers.IO).launch {
                        val podatkiZaPoslat = podatkiVJson.await()

                        if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                            val res =
                                Jsoup.connect(url.URL + "ustvarjanje.php?tabela=Prodaja")
                                    .timeout(5000)
                                    .ignoreHttpErrors(true)
                                    .ignoreContentType(true)
                                    .header("Content-Type", "application/json;charset=UTF-8")
                                    .header("Authorization", "Bearer " + token.token)
                                    .header("Accept", "application/json")
                                    .requestBody(podatkiZaPoslat)
                                    .method(Connection.Method.POST)
                                    .execute()

                            //Uspešno doda se to izpiše (Ustvari svoj AlertDialog in potem se odpre activity pozabe)
                            CoroutineScope(Dispatchers.Main).launch {
                                val builder = AlertDialog.Builder(this@PozabeDodajActivity)

                                builder.setTitle("Uspeh")
                                builder.setMessage("Uspešno dodano")
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
                                    val intent = Intent(this@PozabeDodajActivity, PozabeActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    startActivity(intent)

                                }
                            }

                        } else {
                            CoroutineScope(Dispatchers.Main).launch {
                                NapakaAlert("Ni povezave s strežnikom", this@PozabeDodajActivity)
                            }
                        }

                    }
                }


            }
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    NapakaAlert("Ni povezave s strežnikom", this@PozabeDodajActivity)
                }
            }
        }



    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }
}