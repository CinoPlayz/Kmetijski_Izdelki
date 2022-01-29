package com.nejcroz.kmetijski_izdelki

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.nejcroz.kmetijski_izdelki.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.io.FileWriter
import java.io.IOException


class LoginActivity : AppCompatActivity() {
     lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun poslji(view: View) {
        val URL = binding.editTextTextURL.text.toString().replace(" ", "%20")
        val UprIme = binding.editTextTextUprIme.text
        val Geslo = binding.editTextTextGeslo.text
        var naprej = true

        if(URL.isNullOrEmpty()){
            NapakaAlert("Vpišite URL do mape api", this)
            naprej = false
        }

        if(UprIme.isNullOrEmpty()){
            NapakaAlert("Vpišite uporabniško ime", this)
            naprej = false
        }

        if(Geslo.isNullOrEmpty()){
            NapakaAlert("Vpišite geslo", this)
            naprej = false
        }

        if(naprej){

            //Ustvari podatke za poslat v JSON formatu
            val podatkiVJson = CoroutineScope(Dispatchers.Default).async {
                //Ustvari data class Prijava
                val podatki = Prijava(UprIme.toString(), Geslo.toString())

                //Kliče metodo JsonPrijava, toliko, da vrne ta courutineScope neke podatke
                JsonPrijava(podatki)
            }


            CoroutineScope(Dispatchers.IO).launch {
                val podatkiZaPoslat = podatkiVJson.await()

                //Ustvari URL ter preveri če je veljaven
                val url = "$URL/api/prijava.php"

                naprej = Patterns.WEB_URL.matcher(url).matches()

                if(naprej){

                    //Preveri če obstaja strežnik (če se lahka poveže)
                    if(PovezavaObstajaStreznik(url)){

                        val res = Jsoup.connect(url).timeout(5000)
                            .ignoreHttpErrors(true)
                            .ignoreContentType(true)
                            .header("Content-Type", "application/json;charset=UTF-8")
                            .header("Accept", "application/json")
                            .requestBody(podatkiZaPoslat)
                            .method(Connection.Method.POST)
                            .execute()

                        //Če vrne 400 (največkrat če so podatki narobe to naredi) izpiše da ni pravilno uporabniško ime oz. geslo
                        if(res.statusCode() == 400){
                            CoroutineScope(Dispatchers.Main).launch {
                                NapakaAlert("Uporabniško ime oz. Geslo je narobe", this@LoginActivity)
                            }
                        }

                        //Če vrne 200 pomeni, da je vse vredu in nadaljuje
                        if(res.statusCode() == 200){
                            val IzJson = CoroutineScope(Dispatchers.Default).async {

                                //Kliče metodo JsonPodatki, toliko, da vrne ta courutineScope neke podatke
                                JsonPodatki(res.body())
                            }

                            val Vrnjeno = IzJson.await()


                            //Ustvari objekt Token In ga spremeni v JSON format
                            val TokenObjekt = Token(Vrnjeno.podatki)

                            val tokenJson = Gson().toJson(TokenObjekt)

                            //Ustvari datoteko Login_Token.json kjer je shranjen token
                            val context = this@LoginActivity

                            var datoteka = File(context.filesDir, "Login_Token.json")

                            if (!datoteka.exists()){

                                try {
                                    FileWriter(datoteka).use { it.write(tokenJson) }
                                }
                                catch (t: Throwable){
                                    println(t.message)
                                }

                            }
                            else{
                                File(context.filesDir, "Login_Token.json").writeText(tokenJson)
                            }

                            //Ustvari objekt Config In ga spremeni v JSON format
                            val ConfigObjekt = Config("$URL/api/")

                            val configJson = Gson().toJson(ConfigObjekt)

                            //Ustvari datoteko Login_Token.json kjer je shranjen token
                            datoteka = File(context.filesDir, "config.json")

                            if (!datoteka.exists()){

                                try {
                                    FileWriter(datoteka).use { it.write(configJson) }
                                }
                                catch (t: Throwable){
                                    println(t.message)
                                }

                            }
                            else{
                                File(context.filesDir, "config.json").writeText(configJson)
                            }
                        }



                    }
                    else{
                        CoroutineScope(Dispatchers.Main).launch {
                            NapakaAlert("Ni povezave s strežnikom (preverite, da se začne URL z http oz. https format URL-ja \" http[s]://[IP Naslov oz. domena]/pot do mape api \" )", this@LoginActivity)
                        }

                    }

                }
                else{

                    CoroutineScope(Dispatchers.Main).launch {
                        NapakaAlert("Vpišite veljaven URL", this@LoginActivity)
                    }

                }









                //val doc: String? = Jsoup.connect("http://192.168.1.5:81/JajcaPHP/IzbrisJajca.php?IDprodaje=$IDprodaje&token=$token").get().html()

                //val dobljenipodatki = Jsoup.parse(doc).text()
            }
        }

    }

    suspend fun JsonPrijava(prijava: Prijava): String
    {
        //Ustvari Json string
        val podatkiZaPoslat = Gson().toJson(prijava)
        return podatkiZaPoslat
    }

    suspend fun JsonPodatki(podatki: String): Podatki
    {
        //Ustvari Objekt Podatki iz prejetega JSON-a
        val podatkiZaPoslat = Gson().fromJson(podatki, Podatki::class.java)

        return podatkiZaPoslat
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
        }
        return super.dispatchTouchEvent(ev)
    }
}

data class Prijava (
    var Uporabnisko_ime: String = "",
    var Geslo: String = "") {
}

data class Podatki (
    var podatki: String = ""){
}

data class Token (
    var token: String = ""){
}

data class Config (
    var URL: String = ""){
}

fun PovezavaObstajaStreznik(url: String): Boolean {
    try {
        val res = Jsoup.connect(url).timeout(5000)
            .ignoreHttpErrors(true)
            .ignoreContentType(true)
            .execute()
        return  true
    }catch (e: IOException){
        return false
    }
}

fun NapakaAlert(napaka: String, context: Context){

    val builder = AlertDialog.Builder(context)

    builder.setTitle("Napaka")
    builder.setMessage(napaka)
    builder.setPositiveButton("OK", null)

    val alertDialog = builder.create()
    alertDialog.show()

    //Dobi ok gumb iz alertDialog ter mu nastavi lastnost, width tako, da je na sredini
    val okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
    val layoutParams = okButton.layoutParams as LinearLayout.LayoutParams
    layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
    okButton.layoutParams = layoutParams

}
