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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.example.growagarden.R
import com.example.growagarden.adapters.StockItemAdapter
import com.example.growagarden.data.StockType
import com.example.growagarden.viewmodel.GardenViewModel
import kotlinx.coroutines.launch

class StockFragment : Fragment() {

    private val viewModel: GardenViewModel by activityViewModels()
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var emptyView: MaterialTextView
    private lateinit var headerText: MaterialTextView
    private lateinit var stockItemAdapter: StockItemAdapter

    private var stockType: StockType = StockType.GEAR

    companion object {
        private const val ARG_STOCK_TYPE = "stock_type"

        fun newInstance(stockTypeString: String): StockFragment {
            val fragment = StockFragment()
            val args = Bundle().apply {
                putString(ARG_STOCK_TYPE, stockTypeString)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_STOCK_TYPE)?.let { typeString ->
            stockType = when (typeString.lowercase()) {
                "gear" -> StockType.GEAR
                "seeds" -> StockType.SEEDS
                "eggs" -> StockType.EGGS
                "cosmetics" -> StockType.COSMETICS
                "honey" -> StockType.HONEY
                "night" -> StockType.NIGHT
                "blood" -> StockType.BLOOD
                else -> StockType.GEAR
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stock, container, false)
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
        emptyView = view.findViewById(R.id.emptyView)
        headerText = view.findViewById(R.id.headerText)

        headerText.text = "${stockType.emoji} ${stockType.displayName}"

        swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    private fun setupRecyclerView() {
        stockItemAdapter = StockItemAdapter()

        recyclerView.apply {
            adapter = stockItemAdapter
            layoutManager = GridLayoutManager(requireContext(), getSpanCount())
            setHasFixedSize(true)
        }
    }

    private fun getSpanCount(): Int {
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        return when {
            screenWidthDp >= 840 -> 4  // Tablet landscape
            screenWidthDp >= 600 -> 3  // Tablet portrait
            screenWidthDp >= 480 -> 2  // Large phone landscape
            else -> 1                  // Phone portrait
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE

                    val stocks = viewModel.getStocksByType(stockType)

                    if (stocks.isEmpty() && !uiState.isLoading) {
                        emptyView.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        emptyView.text = "❌ No ${stockType.displayName.lowercase()} available"
                    } else {
                        emptyView.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        stockItemAdapter.submitList(stocks)
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