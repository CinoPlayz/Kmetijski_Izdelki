package com.nejcroz.kmetijski_izdelki

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException

data class Data_Nacrtovani_Prevzemi (
    var data: List<nacrtovani_prevzemi>){
}

data class nacrtovani_prevzemi (
    var id_nacrtovani_prevzem: String = "",
    var Kolicina: String = "",
    var Dan: String = "",
    var Cas: String = "",
    var Izdelek: String = "",
    var id_stranke: String = "",){
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
