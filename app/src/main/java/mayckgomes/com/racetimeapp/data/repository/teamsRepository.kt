package mayckgomes.com.racetimeapp.data.repository

import android.content.Context
import androidx.core.content.edit

class teamsRepository(context: Context) {

    val db = context.getSharedPreferences("teamsFavorites", Context.MODE_PRIVATE)

    fun getFavTeam(): Map<String, *> {

        return db.all

    }

    fun addFavTeam(TeamName: String,TeamId: String){

        db.edit { putString(TeamId, TeamName) }

    }

    fun verifyTeam(TeamId: String): String? {

        return db.getString(TeamId,null)

    }

    fun delTeam(driverId: String){

        db.edit{remove(driverId)}

    }


}