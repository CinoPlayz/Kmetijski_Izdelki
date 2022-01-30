package com.nejcroz.kmetijski_izdelki

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.nejcroz.kmetijski_izdelki.databinding.FragmentFirstBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)


        //Inicializira url in token spremenljivki
        var url: Config = Config("")
        var token : Token = Token("")

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

            if (PovezavaObstajaStreznik(url.URL + "branje.php")) {
                println(url.URL)
                println(token.token)

                val res = Jsoup.connect(url.URL + "branje.php?tabela=Nacrtovani_prevzemi&dan=Ponedeljek").timeout(5000)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header("Authorization", "Bearer " + token.token)
                    .header("Accept", "application/json")
                    .method(Connection.Method.POST)
                    .execute()

                val data = Gson().fromJson(res.body(), Data_Nacrtovani_Prevzemi::class.java)

                var ZaizpisVTextView = ""

                for (podatek in data.data){
                    ZaizpisVTextView += podatek.Cas + " " + podatek.id_stranke + " " + podatek.Kolicina + " " + podatek.Izdelek + "\n"
                }

                println(res.statusCode())

                CoroutineScope(Dispatchers.Main).launch {
                    binding.textviewKdoDanes.text = ZaizpisVTextView
                }
            }
        }

        //branje.php?tabela=Nacrtovani_prevzemi&dan=Četrtek

        binding.textviewKdoDanes.text = ""
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }*/
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}