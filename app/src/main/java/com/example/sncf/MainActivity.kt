package com.example.sncf
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ListView
import okhttp3.*
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException

class MainActivity : AppCompatActivity() {
    /*
    * initListStation() : initialise la liste des stations
    * Récupère les données du fichier stations.json
    * et les ajoute à la liste des stations, et retourne cette liste
     */
    private fun initListStations(): ArrayList<Station> {
        val inputStream = resources.openRawResource(R.raw.gares) // Récupère le fichier stations.json
        val stations = ArrayList<Station>() // Crée une liste de stations vide
        inputStream.bufferedReader().useLines { lines -> // Pour chaque ligne du fichier
            lines.forEach {
                if(it == "CODE_UIC;LIBELLE;long;lat") { // Si c'est la première ligne, on passe
                    return@forEach
                }
                val line = it.split(";") // On sépare les données de la ligne
                val station = Station(line[0].toIntOrNull(), line[1], line[2].toDoubleOrNull(), line[3].toDoubleOrNull()) // On crée une station avec les données
                stations.add(station) // On ajoute la station à la liste
            }
        }
        return stations
    }

    /*
    * initListTrain() : initialise la liste des trains
    * Récupère les données renvoyées par l'API SNCF
    * et les ajoute à la liste des trains, puis retourne cette liste
     */
    private fun initListTrain(response: Response) : ArrayList<Train>{
        val trains = JSONTokener(response.body!!.string()).nextValue() as JSONObject
        val jsonArray = trains.getJSONArray("departures") // Récupère le tableau des trains
        val listTrains = ArrayList<Train>() // Création de la liste des trains

        for (i in 0 until jsonArray.length()) { // Boucle sur le JSONArray pour récupérer les trains
            val trainJson = jsonArray.getJSONObject(i)
            val date = trainJson.getJSONObject("stop_date_time").getString("departure_date_time") //récupère la date sous forme de string

            // Récupérer le type de train

            var type:TypeTrain
            type = when(trainJson.getJSONObject("display_informations").getString("commercial_mode")) {
                "TER" -> TypeTrain.TER
                "TER / Intercités" -> TypeTrain.INTERCITES
                "TGV INOUI" -> {
                    val typeTrain:JSONObject = trainJson.getJSONObject("display_informations")
                        .getJSONObject("stop_points").getJSONArray("commercial_modes")[-1] as JSONObject
                    if (typeTrain.getString("nom") == "OUIGO")TypeTrain.OUIGO  else TypeTrain.TGV;}
                else -> TypeTrain.TER
            }

            val train = Train(
                trainJson.getJSONObject("display_informations").getInt("trip_short_name"),
                type,
                date.substring(9, 11), // Parse la date pour récupérer l'heure,
                date.substring(11, 13), // Parse la date pour récupérer les minutes,
            )
            listTrains.add(train) // Ajoute le train à la liste
        }
        return listTrains
    }

    /*
    * onCreat() : fonction appelée à la création de l'activité
    * Dans celle-ci, on initialise la liste des stations, et on ajoute un listener sur le champ de recherche
    * pour récupérer les trains qui passent par la gare sélectionnée
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val apiKey = BuildConfig.SNCF_KEY // clé d'API SNCF

        val station:ArrayList<Station> = initListStations() // récupère la liste des stations
        val trainListView = findViewById<ListView>(R.id.liste_trains) // récupère sur la vue la liste des trains qu'on va remplir par la suite

        val select_ville: AutoCompleteTextView = findViewById(R.id.select_ville) // récupère sur la vue le champ de recherche de la gare
        val villeAdapter:ArrayAdapter<Station> = ArrayAdapter<Station>(this, android.R.layout.simple_list_item_1, station) // crée un adapter pour le champ de recherche de la gare
        select_ville.setAdapter(villeAdapter) // ajoute l'adapter au champ de recherche de la gare

        // ajoute un listener sur le champ de recherche de la gare pour récupérer les trains qui passent par la gare sélectionnée
        // et les afficher dans la liste des trains quand on clique sur une gare
        select_ville.setOnItemClickListener { parent, view, position, id ->
            val ville = parent.getItemAtPosition(position) as Station // récupère la gare sélectionnée

            val client = OkHttpClient() // crée un client HTTP
            val request = Request.Builder() // crée une requête HTTP
                .url("https://api.sncf.com/v1/coverage/sncf/stop_areas/stop_area:SNCF:" + ville.CODE_UIC + "/departures" ) // url de la requête
                .header("Authorization", apiKey)  // ajoute l'API key dans les headers de la requête
                .build() // construit la requête HTTP

            // envoie la requête HTTP et récupère la réponse
            client.newCall(request).enqueue(object : Callback {
                // si la requête échoue, on affiche un message d'erreur
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                }

                // si la requête réussit, on récupère les trains qui passent par la gare sélectionnée
                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) throw IOException("Unexpected code $response") // si la réponse n'est pas 200, on affiche un message d'erreur

                        // récupère les trains qui passent par la gare sélectionnée
                        val listTrains = initListTrain(it)

                        // On demande au thread principal de mettre à jour la liste des trains
                        this@MainActivity.runOnUiThread {
                            // crée un adapter pour la liste des trains
                            val adapterDepartures = ArrayAdapter(
                                this@MainActivity,
                                android.R.layout.simple_list_item_1,
                                listTrains
                            )
                            trainListView.setAdapter(adapterDepartures) // ajoute l'adapter à la liste des trains
                            adapterDepartures.notifyDataSetChanged() // met à jour la liste des trains
                        }
                    }
                }
            })
        }
    }
}