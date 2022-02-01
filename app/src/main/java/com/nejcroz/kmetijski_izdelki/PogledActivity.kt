package com.nejcroz.kmetijski_izdelki

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.nejcroz.kmetijski_izdelki.databinding.ActivityPogledBinding
import java.io.File

class PogledActivity : AppCompatActivity() {
    lateinit var binding: ActivityPogledBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPogledBinding.inflate(layoutInflater)
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
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }
}