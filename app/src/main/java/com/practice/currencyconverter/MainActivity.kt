package com.practice.currencyconverter

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    private lateinit var spinner1: Spinner
    private lateinit var spinner2: Spinner
    private lateinit var ed1: EditText
    private lateinit var ed2: EditText

    var currencies = arrayOf<String?>("USD", "INR", "JPY", "RUB")
    private var exchangeRates: Map<String, Double> = emptyMap()
    private val apiKey = "61fc68eed36055ef129084f4" // My Registered API key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        spinner1 = findViewById(R.id.spinner1)
        spinner2 = findViewById(R.id.spinner2)
        ed1 = findViewById(R.id.ed1)
        ed2 = findViewById(R.id.ed2)

        spinner1.onItemSelectedListener = this
        spinner2.onItemSelectedListener = this

        val ad: ArrayAdapter<*> = ArrayAdapter<Any?>(
            this,
            android.R.layout.simple_spinner_item,
            currencies
        )
        val ad2: ArrayAdapter<*> = ArrayAdapter<Any?>(
            this,
            android.R.layout.simple_spinner_item,
            currencies
        )

        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ad2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner1.adapter = ad
        spinner2.adapter = ad2

        ed1.doOnTextChanged { _, _, _, _ ->
            if (ed1.isFocused) {
                convertCurrency()
            }
        }

        ed2.doOnTextChanged { _, _, _, _ ->
            if (ed2.isFocused) {
                convertCurrency()
            }
        }

        // Fetch initial exchange rates
        fetchExchangeRates("USD")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        convertCurrency()
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Not needed for this example
    }

    private fun convertCurrency() {
        try {
            val amt = if (ed1.isFocused) {
                if (ed1.text.isEmpty()) 0.0 else ed1.text.toString().toDouble()
            } else {
                if (ed2.text.isEmpty()) 0.0 else ed2.text.toString().toDouble()
            }

            val fromCurrency = if (ed1.isFocused) spinner1.selectedItem.toString() else spinner2.selectedItem.toString()
            val toCurrency = if (ed1.isFocused) spinner2.selectedItem.toString() else spinner1.selectedItem.toString()

            if (exchangeRates.isNotEmpty()) {
                val convertedCurrency = calculateConversion(amt, fromCurrency, toCurrency)
                if (ed1.isFocused) {
                    ed2.setText(convertedCurrency.toString())
                } else {
                    ed1.setText(convertedCurrency.toString())
                }
            }
        } catch (e: NumberFormatException) {
            Log.e("MainActivity", "Invalid number format: ${e.message}")
            Toast.makeText(this, "Invalid input. Please enter a valid number.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in convertCurrency: ${e.message}")
            Toast.makeText(this, "An unexpected error occurred.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateConversion(amount: Double, fromCurrency: String, toCurrency: String): Double {
        try {
            val fromRate = exchangeRates[fromCurrency] ?: throw Exception("Exchange rate not found for $fromCurrency")
            val toRate = exchangeRates[toCurrency] ?: throw Exception("Exchange rate not found for $toCurrency")
            return (amount / fromRate) * toRate
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in calculateConversion: ${e.message}")
            Toast.makeText(this, "Error converting currency.", Toast.LENGTH_SHORT).show()
            return 0.0
        }
    }

    private fun fetchExchangeRates(baseCurrency: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response<ExchangeRateResponse> = RetrofitInstance.api.getExchangeRates(apiKey, baseCurrency)
                if (response.isSuccessful) {
                    val exchangeRateResponse = response.body()
                    exchangeRateResponse?.let {
                        withContext(Dispatchers.Main) {
                            exchangeRates = it.conversion_rates
                            convertCurrency()
                        }
                    } ?: run {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Empty API response.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "API error: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("MainActivity", "Error fetching exchange rates: ${response.errorBody()}")
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("MainActivity", "Network error: ${e.message}")
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "An unexpected error occurred.", Toast.LENGTH_SHORT).show()
                }
                Log.e("MainActivity", "Exception fetching exchange rates: ${e.message}")
            }
        }
    }
}