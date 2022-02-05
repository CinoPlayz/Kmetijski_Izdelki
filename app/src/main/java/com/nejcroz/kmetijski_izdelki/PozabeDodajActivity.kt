package com.nejcroz.kmetijski_izdelki

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.nejcroz.kmetijski_izdelki.databinding.ActivityPozabeBinding
import com.nejcroz.kmetijski_izdelki.databinding.ActivityPozabeDodajBinding
import java.io.File

class PozabeDodajActivity : AppCompatActivity() {
    lateinit var binding: ActivityPozabeDodajBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPozabeDodajBinding.inflate(layoutInflater)
        setContentView(binding.root)
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
        val intent = Intent(this, PozabeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}