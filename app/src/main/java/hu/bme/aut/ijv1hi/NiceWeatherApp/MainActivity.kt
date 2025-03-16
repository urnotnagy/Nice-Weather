package hu.bme.aut.ijv1hi.NiceWeatherApp

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import hu.bme.aut.ijv1hi.NiceWeatherApp.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, WeatherFragment())
                .commit()
        }

// There is a problem with getting the location, I think we need to make it dynamic or add
        // a referesh button to retry getting the location and then get the weather

    }


}

