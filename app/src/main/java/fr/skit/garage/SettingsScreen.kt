package fr.skit.garage

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    onBack: () -> Unit
) {
    val url by repository.urlFlow.collectAsState(initial = "")
    val token by repository.tokenFlow.collectAsState(initial = "")
    val openTime by repository.openTimeFlow.collectAsState(initial = 5)
    val closeTime by repository.closeTimeFlow.collectAsState(initial = 5)

    val coroutineScope = rememberCoroutineScope()

    var urlState by remember { mutableStateOf(url) }
    var tokenState by remember { mutableStateOf(token) }
    var tokenVisible by remember { mutableStateOf(false) }
    var openState by remember { mutableStateOf(openTime.toString()) }
    var closeState by remember { mutableStateOf(closeTime.toString()) }

    // Keep UI state in sync with repository updates
    LaunchedEffect(url) { urlState = url }
    LaunchedEffect(token) { tokenState = token }
    LaunchedEffect(openTime) { openState = openTime.toString() }
    LaunchedEffect(closeTime) { closeState = closeTime.toString() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(text = "Paramètres") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Retour")
                }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = urlState,
            onValueChange = { new ->
                urlState = new
                coroutineScope.launch { repository.saveUrl(new) }
            },
            label = { Text("URL") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        OutlinedTextField(
            value = tokenState,
            onValueChange = { new ->
                tokenState = new
                coroutineScope.launch { repository.saveToken(new) }
            },
            label = { Text("Token") },
            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (tokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                IconButton(onClick = { tokenVisible = !tokenVisible }) {
                    Icon(imageVector = image, contentDescription = if (tokenVisible) "Masquer token" else "Afficher token")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        OutlinedTextField(
            value = openState,
            onValueChange = { new ->
                // accept only digits
                openState = new.filter { it.isDigit() }
                val parsed = openState.toIntOrNull()
                if (parsed != null) {
                    coroutineScope.launch { repository.saveOpenTime(parsed) }
                }
            },
            label = { Text("Temps d'ouverture (secondes)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        OutlinedTextField(
            value = closeState,
            onValueChange = { new ->
                closeState = new.filter { it.isDigit() }
                val parsed = closeState.toIntOrNull()
                if (parsed != null) {
                    coroutineScope.launch { repository.saveCloseTime(parsed) }
                }
            },
            label = { Text("Temps de fermeture (secondes)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Optional: a concise summary row
        Text(
            text = "Sauvegardé: URL=${url}, token=${if (token.isNotEmpty()) "●●●" else "(vide)"}, open=${openTime}s, close=${closeTime}s",
            modifier = Modifier.padding(16.dp)
        )
    }
}
