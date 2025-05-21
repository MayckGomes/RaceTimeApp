package mayckgomes.com.racetimeapp.data.repository

import android.content.Context
import androidx.core.content.edit

class driversRepository(context: Context) {

    val db = context.getSharedPreferences("driversFavorites", Context.MODE_PRIVATE)

    fun getFavDriver(): Map<String, *> {

            return db.all

    }

    fun addFavDriver(driverName: String,driverId: String){

        db.edit { putString(driverId, driverName) }

    }

    fun verifyDriver(driverId: String): String? {

        return db.getString(driverId,null)

    }

    fun delDriver(driverId: String){

        db.edit{remove(driverId)}

    }

}