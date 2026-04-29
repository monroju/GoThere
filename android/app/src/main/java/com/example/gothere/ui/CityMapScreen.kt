@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.gothere.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.gothere.model.CityPoiData
import com.example.gothere.model.Poi
import com.example.gothere.model.PoiCategory
import com.example.gothere.repository.PoiRepository
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

@Composable
fun CityMapScreen(
    navController: NavHostController,
    cityId: String,
    countryId: String
) {
    val context = LocalContext.current
    val cityData = remember(cityId) { PoiRepository.loadCity(context, cityId) }

    // Filter state
    var activeCategories by remember {
        mutableStateOf(PoiCategory.all.map { it.id }.toSet())
    }

    // Selected POI for detail card
    var selectedPoi by remember { mutableStateOf<Poi?>(null) }

    // MapView reference
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }

    // Initialize MapLibre (must happen before MapView is inflated)
    remember {
        MapLibre.getInstance(context, "", org.maplibre.android.WellKnownTileServer.MapLibre)
        true
    }

    val cityName = cityData?.let { getCityDisplayName(it.cityId) } ?: cityId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cityName) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (cityData == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No map data available for $cityName")
            }
            return@Scaffold
        }

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).apply {
                        getMapAsync { map ->
                            mapRef = map
                            map.setStyle(STYLE_URL)
                            map.cameraPosition = CameraPosition.Builder()
                                .target(LatLng(cityData.centerLat, cityData.centerLng))
                                .zoom(cityData.defaultZoom.toDouble())
                                .build()

                            addMarkers(map, cityData, activeCategories, ctx)

                            map.setOnMarkerClickListener { marker ->
                                val poi = cityData.pois.find { it.name == marker.title }
                                selectedPoi = poi
                                true
                            }
                        }
                    }
                },
                update = { mapView ->
                    mapView.getMapAsync { map ->
                        map.markers.forEach { map.removeMarker(it) }
                        addMarkers(map, cityData, activeCategories, context)
                    }
                }
            )

            // Category filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PoiCategory.all.forEach { cat ->
                    val count = cityData.pois.count { it.category == cat.id }
                    if (count > 0) {
                        FilterChip(
                            selected = activeCategories.contains(cat.id),
                            onClick = {
                                activeCategories = if (activeCategories.contains(cat.id)) {
                                    activeCategories - cat.id
                                } else {
                                    activeCategories + cat.id
                                }
                            },
                            label = { Text("${cat.label} ($count)", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(cat.color),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // POI detail card
            AnimatedVisibility(
                visible = selectedPoi != null,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                selectedPoi?.let { poi ->
                    PoiDetailCard(
                        poi = poi,
                        onDismiss = { selectedPoi = null },
                        onCall = { phone ->
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        },
                        onWeb = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // POI count
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                val shown = cityData.pois.count { activeCategories.contains(it.category) }
                Text(
                    "$shown places",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PoiDetailCard(
    poi: Poi,
    onDismiss: () -> Unit,
    onCall: (String) -> Unit,
    onWeb: (String) -> Unit
) {
    val category = PoiCategory.fromId(poi.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        poi.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    category?.let {
                        Text(
                            it.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(it.color)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }

            if (poi.address.isNotBlank()) {
                Text(
                    poi.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (poi.notes.isNotBlank()) {
                Text(
                    poi.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (poi.phone.isNotBlank()) {
                    OutlinedButton(onClick = { onCall(poi.phone) }) {
                        Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Call", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (poi.website.isNotBlank()) {
                    OutlinedButton(onClick = { onWeb(poi.website) }) {
                        Icon(Icons.Outlined.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Website", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

private fun addMarkers(
    map: MapLibreMap,
    data: CityPoiData,
    activeCategories: Set<String>,
    context: android.content.Context
) {
    val iconFactory = IconFactory.getInstance(context)

    data.pois
        .filter { activeCategories.contains(it.category) }
        .forEach { poi ->
            val cat = PoiCategory.fromId(poi.category)
            val color = cat?.color?.toInt() ?: 0xFF888888.toInt()
            val bitmap = createColoredMarker(color)
            val icon = iconFactory.fromBitmap(bitmap)

            map.addMarker(
                MarkerOptions()
                    .position(LatLng(poi.lat, poi.lng))
                    .title(poi.name)
                    .snippet(poi.address)
                    .icon(icon)
            )
        }
}

private fun createColoredMarker(color: Int): Bitmap {
    val size = 36
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, paint)

    // White border
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2f, borderPaint)
    return bitmap
}

private fun getCityDisplayName(cityId: String): String = when (cityId) {
    "madrid" -> "Madrid"
    "barcelona" -> "Barcelona"
    "valencia" -> "Valencia"
    "malaga" -> "Malaga"
    "marbella" -> "Marbella"
    "seville" -> "Sevilla"
    "granada" -> "Granada"
    "alicante" -> "Alicante"
    "bilbao" -> "Bilbao"
    "sansebastian" -> "San Sebastian"
    "palma" -> "Palma de Mallorca"
    "tenerife" -> "Tenerife"
    "vitoria" -> "Vitoria-Gasteiz"
    "coruna" -> "A Coruna"
    "santander" -> "Santander"
    "zaragoza" -> "Zaragoza"
    "lisbon" -> "Lisbon"
    "porto" -> "Porto"
    "cascais" -> "Cascais"
    "sintra" -> "Sintra"
    "faro" -> "Faro"
    "lagos" -> "Lagos"
    "albufeira" -> "Albufeira"
    "coimbra" -> "Coimbra"
    "braga" -> "Braga"
    "funchal" -> "Funchal"
    "setubal" -> "Setubal"
    "aveiro" -> "Aveiro"
    "evora" -> "Evora"
    "tavira" -> "Tavira"
    "ericeira" -> "Ericeira"
    "mexico_city" -> "Mexico City"
    "guadalajara" -> "Guadalajara"
    "monterrey" -> "Monterrey"
    "merida" -> "Merida"
    "playa_del_carmen" -> "Playa del Carmen"
    "cancun" -> "Cancun"
    "puerto_vallarta" -> "Puerto Vallarta"
    "oaxaca" -> "Oaxaca"
    "san_miguel" -> "San Miguel de Allende"
    "queretaro" -> "Queretaro"
    "puebla" -> "Puebla"
    "guanajuato" -> "Guanajuato"
    "tulum" -> "Tulum"
    "los_cabos" -> "Los Cabos"
    "tijuana" -> "Tijuana"
    "leon" -> "Leon"
    else -> cityId.replaceFirstChar { it.uppercase() }
}
