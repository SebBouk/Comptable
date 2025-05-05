package org.example.comptable

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import org.example.comptable.ktorm.Users
import org.example.comptable.routing.Router
import org.example.comptable.routing.Routes
import org.example.comptable.ui.CompteScreen
import org.example.comptable.ui.FirstScreen
import org.example.comptable.ui.LoginScreen
import org.example.comptable.ui.HomeScreen
import org.ktorm.database.asIterable
import org.ktorm.database.Database
import org.ktorm.dsl.from
import org.ktorm.dsl.select
import org.ktorm.dsl.*
import java.awt.Color as AWTColor

@Composable
@Preview
fun App(database: Database) {
    val router = remember { Router() }
    var userName by remember { mutableStateOf("Utilisateur") }

    LaunchedEffect(Unit) {
        router.navigateTo(Routes.LOGIN)
    }

    MaterialTheme {
        // Box englobant toute l'application pour permettre de superposer l'image
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize(),
                color = Color(0xFFF5F5DC)) {
                // Contenu principal
                Box(modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when (router.currentRoute) {
                        Routes.FIRSTSCREEN -> FirstScreen { route ->
                            router.navigateTo(route)
                        }
                        Routes.LOGIN -> LoginScreen({ login, password ->
                            println("Tentative de connexion avec $login et un mot de passe ${password}")
                            val query = database.from(Users).select().where {
                                (Users.Login eq login) and (Users.MdpUser eq password)
                            }
                            val userId = query.map { it[Users.IdUser]!! }.firstOrNull()
                            if (userId != null) {
                                router.updateUserId(userId)
                                userName = database.from(Users)
                                    .select(Users.NomUser, Users.PrenomUser)
                                    .where { Users.IdUser eq userId }
                                    .map { row ->
                                        "${row[Users.PrenomUser]} ${row[Users.NomUser]}"
                                    }
                                    .firstOrNull() ?: "Utilisateur"
                                router.navigateTo(route = Routes.HOME)
                            }
                        })

                        Routes.HOME -> {
                            val userId = router.userId
                            if (userId != null) {
                                HomeScreen(database = database, userId = userId, userName = userName, router = router) { route, accountId ->
                                    router.navigateTo(route, accountId)
                                }
                            } else {
                                Text("Utilisateur non connecté")
                            }
                        }
                        Routes.COMPTE -> {
                            val accountId = router.accountId
                            if (accountId != null) {
                                CompteScreen(
                                    database = database, accountId = accountId,
                                )
                            } else {
                                Text("Aucun compte sélectionné")
                            }
                        }
                    }

                    // Boutons de navigation selon les pages
                    if (router.currentRoute != Routes.LOGIN && router.currentRoute != Routes.FIRSTSCREEN) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .align(Alignment.TopStart),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(onClick = {
                                router.updateUserId(null)
                                router.navigateTo(Routes.LOGIN)
                            },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFFb61431), // Couleur de fond du bouton
                                    contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                            )
                            ) {
                                Text("Déconnexion")
                            }

                            if (router.currentRoute != Routes.HOME) {
                                Button(onClick = {
                                    when (router.currentRoute) {
                                        Routes.COMPTE -> router.navigateTo(Routes.HOME)
                                        else -> {}
                                    }
                                },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF1dbc7c), // Couleur de fond du bouton
                                        contentColor = Color(0xfff5f5dc)         // Couleur du texte (et icône)
                                    )) {
                                    Text("Retour")
                                }
                            }
                        }
                    }
                }
            }

            // Logo en bas à droite, présent sur TOUTES les pages
            Image(
                painter = painterResource("Comptable.png"),
                contentDescription = "Logo de l'application Comptable",
                modifier = Modifier
                    .size(100.dp) // Ajustez la taille selon vos besoins
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp)
                    .clickable{
                        router.navigateTo(Routes.FIRSTSCREEN)
                    }
            )
        }
    }
}
fun main() = application {

    val database = Database.connect(
        url = "jdbc:mysql://localhost:3306/comptable",
        user = "root",
        password = null
    )

    database.useConnection { connection ->
        val sql = "SELECT 1"
        connection.prepareStatement(sql).use { statement ->
            statement.executeQuery().asIterable().map {
                println("it worked : " + it.getString(1))
            }
        }
    }
    println("Hello World!")

    val icon = painterResource("Comptable-icon.png")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Comptable",
        state = WindowState(placement = WindowPlacement.Maximized),
        icon = icon
    ) {
        App(database)
    }
}



