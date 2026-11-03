@file:OptIn(ExperimentalMaterial3Api::class)

package de.protech.termine.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.protech.termine.look.AppCard
import de.protech.termine.BuildConfig
import de.protech.termine.update.Updater

@Composable
fun LegalScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Über & Rechtliches") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            LegalCard("Meine Termine") {
                Paragraph("Version ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})")
                Paragraph("Ein privates Projekt von ProTech1982. Quellcode offen auf GitHub.")
                LinkButton("Projektseite öffnen") {
                    Updater.openInBrowser(context, "https://github.com/${Updater.repo}")
                }
            }

            LegalCard("Mit künstlicher Intelligenz entwickelt") {
                Paragraph(
                    "Konzept, Anforderungen und alle Entscheidungen stammen von " +
                        "ProTech1982. Den Programmcode hat überwiegend Claude, ein " +
                        "KI-Assistent von Anthropic, nach diesen Vorgaben geschrieben."
                )
                Spacer(Modifier.height(10.dp))
                Paragraph("Was das bedeutet:", bold = true)
                Bullet("Der Code ist nicht von einem Menschen Zeile für Zeile geprüft worden. Er wird automatisch kompiliert, aber nicht systematisch getestet.")
                Bullet("Diese App ist kein geprüftes Produkt und kein Medizinprodukt. Sie erinnert an Termine, nichts weiter, und ersetzt keine ärztliche Beratung.")
                Bullet("Sie kann Fehler enthalten. Verlass dich bei wichtigen Terminen nicht allein auf sie.")
                Spacer(Modifier.height(10.dp))
                Paragraph(
                    "Dieser Hinweis steht hier aus Transparenz, nicht weil eine " +
                        "Vorschrift ihn verlangt.",
                    muted = true
                )
            }

            LegalCard("Haftung") {
                Paragraph(
                    "Die Nutzung erfolgt auf eigene Gefahr. Die App wird ohne Gewähr " +
                        "bereitgestellt; eine Haftung für Schäden aus ihrer Nutzung ist, " +
                        "soweit gesetzlich zulässig, ausgeschlossen. Unberührt bleibt die " +
                        "Haftung für Vorsatz und grobe Fahrlässigkeit sowie für Schäden aus " +
                        "der Verletzung von Leben, Körper oder Gesundheit."
                )
            }

            LegalCard("Datenschutz") {
                Paragraph(
                    "Alle Termine bleiben auf diesem Gerät, in einer lokalen Datenbank. " +
                        "Es gibt keine Konten, keine Cloud, keine Werbung und keine " +
                        "Analyse-Werkzeuge."
                )
                Spacer(Modifier.height(10.dp))
                Paragraph(
                    "Daten verlassen das Gerät nur in einem Fall: wenn du auf " +
                        "„Nach Update suchen“ tippst. Dann fragt die App bei " +
                        "api.github.com nach der neuesten Version. GitHub sieht dabei – " +
                        "wie beim Aufruf jeder Webseite – die IP-Adresse des Geräts. " +
                        "Den Download selbst übernimmt der Browser, nicht die App."
                )
                Spacer(Modifier.height(10.dp))
                Paragraph(
                    "Tippst du auf die Adresse eines Termins, wird sie an die Karten-App " +
                        "übergeben, die auf diesem Gerät dafür eingestellt ist. Die App " +
                        "bindet keine Karte ein und übermittelt von sich aus nichts an " +
                        "Kartendienste. Was die geöffnete App mit der Adresse macht, " +
                        "richtet sich nach deren eigener Datenschutzerklärung."
                )
            }

            LegalCard("Lizenzen") {
                Paragraph(
                    "Der eigene Programmcode dieser App steht unter der MIT-Lizenz: " +
                        "Verwenden, ändern und weitergeben ist erlaubt, solange der " +
                        "Copyright-Hinweis erhalten bleibt. Ohne Gewährleistung."
                )
                Spacer(Modifier.height(12.dp))
                Paragraph("Verwendete Bibliotheken, alle unter Apache License 2.0:", bold = true)
                Bullet("AndroidX Core, Activity, Lifecycle, Navigation — The Android Open Source Project")
                Bullet("AndroidX Room — The Android Open Source Project")
                Bullet("Jetpack Compose mit Material 3 — The Android Open Source Project")
                Bullet("Kotlin Standard Library und Coroutines — JetBrains s.r.o. und Mitwirkende")
                Spacer(Modifier.height(6.dp))
                LinkButton("Apache License 2.0 lesen") {
                    Updater.openInBrowser(context, "https://www.apache.org/licenses/LICENSE-2.0")
                }
                LinkButton("Lizenzen im Projekt nachlesen") {
                    Updater.openInBrowser(
                        context,
                        "https://github.com/${Updater.repo}/blob/main/THIRD-PARTY-LICENSES.md"
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/* ---------- Bausteine ---------- */

@Composable
private fun LegalCard(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(14.dp))
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun Paragraph(text: String, bold: Boolean = false, muted: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(top = 6.dp)) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LinkButton(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(top = 4.dp)) {
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}
