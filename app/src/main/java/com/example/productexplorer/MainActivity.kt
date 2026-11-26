package com.example.productexplorer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.productexplorer.ui.theme.ProductExplorerTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProductExplorerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ProductCatalogContainer(
                        onProductClick = {
                            // Plus tard : ouvrir le détail du produit
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// ---------- Modèles ----------

data class ProductUi(
    val id: Int,
    val title: String,
    val brand: String,
    val category: String,
    val description: String,
    val price: Double,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val warrantyInformation: String,
    val shippingInformation: String
)

enum class PriceSort(val label: String) {
    NONE("Tri : aucun"),
    ASCENDING("Prix croissant"),
    DESCENDING("Prix décroissant")
}

data class ProductCatalogUiState(
    val products: List<ProductUi> = emptyList(),
    val categories: List<String> = emptyList(),
    val searchQuery: String = "",
    val showOnlyInStock: Boolean = false,
    val showOnlyFavorites: Boolean = false,
    val selectedCategory: String? = null,
    val priceSort: PriceSort = PriceSort.NONE,
    val favoriteProductIds: List<Int> = emptyList()
)

// ---------- Données de test ----------

fun sampleProduct(): ProductUi {
    return ProductUi(
        id = 1,
        title = "Smartphone",
        brand = "TotoTech",
        category = "Smartphones",
        description = "Un smartphone léger avec un écran lumineux, une bonne autonomie et un design moderne.",
        price = 699.99,
        discountPercentage = 12.5,
        rating = 4.6,
        stock = 34,
        warrantyInformation = "Garantie constructeur : 2 ans",
        shippingInformation = "Livraison estimée : 3 à 5 jours ouvrés"
    )
}

fun sampleProducts(): List<ProductUi> {
    return listOf(
        sampleProduct(),
        ProductUi(
            id = 2,
            title = "Casque Audio Pulse",
            brand = "SoundPeak",
            category = "Audio",
            description = "Un casque confortable pour écouter de la musique et travailler dans de bonnes conditions.",
            price = 129.99,
            discountPercentage = 8.0,
            rating = 4.2,
            stock = 0,
            warrantyInformation = "Garantie constructeur : 1 an",
            shippingInformation = "Produit temporairement indisponible"
        ),
        ProductUi(
            id = 3,
            title = "Montre Connectée FitTime",
            brand = "FitTime",
            category = "Wearables",
            description = "Une montre connectée simple pour suivre l'activité quotidienne",
            price = 89.99,
            discountPercentage = 15.0,
            rating = 4.4,
            stock = 18,
            warrantyInformation = "Garantie Constructeur : 2 ans",
            shippingInformation = "Livraison estimée : 2 à 4 jours ouvrés"
        ),
        ProductUi(
            id = 4,
            title = "Enceinte Mini Boom",
            brand = "BoomSound",
            category = "Audio",
            description = "Une enceinte compacte pour écouter de la musique à la maison ou en déplacement.",
            price = 59.99,
            discountPercentage = 5.0,
            rating = 4.1,
            stock = 52,
            warrantyInformation = "Garantie constructeur : 1 an",
            shippingInformation = "Livraison estimée : 3 jours ouvrés"
        )
    )
}

// ---------- ViewModel : source de vérité de l'état ----------

class ProductCatalogViewModel : ViewModel() {

    private val allProducts: List<ProductUi> = sampleProducts()

    // Catégories dérivées des produits : évite les incohérences (singulier/pluriel)
    private val allCategories: List<String> = allProducts.map { it.category }.distinct()

    private val _uiState = MutableStateFlow(
        applyFilters(ProductCatalogUiState(categories = allCategories))
    )

    val uiState: StateFlow<ProductCatalogUiState> = _uiState.asStateFlow()

    /** Point unique de calcul de la liste affichée. Toute modification passe par ici. */
    private fun applyFilters(state: ProductCatalogUiState): ProductCatalogUiState {
        val filtered = allProducts
            .filter { product ->
                state.searchQuery.isBlank() ||
                        product.title.contains(state.searchQuery, ignoreCase = true) ||
                        product.brand.contains(state.searchQuery, ignoreCase = true) ||
                        product.category.contains(state.searchQuery, ignoreCase = true)
            }
            .filter { product -> !state.showOnlyInStock || product.stock > 0 }
            .filter { product -> !state.showOnlyFavorites || product.id in state.favoriteProductIds }
            .filter { product -> state.selectedCategory == null || product.category == state.selectedCategory }
            .let { list ->
                when (state.priceSort) {
                    PriceSort.NONE -> list
                    PriceSort.ASCENDING -> list.sortedBy { it.price }
                    PriceSort.DESCENDING -> list.sortedByDescending { it.price }
                }
            }

        return state.copy(products = filtered)
    }

    /** Applique une modification, puis recalcule TOUJOURS la liste affichée. */
    private fun updateState(transform: (ProductCatalogUiState) -> ProductCatalogUiState) {
        _uiState.update { current -> applyFilters(transform(current)) }
    }

    fun onSearchQueryChange(newValue: String) {
        updateState { it.copy(searchQuery = newValue) }
    }

    fun onToggleStockFilter() {
        updateState { it.copy(showOnlyInStock = !it.showOnlyInStock) }
    }

    fun onToggleFavoritesFilter() {
        updateState { it.copy(showOnlyFavorites = !it.showOnlyFavorites) }
    }

    fun onCategorySelected(category: String?) {
        updateState { it.copy(selectedCategory = category) }
    }

    fun onPriceSortSelected(sort: PriceSort) {
        updateState { it.copy(priceSort = sort) }
    }

    fun onFavoriteClick(productId: Int) {
        updateState { state ->
            val newFavoriteIds =
                if (state.favoriteProductIds.contains(productId)) {
                    state.favoriteProductIds - productId
                } else {
                    state.favoriteProductIds + productId
                }
            state.copy(favoriteProductIds = newFavoriteIds)
        }
    }
}

// ---------- Composable stateful : connecte le ViewModel à Compose ----------

@Composable
fun ProductCatalogContainer(
    onProductClick: (ProductUi) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductCatalogViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ProductCatalogScreen(
        products = uiState.products,
        categories = uiState.categories,
        searchQuery = uiState.searchQuery,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        showOnlyInStock = uiState.showOnlyInStock,
        onToggleStockFilter = viewModel::onToggleStockFilter,
        showOnlyFavorites = uiState.showOnlyFavorites,
        onToggleFavoritesFilter = viewModel::onToggleFavoritesFilter,
        selectedCategory = uiState.selectedCategory,
        onCategorySelected = viewModel::onCategorySelected,
        priceSort = uiState.priceSort,
        onPriceSortSelected = viewModel::onPriceSortSelected,
        favoriteProductIds = uiState.favoriteProductIds,
        onFavoriteClick = viewModel::onFavoriteClick,
        onProductClick = onProductClick,
        modifier = modifier
    )
}

// ---------- Composable stateless : affiche uniquement ce qu'il reçoit ----------

@Composable
fun ProductCatalogScreen(
    products: List<ProductUi>,
    categories: List<String>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    showOnlyInStock: Boolean,
    onToggleStockFilter: () -> Unit,
    showOnlyFavorites: Boolean,
    onToggleFavoritesFilter: () -> Unit,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    priceSort: PriceSort,
    onPriceSortSelected: (PriceSort) -> Unit,
    favoriteProductIds: List<Int>,
    onFavoriteClick: (Int) -> Unit,
    onProductClick: (ProductUi) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Catalogue produits",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text(text = "Rechercher un produit") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            FilterControls(
                showOnlyInStock = showOnlyInStock,
                onToggleStockFilter = onToggleStockFilter,
                showOnlyFavorites = showOnlyFavorites,
                onToggleFavoritesFilter = onToggleFavoritesFilter,
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected,
                priceSort = priceSort,
                onPriceSortSelected = onPriceSortSelected
            )
        }
        item {
            Text(
                text = "${products.size} produit(s) affiché(s)",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        items(
            items = products,
            key = { product -> product.id }
        ) { product ->
            ProductListItem(
                product = product,
                isFavorite = favoriteProductIds.contains(product.id),
                onFavoriteClick = { onFavoriteClick(product.id) },
                onClick = { onProductClick(product) }
            )
        }
    }
}

// ---------- Bloc de filtres ----------

@Composable
fun FilterControls(
    showOnlyInStock: Boolean,
    onToggleStockFilter: () -> Unit,
    showOnlyFavorites: Boolean,
    onToggleFavoritesFilter: () -> Unit,
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    priceSort: PriceSort,
    onPriceSortSelected: (PriceSort) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = showOnlyInStock,
                onClick = onToggleStockFilter,
                label = { Text("En stock") }
            )
            FilterChip(
                selected = showOnlyFavorites,
                onClick = onToggleFavoritesFilter,
                label = { Text("Favoris uniquement") }
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelected(null) },
                    label = { Text("Toutes") }
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { Text(category) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriceSort.entries.forEach { sort ->
                FilterChip(
                    selected = priceSort == sort,
                    onClick = { onPriceSortSelected(sort) },
                    label = { Text(sort.label) }
                )
            }
        }
    }
}

