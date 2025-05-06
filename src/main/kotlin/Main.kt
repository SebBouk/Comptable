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
import org.example.comptable.ui.components.ErrorDialog
import org.example.comptable.ui.components.ErrorHandler
import org.ktorm.database.asIterable
import org.ktorm.database.Database
import org.ktorm.dsl.from
import org.ktorm.dsl.select
import org.ktorm.dsl.*
import org.example.comptable.ui.components.DatabaseConfigDialog
import org.example.comptable.config.DatabaseConfig

@Composable
@Preview
fun App(database: Database) {
    val router = remember { Router() }
    var userName by remember { mutableStateOf("Utilisateur") }
    var loginFailed by remember { mutableStateOf(false) }

    // Snackbar pour les messages
    val scaffoldState = rememberScaffoldState()
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            scaffoldState.snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    LaunchedEffect(Unit) {
        router.navigateTo(Routes.LOGIN)
    }

    ErrorHandler.currentError.value?.let {
        ErrorDialog(onDismiss = { ErrorHandler.clearError() })
    }


    Scaffold(
        scaffoldState = scaffoldState,
        content = { paddingValues ->
            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    Surface(modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFF5F5DC)) {
                        Box(modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            when (router.currentRoute) {
                                Routes.FIRSTSCREEN -> FirstScreen { route ->
                                    router.navigateTo(route)
                                }
                                Routes.LOGIN -> LoginScreen(
                                    onLogin = { login, password ->
                                        println("Tentative de connexion avec $login et un mot de passe $password")
                                        val query = database.from(Users).select().where {
                                            (Users.Login eq login) and (Users.MdpUser eq password)
                                        }
                                        val userId = query.map { it[Users.IdUser]!! }.firstOrNull()
                                        if (userId != null) {
                                            // Connexion réussie
                                            loginFailed = false
                                            router.updateUserId(userId)
                                            userName = database.from(Users)
                                                .select(Users.NomUser, Users.PrenomUser)
                                                .where { Users.IdUser eq userId }
                                                .map { row ->
                                                    "${row[Users.PrenomUser]} ${row[Users.NomUser]}"
                                                }
                                                .firstOrNull() ?: "Utilisateur"
                                            router.navigateTo(route = Routes.HOME)
                                        } else {
                                            // Connexion échouée
                                            loginFailed = true
                                        }
                                    },
                                    loginFailed = loginFailed,
                                    onCreateUser = { nom, prenom, login, password, email ->
                                        // Vérifier si le login existe déjà
                                        val existingUser = database.from(Users).select().where {
                                            Users.Login eq login
                                        }.map { it[Users.IdUser] }.firstOrNull()

                                        if (existingUser != null) {
                                            snackbarMessage = "Ce login est déjà utilisé"
                                        } else {
                                            try {
                                                // Insérer le nouvel utilisateur avec RETURN_GENERATED_KEYS
                                                database.useConnection { conn ->
                                                    // Désactiver l'auto-commit pour cette transaction
                                                    val autoCommit = conn.autoCommit
                                                    conn.autoCommit = false

                                                    try {
                                                        // Créer l'utilisateur
                                                        val insertSql = """
                                                            INSERT INTO users (NomUser, PrenomUser, Login, MdpUser, MailUser) 
                                                            VALUES (?, ?, ?, ?, ?)
                                                        """.trimIndent()

                                                        conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS).use { stmt ->
                                                            stmt.setString(1, nom)
                                                            stmt.setString(2, prenom)
                                                            stmt.setString(3, login)
                                                            stmt.setString(4, password)
                                                            stmt.setString(5, email)

                                                            val affectedRows = stmt.executeUpdate()

                                                            if (affectedRows > 0) {
                                                                // Récupérer l'ID généré
                                                                val generatedKeys = stmt.generatedKeys
                                                                if (generatedKeys.next()) {
                                                                    val newUserId = generatedKeys.getInt(1)

                                                                    // Connecter directement l'utilisateur
                                                                    loginFailed = false
                                                                    router.updateUserId(newUserId)
                                                                    userName = "$prenom $nom"

                                                                    // Valider la transaction
                                                                    conn.commit()

                                                                    // Message de bienvenue
                                                                    snackbarMessage = "Bienvenue $prenom ! Votre compte a été créé avec succès."

                                                                    // Redirection
                                                                    router.navigateTo(route = Routes.HOME)
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        // Annuler la transaction en cas d'erreur
                                                        conn.rollback()
                                                        snackbarMessage = "Erreur lors de la création du compte: ${e.message}"
                                                        println("Erreur: ${e.message}")
                                                        e.printStackTrace()
                                                    } finally {
                                                        // Restaurer l'état de l'auto-commit
                                                        conn.autoCommit = autoCommit
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                snackbarMessage = "Erreur lors de la création du compte: ${e.message}"
                                                println("Erreur: ${e.message}")
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                )

                                // Le reste du code pour les autres routes...
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

                            // Boutons de navigation
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
                                            backgroundColor = Color(0xFFb61431),
                                            contentColor = Color(0xfff5f5dc)
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
                                                backgroundColor = Color(0xFF1dbc7c),
                                                contentColor = Color(0xfff5f5dc)
                                            )) {
                                            Text("Retour")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Logo en bas à droite
                    Image(
                        painter = painterResource("Comptable.png"),
                        contentDescription = "Logo de l'application Comptable",
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 16.dp, end = 16.dp)
                            .clickable{
                                router.navigateTo(Routes.FIRSTSCREEN)
                            }
                    )
                }
            }
        }
    )
}

fun main() = application {

    // Tentative de connexion initiale
    var databaseConfig by remember { mutableStateOf(DatabaseConfig.load()) }
    var database by remember { mutableStateOf<Database?>(null) }
    var showDatabaseConfigDialog by remember { mutableStateOf(false) }
    var connectionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(databaseConfig) {
        try {
            database = Database.connect(
                url = databaseConfig.url,
                user = databaseConfig.user,
                password = databaseConfig.password
            )

            // Test de connexion
            database?.useConnection { connection ->
                val sql = "SELECT 1"
                connection.prepareStatement(sql).use { statement ->
                    statement.executeQuery().asIterable().map {
                        println("Connexion réussie : " + it.getString(1))
                    }
                }
            }
            connectionError = null
        } catch (e: Exception) {
            connectionError = e.message
            database = null
            // Afficher le dialogue de configuration si la connexion échoue
            showDatabaseConfigDialog = true
        }
    }

    val icon = painterResource("Comptable-icon.png")

    if (showDatabaseConfigDialog) {
        DatabaseConfigDialog(
            currentConfig = databaseConfig,
            onConfigSaved = { newConfig, newDb ->
                databaseConfig = newConfig
                database = newDb
                showDatabaseConfigDialog = false
            },
            onDismiss = {
                showDatabaseConfigDialog = false
                // Si aucune connexion valide n'est disponible, quitter l'application
                if (database == null) {
                    exitApplication()
                }
            }
        )
    }

    // Afficher l'application principale uniquement si une connexion à la base de données est établie
    database?.let { db ->
        Window(
            onCloseRequest = ::exitApplication,
            title = "Comptable",
            state = WindowState(placement = WindowPlacement.Maximized),
            icon = icon
        ) {
            App(db)
        }
    } ?: run {
        // Si pas de connexion et pas de dialogue affiché, montrer un écran d'erreur
        if (!showDatabaseConfigDialog) {
            Window(
                onCloseRequest = ::exitApplication,
                title = "Erreur de connexion - Comptable",
                icon = icon
            ) {
                MaterialTheme {
                    Surface(color = Color(0xFFF5F5DC)) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Erreur de connexion à la base de données",
                                style = MaterialTheme.typography.h5,
                                color = Color(0xFFb61431)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                connectionError ?: "Impossible de se connecter à la base de données",
                                style = MaterialTheme.typography.body1
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { showDatabaseConfigDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF1dbc7c),
                                        contentColor = Color(0xfff5f5dc)
                                    )
                                ) {
                                    Text("Configurer la connexion")
                                }
                                Button(
                                    onClick = ::exitApplication,
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFFb61431),
                                        contentColor = Color(0xfff5f5dc)
                                    )
                                ) {
                                    Text("Quitter")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



