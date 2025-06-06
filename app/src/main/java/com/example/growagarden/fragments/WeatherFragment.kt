package com.example.growagarden.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.example.growagarden.R
import com.example.growagarden.viewmodel.GardenViewModel
import kotlinx.coroutines.launch

class WeatherFragment : Fragment() {

    private val viewModel: GardenViewModel by activityViewModels()
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var weatherCard: MaterialCardView
    private lateinit var weatherIcon: MaterialTextView
    private lateinit var weatherText: MaterialTextView
    private lateinit var bonusText: MaterialTextView
    private lateinit var resetTimesCard: MaterialCardView
    private lateinit var gearResetText: MaterialTextView
    private lateinit var eggResetText: MaterialTextView
    private lateinit var cosmeticResetText: MaterialTextView
    private lateinit var honeyResetText: MaterialTextView
    private lateinit var emptyView: MaterialTextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_weather, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        observeViewModel()
    }

    private fun setupViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        progressBar = view.findViewById(R.id.progressBar)
        weatherCard = view.findViewById(R.id.weatherCard)
        weatherIcon = view.findViewById(R.id.weatherIcon)
        weatherText = view.findViewById(R.id.weatherText)
        bonusText = view.findViewById(R.id.bonusText)
        resetTimesCard = view.findViewById(R.id.resetTimesCard)
        gearResetText = view.findViewById(R.id.gearResetText)
        eggResetText = view.findViewById(R.id.eggResetText)
        cosmeticResetText = view.findViewById(R.id.cosmeticResetText)
        honeyResetText = view.findViewById(R.id.honeyResetText)
        emptyView = view.findViewById(R.id.emptyView)

        swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE

                    uiState.weatherData?.let { weather ->
                        weatherCard.visibility = View.VISIBLE
                        emptyView.visibility = View.GONE

                        weatherIcon.text = weather.icon
                        weatherText.text = weather.currentWeather
                        bonusText.text = "🪴 Crop Bonus: ${weather.cropBonuses}"
                    } ?: run {
                        if (!uiState.isLoading) {
                            weatherCard.visibility = View.GONE
                            emptyView.visibility = View.VISIBLE
                        }
                    }

                    uiState.resetTimes?.let { resetTimes ->
                        resetTimesCard.visibility = View.VISIBLE
                        gearResetText.text = "🛠️🌱 Gear/Seeds: ${resetTimes.gear}"
                        eggResetText.text = "🥚 Eggs: ${resetTimes.egg}"
                        cosmeticResetText.text = "💄 Cosmetics: ${resetTimes.cosmetic}"
                        honeyResetText.text = "🍯 Honey: ${resetTimes.honey}"
                    }

                    uiState.error?.let { error ->
                        Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG)
                            .setAction("Retry") { viewModel.loadData() }
                            .show()
                        viewModel.clearError()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isRefreshing.collect { isRefreshing ->
                    swipeRefresh.isRefreshing = isRefreshing
                }
            }
        }
    }
}