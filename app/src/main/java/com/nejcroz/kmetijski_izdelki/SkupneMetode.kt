package com.nejcroz.kmetijski_izdelki

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import com.google.gson.Gson
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.util.*

data class Data_Nacrtovani_Prevzemi (
    var data: List<nacrtovani_prevzemi>){
}

data class Data_Stranke (
    var data: List<stranka>){
}

data class Data_Izdelki (
    var data: List<izdelek>){
}

data class Data_Prodaja (
    var data: List<prodaja>){
}

data class Data_Pozaba (
    var data: List<pozabe>){
}


data class nacrtovani_prevzemi (
    var id_nacrtovani_prevzem: String = "",
    var Kolicina: String = "",
    var Dan: String = "",
    var Cas: String = "",
    var Izdelek: String = "",
    var id_stranke: String = "",
    var Ime: String = "",
    var Priimek: String = "",){
}

data class prodajaPoslat (
    var Datum_Prodaje: String = "",
    var Datum_Vpisa: String = "",
    var Koliko: String = "",
    var id_stranke: String = "",
    var Izdelek: String = "",
    var Uporabnisko_ime: String = "vsejeno"){
}

data class prodajaSpreminjanje (
    var id_prodaje: String = "",
    var Datum_Prodaje: String = "",
    var Datum_Vpisa: String = "",
    var Koliko: String = "",
    var id_stranke: String = "",
    var Izdelek: String = "",
    var Uporabnisko_ime: String = "vsejeno"){
}

data class stranka (
    var id_stranke: String = "",
    var Ime: String = "",
    var Priimek: String = "",){
}

data class izdelek (
    var Izdelek: String = ""){
}

data class prodaja (
    var id_prodaje: String = "",
    var Datum_Prodaje: String = "",
    var Datum_Vpisa: String = "",
    var Koliko: String = "",
    var id_stranke: String = "",
    var Ime: String = "",
    var Priimek: String = "",
    var Izdelek: String = "",
    var Uporabnisko_ime: String = "vsejeno"){
}

data class pozabe (
    var id_stranke: String = "",
    var Ime: String = "",
    var Priimek: String = "",
    var Izdelek: String = "",
    var Datum: String = "",
    var Kolicina: String = ""){
}

    fun PovezavaObstajaStreznik(url: String): Boolean {

        try {
            val res = Jsoup.connect(url).timeout(5000)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .execute()
            return true
        }catch (e: IOException){
            return false
        }
    }

    fun DanVTednuVSlovenscini(dan: Int ): String {

        when (dan) {
            Calendar.SUNDAY -> return "Nedelja"
            Calendar.MONDAY -> return "Ponedeljek"
            Calendar.TUESDAY -> return "Torek"
            Calendar.WEDNESDAY -> return "Sreda"
            Calendar.THURSDAY -> return "Četrtek"
            Calendar.FRIDAY -> return "Petek"
            Calendar.SATURDAY -> return "Sobota"
            else ->{
                return  "Nedelja"
            }
        }
    }

    suspend fun JsonUstvarjanjeProdaja(prodaja: prodajaPoslat): String
    {
        //Ustvari Json string
        val podatkiZaPoslat = Gson().toJson(prodaja)
        return podatkiZaPoslat
    }

    suspend fun JsonUstvarjanjeProdajaSpreminjanje(prodaja: prodajaSpreminjanje): String
    {
        //Ustvari Json string
        val podatkiZaPoslat = Gson().toJson(prodaja)
        return podatkiZaPoslat
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

    fun UspehAlert(uspeh: String, context: Context){

        val builder = AlertDialog.Builder(context)

        builder.setTitle("Uspeh")
        builder.setMessage(uspeh)
        builder.setPositiveButton("OK", null)

        val alertDialog = builder.create()
        alertDialog.show()

        //Dobi ok gumb iz alertDialog ter mu nastavi lastnost, width tako, da je na sredini
        val okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val layoutParams = okButton.layoutParams as LinearLayout.LayoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        okButton.layoutParams = layoutParams

    }

    fun BranjeTokenInConfig(context: Context): Array<Any> {
        var url: Config = Config("")
        var token : Token = Token("")

        var datoteka = File(context.filesDir, "Login_Token.json")

        if (datoteka.exists()) {
            val napisano =
                File(context.filesDir.absolutePath + "/Login_Token.json").readText(Charsets.UTF_8)

            token = Gson().fromJson(napisano, Token::class.java)
        }

        datoteka = File(context.filesDir, "config.json")

        if (datoteka.exists()) {
            val napisano =
                File(context.filesDir.absolutePath + "/config.json").readText(Charsets.UTF_8)

            url = Gson().fromJson(napisano, Config::class.java)
        }

        return arrayOf(url, token)
    }
