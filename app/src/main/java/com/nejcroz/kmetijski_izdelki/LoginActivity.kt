package com.nejcroz.kmetijski_izdelki

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import com.nejcroz.kmetijski_izdelki.databinding.ActivityLoginBinding
import kotlinx.coroutines.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.w3c.dom.Document
import java.io.File
import java.io.FileWriter
import java.io.IOException


class LoginActivity : AppCompatActivity() {
     lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        //Dobi ali je dark mode enablan, če je da drugo themo
        when (this.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)) {
            Configuration.UI_MODE_NIGHT_YES -> {setTheme(com.nejcroz.kmetijski_izdelki.R.style.AppThemeDodajDark) }
            Configuration.UI_MODE_NIGHT_NO -> {setTheme(com.nejcroz.kmetijski_izdelki.R.style.AppThemeDodaj)}
            Configuration.UI_MODE_NIGHT_UNDEFINED -> {setTheme(com.nejcroz.kmetijski_izdelki.R.style.AppThemeDodaj)}
        }
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val context = this

        var datoteka = File(context.filesDir, "Login_Token.json")

        var obstaja = 0

        if (datoteka.exists()){
            val napisano = File(context.filesDir.absolutePath + "/Login_Token.json").readText(Charsets.UTF_8)
            if(napisano.length > 91){
                obstaja++
            }
        }

        datoteka = File(context.filesDir, "config.json")

        if (datoteka.exists()){
            val napisano = File(context.filesDir.absolutePath + "/config.json").readText(Charsets.UTF_8)
            if(napisano.length > 4){
                obstaja++
            }
        }

        if(obstaja == 2){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }


        //Preveri če ima intent zraven URL, če ga ima nastavi edittextbox za URL na ta url, ki je bil v intentu, drugače ne naredi ničesar
        var URL = ""

        val extra = intent.extras

        if(extra != null){
            if(extra.containsKey("URL")){
                URL = intent.getStringExtra("URL").toString()
            }
        }

        if(!URL.isNullOrEmpty()){
            URL = URL.replace("https://", "")
            URL = URL.replace("http://", "")
            URL = URL.replace("/api/", "")
            binding.editTextTextURL.setText(URL)
        }



    }

    fun poslji(view: View) {
        var URL = binding.editTextTextURL.text.toString().replace(" ", "%20")
        val UprIme = binding.editTextTextUprIme.text
        val Geslo = binding.editTextTextGeslo.text
        var naprej = true

        if(URL.isEmpty()){
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
                var url = "$URL/api/prijava.php"

                naprej = Patterns.WEB_URL.matcher(url).matches()

                if(naprej){

                    if(UporabljaHttps(URL)){
                        url = "https://$URL/api/prijava.php"
                        URL = "https://$URL"
                    }
                    else{
                        url = "http://$URL/api/prijava.php"
                        URL = "http://$URL"
                    }

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

                        //Če vrne 404, kar pomeni, da ni bila najdena mapa api na strežniku naredi spodnje
                        if(res.statusCode() == 404){
                            CoroutineScope(Dispatchers.Main).launch {
                                NapakaAlert("Ni povezave s strežnikom (preverite, da je URL v pravilnem formatu \" [IP Naslov oz. domena]/[pot do mape api] \" )", this@LoginActivity)
                            }
                        }

                        //Če vrne 200 pomeni, da je vse vredu in nadaljuje
                        if(res.statusCode() == 200){
                            //Preveri če so vrnjeni podatki takšni ko bi jih vrnil api (pač če ima response body nekatere stvari, ki jih vrne api)
                            if(res.body().contains("{\"podatki\":")){

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

                                CoroutineScope(Dispatchers.Main).launch {
                                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                    startActivity(intent)
                                }


                            }
                            else{
                                CoroutineScope(Dispatchers.Main).launch {
                                    NapakaAlert("Strežnik nima api mape oz. URL, ki ste ga vnesili je nepravilen", this@LoginActivity)
                                }
                            }

                        }



                    }
                    else{
                        CoroutineScope(Dispatchers.Main).launch {
                            NapakaAlert("Ni povezave s strežnikom (preverite, da je URL v pravilnem formatu \" [IP Naslov oz. domena]/[pot do mape api] \" )", this@LoginActivity)
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

    fun JsonPrijava(prijava: Prijava): String
    {
        //Ustvari Json string
        val podatkiZaPoslat = Gson().toJson(prijava)
        return podatkiZaPoslat
    }

    fun JsonPodatki(podatki: String): Podatki
    {
        //Ustvari Objekt Podatki iz prejetega JSON-a
        val podatkiZaPoslat = Gson().fromJson(podatki, Podatki::class.java)

        return podatkiZaPoslat
    }

    fun UporabljaHttps(URL: String): Boolean{
        try {
            Jsoup.connect("https://$URL")
            .timeout(6000)
            .execute()

            return true
        }
        catch (e: IOException){
                //Preveri če je slučajno vrjen forbiden za domeno, če je kljub temu vrne da je https
                if(e.message.toString().contains("Status=403", false)){
                    return true
                }

            return false
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


