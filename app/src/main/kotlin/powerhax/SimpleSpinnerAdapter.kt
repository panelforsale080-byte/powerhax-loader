package astral.loader

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SimpleSpinnerAdapter(
    context: Context,
    private val values: Array<String>
) : ArrayAdapter<String>(context, R.layout.spinner_item_simple, values) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.spinner_item_simple, parent, false)
        view.findViewById<TextView>(R.id.spinnerText).text = values[position]
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }
}