// ---------- Carte produit (stateless) ----------

@Composable
fun ProductListItem(
    product: ProductUi,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Button(onClick = onFavoriteClick) {
                Text(
                    text = if (isFavorite) {
                        "Retirer des favoris"
                    } else {
                        "Ajouter aux favoris"
                    }
                )
            }
            Text(
                text = product.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = product.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${product.price} € • ★ ${product.rating}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onClick) {
                Text(text = "Voir le produit")
            }
        }
    }
}

// ---------- Écrans annexes (inchangés) ----------

@Composable
fun ProductDetailScreen(
    product: ProductUi,
    onAddToCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ProductImage(
            productTitle = product.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        ProductHeader(
            title = product.title,
            brand = product.brand,
            category = product.category
        )
        ProductPriceCard(
            price = product.price,
            discountPercentage = product.discountPercentage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        ProductRating(
            rating = product.rating,
            modifier = Modifier.padding(top = 16.dp)
        )
        ProductAvailabilityCard(
            stock = product.stock,
            shippingInformation = product.shippingInformation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        ProductDescription(
            description = product.description,
            modifier = Modifier.padding(top = 16.dp)
        )
        ProductWarrantyField(
            warrantyInformation = product.warrantyInformation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
        AddToCartButton(
            onClick = onAddToCartClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )
    }
}

@Composable
fun ProductHeader(
    title: String,
    brand: String,
    category: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = brand,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = category,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ProductPriceCard(
    price: Double,
    discountPercentage: Double,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "$price €",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "$discountPercentage %",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ProductImage(
    productTitle: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = android.R.drawable.ic_menu_gallery),
        contentDescription = "Image du produit $productTitle",
        modifier = modifier
    )
}

@Composable
fun ProductRating(
    rating: Double,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = "Note du produit"
        )
        Text(
            text = "$rating / 5",
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun ProductAvailabilityCard(
    stock: Int,
    shippingInformation: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Disponibilité",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Stock : $stock unités",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = shippingInformation,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ProductDescription(
    description: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Description",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ProductWarrantyField(
    warrantyInformation: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = warrantyInformation,
        onValueChange = {},
        readOnly = true,
        label = {
            Text(text = "Garantie")
        },
        modifier = modifier
    )
}

@Composable
fun AddToCartButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
    ) {
        Text(text = "Ajouter au panier")
    }
}

@Composable
fun ProductHomeScreen(
    featuredProduct: ProductUi,
    onFeaturedProductClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        HomeHeader()
        Spacer(modifier = Modifier.height(24.dp))
        SearchPreviewBar()
        Spacer(modifier = Modifier.height(24.dp))
        FeaturedProductSection(
            product = featuredProduct,
            onClick = onFeaturedProductClick
        )
        Spacer(modifier = Modifier.height(24.dp))
        CategorySection()
        Spacer(modifier = Modifier.height(24.dp))
        DailyOfferBox()
    }
}

@Composable
fun HomeHeader(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Product Explorer",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Découvrez les produits du moment",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SearchPreviewBar(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Text(
            text = "Rechercher un produit...",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun FeaturedProductSection(
    product: ProductUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Produit mis en avant",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = product.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = product.brand,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            ProductQuickInfoRow(
                price = product.price,
                rating = product.rating,
                stock = product.stock
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onClick) {
                Text(text = "Voir le produit")
            }
        }
    }
}

@Composable
fun ProductQuickInfoRow(
    price: Double,
    rating: Double,
    stock: Int,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$price €",
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "★ $rating",
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$stock en stock",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun CategoryChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun CategorySection(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Catégories",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            CategoryChip(label = "Smartphones")
            Spacer(modifier = Modifier.weight(1f))
            CategoryChip(label = "Audio")
            Spacer(modifier = Modifier.weight(1f))
            CategoryChip(label = "Maison")
        }
    }
}

