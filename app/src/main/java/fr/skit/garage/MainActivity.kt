package fr.skit.garage

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.skit.garage.ui.theme.GarageTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GarageTheme {
                val navController = rememberNavController()
                // create repository with application context
                val settingsRepo = SettingsRepository(applicationContext)

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        // Use MainScreen which handles polling and state
                        MainScreen(
                            settingsRepo = settingsRepo,
                            onSettings = { navController.navigate("settings") }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            repository = settingsRepo,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    settingsRepo: SettingsRepository,
    onSettings: () -> Unit
) {
    // last known values
    var isConnected by remember { mutableStateOf(false) }
    var lastGateStatus by remember { mutableStateOf<GateStatus?>(null) }
    var isOperating by remember { mutableStateOf(false) }
    var operationMessage by remember { mutableStateOf<String?>(null) }

    val url by settingsRepo.urlFlow.collectAsState()
    val token by settingsRepo.tokenFlow.collectAsState()
    val openTime by settingsRepo.openTimeFlow.collectAsState()
    val closeTime by settingsRepo.closeTimeFlow.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Polling loop - restart when url changes
    LaunchedEffect(url) {
        // small delay before first check
        val pollingIntervalMs = 5_000L
        while (true) {
            val normalized = normalizeBaseUrl(url)
            if (normalized.isBlank()) {
                isConnected = false
            } else {
                try {
                    val ok = ApiClient.checkHealth(normalized)
                    isConnected = ok
                    // fetch gate status as well (optional)
                    val status = ApiClient.getGateStatus(normalized)
                    lastGateStatus = status
                } catch (_: Exception) {
                    // network or parsing error -> mark disconnected
                    isConnected = false
                }
            }
            delay(pollingIntervalMs)
        }
    }

    // Handlers for open/close which show message for configured duration then refresh status
    fun handleOpen() {
        coroutineScope.launch {
            val normalized = normalizeBaseUrl(url)
            if (normalized.isBlank() || token.isBlank()) {
                Toast.makeText(context, "URL ou token manquant", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val success = ApiClient.openGate(normalized, token)
                if (!success) {
                    Toast.makeText(context, "Échec de l'ouverture", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                // success: show operation message for openTime seconds
                isOperating = true
                operationMessage = "Ouverture en cours..."
                Toast.makeText(context, "Ouverture en cours", Toast.LENGTH_SHORT).show()
                delay(openTime * 1000L)
                // after delay, refresh status
                val status = try { ApiClient.getGateStatus(normalized) } catch (_: Exception) { null }
                lastGateStatus = status
            } catch (_: Exception) {
                Toast.makeText(context, "Erreur réseau pendant l'ouverture", Toast.LENGTH_SHORT).show()
            } finally {
                isOperating = false
                operationMessage = null
            }
        }
    }

    fun handleClose() {
        coroutineScope.launch {
            val normalized = normalizeBaseUrl(url)
            if (normalized.isBlank() || token.isBlank()) {
                Toast.makeText(context, "URL ou token manquant", Toast.LENGTH_SHORT).show()
                return@launch
            }
            try {
                val success = ApiClient.closeGate(normalized, token)
                if (!success) {
                    Toast.makeText(context, "Échec de la fermeture", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                // success: show operation message for closeTime seconds
                isOperating = true
                operationMessage = "Fermeture en cours..."
                Toast.makeText(context, "Fermeture en cours", Toast.LENGTH_SHORT).show()
                delay(closeTime * 1000L)
                // after delay, refresh status
                val status = try { ApiClient.getGateStatus(normalized) } catch (_: Exception) { null }
                lastGateStatus = status
            } catch (_: Exception) {
                Toast.makeText(context, "Erreur réseau pendant la fermeture", Toast.LENGTH_SHORT).show()
            } finally {
                isOperating = false
                operationMessage = null
            }
        }
    }

    // Pass isConnected, lastGateStatus and handlers to Greeting
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Greeting(
            modifier = Modifier.padding(innerPadding),
            isConnected = isConnected,
            gateStatus = lastGateStatus,
            isOperating = isOperating,
            operationMessage = operationMessage,
            onSettingsClick = onSettings,
            onOpenClick = { handleOpen() },
            onCloseClick = { handleClose() }
        )
    }
}

private fun normalizeBaseUrl(raw: String?): String {
    if (raw == null) return ""
    var t = raw.trim()
    if (t.isEmpty()) return ""
    if (!t.startsWith("http://") && !t.startsWith("https://")) {
        t = "http://$t"
    }
    return t.trimEnd('/')
}

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    isConnected: Boolean = false,
    gateStatus: GateStatus? = null,
    isOperating: Boolean = false,
    operationMessage: String? = null,
    onSettingsClick: () -> Unit = {},
    onOpenClick: () -> Unit = {},
    onCloseClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Top-left small circular indicator (10px converted to dp)
        val indicatorSize = (10f / LocalDensity.current.density).dp
        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(indicatorSize)
                .align(Alignment.TopStart)
                .background(
                    color = if (isConnected) Color.Green else Color.Red,
                    shape = CircleShape
                )
        )

        // Top-right settings button (gear)
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Paramètres"
            )
        }

        // Centered buttons
        Column(
            modifier = Modifier
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Show Open button only if status is not "open"
            if (gateStatus?.status != "open") {
                Button(
                    onClick = onOpenClick,
                    enabled = !isOperating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(text = "Ouvrir")
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Show Close button only if status is not "closed"
            if (gateStatus?.status != "closed") {
                Button(
                    onClick = onCloseClick,
                    enabled = !isOperating,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
                ) {
                    Text(text = "Fermer")
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Operation message
            operationMessage?.let { msg ->
                Text(text = msg)
            }

            // Status summary
            gateStatus?.let { s ->
                val statusText = when (s.status) {
                    "closed" -> "Porte: Fermée"
                    "open" -> "Porte: Ouverte"
                    "opening" -> "Ouverture en cours"
                    "closing" -> "Fermeture en cours"
                    "unknown" -> "Porte: Position indéterminée"
                    else -> "Porte: ${s.status}"
                }
                Text(text = statusText)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GarageTheme {
        // Preview with connected = true to show green dot
        Greeting(
            isConnected = true,
            gateStatus = GateStatus(status = "open", sensorOpen = true),
            isOperating = false,
            operationMessage = null,
            onSettingsClick = {},
            onOpenClick = {},
            onCloseClick = {}
        )
    }
}