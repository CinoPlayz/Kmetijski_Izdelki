package com.nejcroz.kmetijski_izdelki

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import com.google.gson.Gson
import com.nejcroz.kmetijski_izdelki.databinding.ActivityIzbrisBinding
import com.nejcroz.kmetijski_izdelki.databinding.ActivityPozabeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class PozabeActivity : AppCompatActivity() {
    var url: Config = Config("")
    var token : Token = Token("")
    lateinit var binding: ActivityPozabeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPozabeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dobipodatke = CoroutineScope(Dispatchers.Default).async {
            val tokenInUrl = BranjeTokenInConfig(this@PozabeActivity)

            url = tokenInUrl[0] as Config
            token = tokenInUrl[1] as Token

        }

        CoroutineScope(Dispatchers.IO).launch {
            dobipodatke.await()

            if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                //Ustvari datum, ki je sedem dni nazaj
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_MONTH, -7)
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val datumparse =  format.format(cal.time)
                val datum = "$datumparse 00:00:00"

                //Dobimo pozabe glede na datume
                val res =
                    Jsoup.connect(url.URL + "pozabe.php").timeout(5000)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .header("Authorization", "Bearer " + token.token)
                        .header("Accept", "application/json")
                        .method(Connection.Method.POST)
                        .requestBody("{ \"Datum_Zacetek\":\"$datum\"}")
                        .execute()

                val datapozabe = Gson().fromJson(res.body(), Data_Pozaba::class.java)

                if (!datapozabe.data.isNullOrEmpty()) {

                    val tl = binding.tableLayout

                    for(podatek in datapozabe.data){

                        val tablerow1 = TableRow(this@PozabeActivity)

                        val tablerowparametri = TableRow.LayoutParams (TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)

                        tablerow1.setLayoutParams(tablerowparametri)


                        val datumparser =  format.parse(podatek.Datum)
                        val formatevropski = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        val datum = formatevropski.format(datumparser)



                        val textviewPozabe = TextView(this@PozabeActivity)
                        val textviewPovezaveparametri = TableRow.LayoutParams (TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT, 1f)

                        textviewPozabe.setLayoutParams(textviewPovezaveparametri)

                        textviewPozabe.setText(podatek.Ime + " " + podatek.Priimek + " (ID: " + podatek.id_stranke + ") \n" + datum + "\n" + podatek.Kolicina + " - " + podatek.Izdelek)
                        textviewPozabe.setGravity(Gravity.CENTER)
                        textviewPozabe.textSize = 18F
                        textviewPozabe.isSingleLine = false
                        textviewPozabe.isClickable = true
                        textviewPozabe.setPadding(0,0,0,10)

                        tablerow1.addView(textviewPozabe)

                        CoroutineScope(Dispatchers.Main).launch {
                            tl.addView(
                                tablerow1,
                                TableLayout.LayoutParams(
                                    TableRow.LayoutParams.MATCH_PARENT,
                                    TableRow.LayoutParams.WRAP_CONTENT
                                )
                            )
                        }

                        textviewPozabe.setOnClickListener {
                            val intent = Intent(this@PozabeActivity, PozabeDodajActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)

                        }
                    }
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
}