@Composable
fun DailyOfferBox(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Offre du jour",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Jusqu'à -20% sur une séléction de produits",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            shape = MaterialTheme.shapes.small,
            tonalElevation = 6.dp
        ) {
            Text(
                text = "Nouveau",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

// ---------- Previews ----------

@Preview(showBackground = true, name = "Catalogue - Dark Theme")
@Composable
fun ProductCatalogScreenDarkPreview() {
    ProductExplorerTheme(darkTheme = true) {
        ProductCatalogScreen(
            products = sampleProducts(),
            categories = sampleProducts().map { it.category }.distinct(),
            searchQuery = "audio",
            onSearchQueryChange = {},
            showOnlyInStock = true,
            onToggleStockFilter = {},
            showOnlyFavorites = false,
            onToggleFavoritesFilter = {},
            selectedCategory = null,
            onCategorySelected = {},
            priceSort = PriceSort.ASCENDING,
            onPriceSortSelected = {},
            favoriteProductIds = listOf(2),
            onFavoriteClick = {},
            onProductClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Catalogue - Light Theme")
@Composable
fun ProductCatalogScreenLightPreview() {
    ProductExplorerTheme(darkTheme = false) {
        ProductCatalogScreen(
            products = sampleProducts(),
            categories = sampleProducts().map { it.category }.distinct(),
            searchQuery = "",
            onSearchQueryChange = {},
            showOnlyInStock = false,
            onToggleStockFilter = {},
            showOnlyFavorites = false,
            onToggleFavoritesFilter = {},
            selectedCategory = null,
            onCategorySelected = {},
            priceSort = PriceSort.NONE,
            onPriceSortSelected = {},
            favoriteProductIds = listOf(1),
            onFavoriteClick = {},
            onProductClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProductCatalogContainerPreview() {
    ProductExplorerTheme {
        ProductCatalogScreen(
            products = sampleProducts(),
            categories = sampleProducts().map { it.category }.distinct(),
            searchQuery = "",
            onSearchQueryChange = {},
            showOnlyInStock = false,
            onToggleStockFilter = {},
            showOnlyFavorites = false,
            onToggleFavoritesFilter = {},
            selectedCategory = null,
            onCategorySelected = {},
            priceSort = PriceSort.NONE,
            onPriceSortSelected = {},
            favoriteProductIds = emptyList(),
            onFavoriteClick = {},
            onProductClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProductDetailScreenPreview() {
    ProductExplorerTheme {
        ProductDetailScreen(
            product = sampleProduct(),
            onAddToCartClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProductHomeScreenPreview() {
    ProductExplorerTheme {
        ProductHomeScreen(
            featuredProduct = sampleProduct(),
            onFeaturedProductClick = {}
        )
    }
}