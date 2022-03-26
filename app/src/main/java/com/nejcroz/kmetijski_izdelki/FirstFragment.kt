package com.nejcroz.kmetijski_izdelki

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.nejcroz.kmetijski_izdelki.databinding.FragmentFirstBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    //Inicializira url in token spremenljivki
    var url: Config = Config("")
    var token : Token = Token("")

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(

        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        //list za id stranke in količino

        var strankakolicina = mutableListOf<Array<String>>()

        binding.recyclerview.layoutManager = LinearLayoutManager(context)

        //Pridobi podatke shranjene v mapi login_token in config
        val context = requireContext()

        val dobipodatke = CoroutineScope(Dispatchers.Default).async {
            val tokenInUrl = BranjeTokenInConfig(context)

            url = tokenInUrl[0] as Config
            token = tokenInUrl[1] as Token

        }

        //Testira če je mogoče se povezati
        CoroutineScope(Dispatchers.IO).launch {
            dobipodatke.await()

            if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                //Dobi trenutni datum
                val datum = Calendar.getInstance()

                val dan = DanVTednuVSlovenscini(datum.get(Calendar.DAY_OF_WEEK))

                var lahkonaprej = true

                try {
                    val resTokenVeljaven = Jsoup.connect(url.URL + "branje.php?tabela=Nacrtovani_prevzemi").timeout(5000)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .header("Authorization", "Bearer " + token.token)
                        .header("Accept", "application/json")
                        .method(Connection.Method.POST)
                        .execute()

                    if(resTokenVeljaven.statusCode() == 401){
                        lahkonaprej = false
                    }

                }
                catch (e: IOException){
                    lahkonaprej = false
                }

                if(lahkonaprej){
                    //Dobi podatke za prikaz kdo danes naj bi prevzel podatke
                    val res = Jsoup.connect(url.URL + "branje.php?tabela=Nacrtovani_prevzemi&dan=$dan").timeout(5000)
                        .ignoreHttpErrors(true)
                        .ignoreContentType(true)
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .header("Authorization", "Bearer " + token.token)
                        .header("Accept", "application/json")
                        .method(Connection.Method.POST)
                        .execute()

                    //Pretvori podatke iz spletne strani v data class Data_Nacrtovani_Prevzemi
                    val data = Gson().fromJson(res.body(), Data_Nacrtovani_Prevzemi::class.java)

                    //Ustvari podatke za izpis na zaslon
                    var ZaizpisVTextView = ""

                    var stranke = mutableListOf<String>()

                    var recylerpodatki = mutableListOf<String>()

                    //Da pondtke v ustrezen array
                    if(!data.data.isNullOrEmpty()){
                        for (podatek in data.data){
                            ZaizpisVTextView += podatek.Cas + ": " +  podatek.Priimek + " " + podatek.Ime + " (ID:" + podatek.id_stranke + ")" +" - " +  podatek.Kolicina + " " + podatek.Izdelek + "\n"
                            stranke.add(podatek.Priimek + " " + podatek.Ime + " - " + podatek.id_stranke)
                            strankakolicina.add(arrayOf(podatek.id_stranke, podatek.Kolicina, podatek.Izdelek))
                            recylerpodatki.add(podatek.Cas + ": " +  podatek.Priimek + " " + podatek.Ime + " (ID:" + podatek.id_stranke + ")" +" - " +  podatek.Kolicina + " " + podatek.Izdelek)

                        }
                    }
                    else{
                        ZaizpisVTextView += "Nobeden"
                    }

                    //Dejansko spremeni elemente activity v podatke
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.textviewKdoDanes.text = ZaizpisVTextView


                        val recylerpodatkiArray = recylerpodatki.toTypedArray()


                        binding.recyclerview.adapter = RecyclerNovAdapter(recylerpodatkiArray){
                            println(it)
                        }


                        val spinner: Spinner = binding.spinnerStranke
                        spinner.adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, stranke)
                    }

                }
                else{
                    CoroutineScope(Dispatchers.Main).launch {
                        val builder = AlertDialog.Builder(requireContext())

                        builder.setTitle("Napaka")
                        builder.setMessage("Token je neveljaven (Možno, da se je nekdo prijavil na drugi napravi z istim uporabniškim imenom)")
                        builder.setPositiveButton("OK", null)

                        val alertDialog = builder.create()
                        alertDialog.show()

                        //Dobi ok gumb iz alertDialog ter mu nastavi lastnost, width tako, da je na sredini
                        val okButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        val layoutParams = okButton.layoutParams as LinearLayout.LayoutParams
                        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                        okButton.layoutParams = layoutParams

                        //Odjavi se
                        okButton.setOnClickListener{
                            alertDialog.dismiss()
                            val context = requireContext()

                            var datoteka = File(context.filesDir, "Login_Token.json")

                            if (datoteka.exists()){
                                datoteka.delete()
                            }

                            datoteka = File(context.filesDir, "config.json")

                            if (datoteka.exists()){
                                datoteka.delete()
                            }

                            val intent = Intent(context, LoginActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(intent)

                        }
                    }
                }


            }
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    NapakaAlert("Ni povezave s strežnikom", context)
                    binding.buttonpogled.isEnabled = false
                    binding.buttonposlji.isEnabled = false
                    binding.buttonpozabe.isEnabled = false
                }
            }
        }


        //Spodnja koda se izvede, ko spremenimo, kaj je selectano na spinnerju. To spremeni kaj je napisano v editTextnumber,
        //glede na koliko ima vpisana stranka za tisti dan
        binding.spinnerStranke.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                var kolicina = 0
                var izdelek = ""
                val id_stranke = binding.spinnerStranke.selectedItem.toString().substringAfterLast(" - ")

                //Preveri kolikokrat je stranka danes navrsti za prevzem
                var stranka_danes = mutableListOf<Int>()

                strankakolicina.forEachIndexed { index, strings -> if(strings[0] == id_stranke){
                        stranka_danes.add(index)
                    }
                }

                if(stranka_danes.size == 1){
                    kolicina = strankakolicina[stranka_danes[0]][1].toInt()
                    izdelek = strankakolicina[stranka_danes[0]][2]
                }
                else{
                    kolicina = strankakolicina[position][1].toInt()
                    izdelek = strankakolicina[position][2]
                }




                binding.editTextNumberKolicina.setText(kolicina.toString())
                binding.textViewIzdelek.text = izdelek
            }

        }

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }*/

        binding.buttonposlji.setOnClickListener {
            var id_stranke: String = ""
            if(binding.spinnerStranke.selectedItem != null){
                id_stranke = binding.spinnerStranke.selectedItem.toString().substringAfterLast(" - ")
            }

            val kolicina = binding.editTextNumberKolicina.text
            val izdelek = binding.textViewIzdelek.text

            val datumzdaj = Calendar.getInstance().time
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val datum = format.format(datumzdaj)

            //Preveri podatke, da niso prazni
            var naprej = true
            if(id_stranke.isNullOrEmpty()){
                NapakaAlert("Izberite stranko", requireContext())
                naprej = false
            }

            if(kolicina.isNullOrEmpty()){
                NapakaAlert("Vpišite količino", requireContext())
                naprej = false
            }

            if(izdelek.isNullOrEmpty()){
                NapakaAlert("Izdeleka ni", requireContext())
                naprej = false
            }

            if(naprej){

                //Ustvari podatke za poslat v JSON formatu
                val podatkiVJson = CoroutineScope(Dispatchers.Default).async {
                    //Ustvari data class prodajaPoslat
                    val podatki = prodajaPoslat(datum, datum, kolicina.toString(), id_stranke, izdelek.toString())

                    //Kliče metodo JsonUstvarjanjeProdaja, toliko, da vrne ta courutineScope neke podatke
                    JsonUstvarjanjeProdaja(podatki)
                }

                //Se poveže in ustvari Podatke
                CoroutineScope(Dispatchers.IO).launch {
                    val podatkiZaPoslat = podatkiVJson.await()

                    if (PovezavaObstajaStreznik(url.URL + "odziva.php")) {

                        val res =
                            Jsoup.connect(url.URL + "ustvarjanje.php?tabela=Prodaja").timeout(5000)
                                .ignoreHttpErrors(true)
                                .ignoreContentType(true)
                                .header("Content-Type", "application/json;charset=UTF-8")
                                .header("Authorization", "Bearer " + token.token)
                                .header("Accept", "application/json")
                                .requestBody(podatkiZaPoslat)
                                .method(Connection.Method.POST)
                                .execute()

                        //Uspešno doda se to izpiše
                        CoroutineScope(Dispatchers.Main).launch {
                            UspehAlert("Uspešno dodano", requireContext())
                        }

                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            NapakaAlert("Ni povezave s strežnikom", requireContext())
                        }
                    }

                }
            }
        }


        binding.buttonpogled.setOnClickListener {
            val intent = Intent(requireContext(), PogledActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

        binding.buttonpozabe.setOnClickListener {
            val intent = Intent(requireContext(), PozabeActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //Ustvarjen custom adapter za recyclerview
    class RecyclerNovAdapter(private val podatkiZbrika: Array<String>,
                             private val clickListener: (String) -> Unit) : RecyclerView.Adapter<RecyclerNovAdapter.ViewHolder>(){

        //Spodnji class je metadata za item v Recyclerview-ju, inicializira spremenljivko textview in ustvari setOnClickListiner za view
        class ViewHolder(view: View, clickPozicija: (Int) -> Unit) : RecyclerView.ViewHolder(view){
            val textView: TextView = view.findViewById(R.id.textViewItem)

            init {
                view.setOnClickListener {
                    clickPozicija(adapterPosition)
                }
            }
        }

        //Ko se viewholder ustvari, spremeni layout in doda ta item v recyclerview, poleg tega pa še nastavi spremenljivko clickListener na string s podatki
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_items, parent, false)){
                clickListener(podatkiZbrika[it])
            }

            return  view
        }

        //Nastavi text za textview
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = podatkiZbrika[position]
        }

        override fun getItemCount() = podatkiZbrika.size
    }
}