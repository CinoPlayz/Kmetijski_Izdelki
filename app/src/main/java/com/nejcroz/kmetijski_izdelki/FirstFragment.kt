package com.nejcroz.kmetijski_izdelki

import android.R
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.nejcroz.kmetijski_izdelki.databinding.FragmentFirstBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
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

                //Da pondtke v ustrezen array
                if(!data.data.isNullOrEmpty()){
                    for (podatek in data.data){
                        ZaizpisVTextView += podatek.Cas + ": " +  podatek.Priimek + " " + podatek.Ime + " (ID:" + podatek.id_stranke + ")" +" - " +  podatek.Kolicina + " " + podatek.Izdelek + "\n"
                        stranke.add(podatek.Priimek + " " + podatek.Ime + " - " + podatek.id_stranke)
                        strankakolicina.add(arrayOf(podatek.id_stranke, podatek.Kolicina, podatek.Izdelek))
                    }
                }
                else{
                    ZaizpisVTextView += "Nobeden"
                }


                //Dejansko spremeni elemente activity v podatke
                CoroutineScope(Dispatchers.Main).launch {
                    binding.textviewKdoDanes.text = ZaizpisVTextView

                    val spinner: Spinner = binding.spinnerStranke
                    spinner.adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, stranke)
                }
            }
            else {
                CoroutineScope(Dispatchers.Main).launch {
                    NapakaAlert("Ni povezave s strežnikom", context)
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

                for (stranka in strankakolicina){
                    if(stranka[0] == id_stranke){
                        kolicina = stranka[1].toInt()
                        izdelek = stranka[2].toString()
                    }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}