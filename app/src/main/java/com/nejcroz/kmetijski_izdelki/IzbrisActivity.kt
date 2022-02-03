package com.nejcroz.kmetijski_izdelki

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.nejcroz.kmetijski_izdelki.databinding.ActivityIzbrisBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File

class IzbrisActivity : AppCompatActivity() {
    var url: Config = Config("")
    var token : Token = Token("")
    lateinit var binding: ActivityIzbrisBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIzbrisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Dobi podatke iz intenta in jih da v textboxe
        val Datum_prodaje = intent.getStringExtra("Datum_prodaje").toString()
        val Kolicina = intent.getStringExtra("Kolicina").toString()
        val Priimek = intent.getStringExtra("Priimek").toString()
        val Ime = intent.getStringExtra("Ime").toString()
        val IDStranke = intent.getStringExtra("IDStranke").toString()
        val Izdelek = intent.getStringExtra("Izdelek").toString()

        binding.textViewDatum.text = Datum_prodaje
        binding.textViewStranka.text = "$Priimek $Ime - $IDStranke"
        binding.textViewKolicina2.text = Kolicina
        binding.textViewIzdelek2.text = Izdelek


        CoroutineScope(Dispatchers.Default).launch {
            val tokenInUrl = BranjeTokenInConfig(this@IzbrisActivity)

            url = tokenInUrl[0] as Config
            token = tokenInUrl[1] as Token

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

    fun poslji(view: View) {
        //Ustvari podatke za poslat v JSON formatu
        val podatkiVJson = CoroutineScope(Dispatchers.Default).async {

            val IDProdaje = intent.getStringExtra("IDProdaje").toString()

            //Ustvari data class prodajaPoslat
            val podatki = prodajaSpreminjanje(IDProdaje)

            //Kliče metodo JsonUstvarjanjeProdaja, toliko, da vrne ta courutineScope neke podatke
            JsonUstvarjanjeProdajaSpreminjanje(podatki)
        }

        CoroutineScope(Dispatchers.IO).launch {


            if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {
                val podatkiZaPoslat = podatkiVJson.await()

                val res = Jsoup.connect(url.URL + "izbris.php?tabela=Prodaja").timeout(5000)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Authorization", "Bearer " + token.token)
                    .header("Accept", "application/json")
                    .requestBody(podatkiZaPoslat)
                    .method(Connection.Method.POST)
                    .execute()

                //Uspešno izbriše se to izpiše
                CoroutineScope(Dispatchers.Main).launch {

                    val builder = AlertDialog.Builder(this@IzbrisActivity)

                    builder.setTitle("Uspeh")
                    builder.setMessage("Uspešno izbrisano")
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
                        val intent = Intent(this@IzbrisActivity, PogledActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)

                    }

                }

            }
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    NapakaAlert("Ni povezave s strežnikom", this@IzbrisActivity)
                }
            }
    }   }
}