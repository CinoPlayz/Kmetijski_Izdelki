package com.nejcroz.kmetijski_izdelki

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import com.nejcroz.kmetijski_izdelki.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup

class LoginActivity : AppCompatActivity() {
     lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    fun poslji(view: View) {
        val URL = binding.editTextTextURL.text
        val UprIme = binding.editTextTextUprIme.text
        val Geslo = binding.editTextTextGeslo.text
        var naprej = true

        if(URL.isNullOrEmpty()){
            Toast.makeText(this, "Vpišite IP Naslov", Toast.LENGTH_LONG).show()
            naprej = false
        }

        if(UprIme.isNullOrEmpty()){
            Toast.makeText(this, "Vpišite uporabniško ime", Toast.LENGTH_LONG).show()
            naprej = false
        }

        if(Geslo.isNullOrEmpty()){
            Toast.makeText(this, "Vpišite geslo", Toast.LENGTH_LONG).show()
            naprej = false
        }

        if(naprej){

            /*val podatkiVJson = CoroutineScope(Dispatchers.Default).async {
                //Ustvari data class Prijava
                val podatki = Prijava(UprIme.toString(), Geslo.toString())

                //Kliče metodo JsonPrijava, toliko, da vrne ta courutineScope neke podatke
                JsonPrijava(podatki)
            }*/


            CoroutineScope(Dispatchers.IO).launch {
                //val podatkiZaPoslat = podatkiVJson.await()

                println("$URL/api/prijava.php")

                val res = Jsoup.connect("$URL/api/prijava.php").timeout(60000)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .method(Connection.Method.POST)
                    .data("Uporabnisko_ime", UprIme.toString())
                    .data("Geslo", Geslo.toString())
                    .get()




                println(res)

                //val doc: String? = Jsoup.connect("http://192.168.1.5:81/JajcaPHP/IzbrisJajca.php?IDprodaje=$IDprodaje&token=$token").get().html()

                //val dobljenipodatki = Jsoup.parse(doc).text()
            }
        }

    }

    /*suspend fun JsonPrijava(prijava: Prijava): String
    {
        //Ustvari Json string
        val podatkiZaPoslat = Gson().toJson(prijava)
        return podatkiZaPoslat
    }*/
}

/*data class Prijava (
    var Uporabnisko_ime: String = "",
    var Geslo: String = "") {
}*/