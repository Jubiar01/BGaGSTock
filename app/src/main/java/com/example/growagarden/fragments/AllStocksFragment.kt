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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.example.growagarden.R
import com.example.growagarden.adapters.StockCategoryAdapter
import com.example.growagarden.data.StockType
import com.example.growagarden.viewmodel.GardenViewModel
import kotlinx.coroutines.launch

class AllStocksFragment : Fragment() {

    private val viewModel: GardenViewModel by activityViewModels()
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var stockCategoryAdapter: StockCategoryAdapter
    private lateinit var totalItemsText: MaterialTextView
    private lateinit var weatherCard: MaterialCardView
    private lateinit var weatherText: MaterialTextView
    private lateinit var bonusText: MaterialTextView
    private lateinit var resetTimesCard: MaterialCardView
    private lateinit var gearResetText: MaterialTextView
    private lateinit var eggResetText: MaterialTextView
    private lateinit var cosmeticResetText: MaterialTextView
    private lateinit var honeyResetText: MaterialTextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_stocks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        recyclerView = view.findViewById(R.id.recyclerViewStocks)
        progressBar = view.findViewById(R.id.progressBar)
        totalItemsText = view.findViewById(R.id.totalItemsText)
        weatherCard = view.findViewById(R.id.weatherCard)
        weatherText = view.findViewById(R.id.weatherText)
        bonusText = view.findViewById(R.id.bonusText)
        resetTimesCard = view.findViewById(R.id.resetTimesCard)
        gearResetText = view.findViewById(R.id.gearResetText)
        eggResetText = view.findViewById(R.id.eggResetText)
        cosmeticResetText = view.findViewById(R.id.cosmeticResetText)
        honeyResetText = view.findViewById(R.id.honeyResetText)

        swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    private fun setupRecyclerView() {
        stockCategoryAdapter = StockCategoryAdapter()
        recyclerView.apply {
            adapter = stockCategoryAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE

                    uiState.stockInfo?.let { stockInfo ->
                        val categories = listOf(
                            Pair(StockType.GEAR, stockInfo.gearStock),
                            Pair(StockType.SEEDS, stockInfo.seedsStock),
                            Pair(StockType.EGGS, stockInfo.eggStock),
                            Pair(StockType.COSMETICS, stockInfo.cosmeticsStock),
                            Pair(StockType.HONEY, stockInfo.honeyStock),
                            Pair(StockType.NIGHT, stockInfo.nightStock),
                            Pair(StockType.BLOOD, stockInfo.bloodStock)
                        )

                        stockCategoryAdapter.submitList(categories)
                        totalItemsText.text = "📊 ${viewModel.getTotalItemCount()} total items"
                    }

                    uiState.weatherData?.let { weather ->
                        weatherCard.visibility = View.VISIBLE
                        weatherText.text = "${weather.icon} ${weather.currentWeather}"
                        bonusText.text = "🪴 Bonus: ${weather.cropBonuses}"
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