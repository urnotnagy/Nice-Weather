package hu.bme.aut.ijv1hi.NiceWeatherApp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class WeatherAdapter(private var weatherList: MutableList<WeatherItem>) : RecyclerView.Adapter<WeatherAdapter.WeatherViewHolder>() {

    class WeatherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewValue: TextView = itemView.findViewById(R.id.textViewValue)
        val textViewCondition: TextView = itemView.findViewById(R.id.textViewCondition)
        val imageViewIcon: ImageView = itemView.findViewById(R.id.imageViewIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WeatherViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_weather, parent, false)
        return WeatherViewHolder(view)
    }

    override fun onBindViewHolder(holder: WeatherViewHolder, position: Int) {
        val weatherItem = weatherList[position]
        holder.textViewValue.text = weatherItem.value
        holder.textViewCondition.text = weatherItem.condition

        when (weatherItem.condition) {
            "Pressure" -> holder.imageViewIcon.setImageResource(android.R.drawable.ic_menu_compass)
            "Wind" -> holder.imageViewIcon.setImageResource(android.R.drawable.ic_menu_directions)
            "Sunrise" -> holder.imageViewIcon.setImageResource(android.R.drawable.ic_menu_today)
            "Humidity" -> holder.imageViewIcon.setImageResource(android.R.drawable.ic_menu_more)
            "Visibility" -> holder.imageViewIcon.setImageResource(android.R.drawable.ic_menu_view)
            "Sunset" -> holder.imageViewIcon.setImageResource(android.R.drawable.ic_menu_today)
            else -> holder.imageViewIcon.setImageResource(android.R.drawable.ic_menu_help)
        }
    }

    override fun getItemCount() = weatherList.size

    fun updateData(newWeatherList: List<WeatherItem>) {
        weatherList.clear()
        weatherList.addAll(newWeatherList)
        notifyDataSetChanged()
    }
}