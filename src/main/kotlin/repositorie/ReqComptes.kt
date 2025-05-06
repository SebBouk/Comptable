package org.example.comptable.repositorie

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import org.example.comptable.ktorm.*
import org.example.comptable.routing.Router
import org.example.comptable.routing.Routes
import org.example.comptable.ui.ComptesEntity
import org.example.comptable.ui.DynamicTable
import org.example.comptable.ui.components.AccountData
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.Column
import java.text.NumberFormat
import java.time.LocalDateTime
import java.util.*

fun fetchComptesData(database: Database, userId: Int): Pair<List<String>, List<List<String?>>> {
    val columns = listOf("ID", "Numéro de compte", "Établissement", "Type")
    val rows = database
        .from(Comptes)
        .leftJoin(Etablissements, on = Comptes.IdEtablissement eq Etablissements.IdEtablissement)
        .leftJoin(TypeComptes, on = Comptes.IdType eq TypeComptes.IdType)
        .select(
            Comptes.IdCompte,
            Comptes.NumeroCompte,
            Etablissements.NomEtablissement,
            TypeComptes.NomType
        )
        .where { Comptes.IdUser eq userId }
        .map { row ->
            val compteId = row[Comptes.IdCompte]!!
            // Calculer le solde pour chaque compte
            val solde = calculerSoldeCompte(database, compteId)
            // Formater le solde avec le symbole €
            val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE)
            val soldeFormate = formatter.format(solde)

            listOf(
                row[Comptes.IdCompte].toString(),
                row[Comptes.NumeroCompte],
                row[Etablissements.NomEtablissement],
                soldeFormate,
                row[TypeComptes.NomType]
            )
        }
    return Pair(columns, rows)
}


fun calculerSoldeCompte(database: Database, compteId: Int): Double {
    val operations = fetchOperationsForAccount(database, compteId)
    return operations.fold(0.0) { acc, operation ->
        if (operation.NatureOperation) { // si entrée (true)
            acc + operation.PrixOperation.toDouble()
        } else { // si sortie (false)
            acc - operation.PrixOperation.toDouble()
        }
    }
}

fun getNumeroCompte(database: Database, compteId: Int): String {
    return database
        .from(Comptes)
        .select(Comptes.NumeroCompte)
        .where { Comptes.IdCompte eq compteId }
        .map { it[Comptes.NumeroCompte] }
        .firstOrNull() ?: "Numéro inconnu"
}

fun fetchComptes(database: Database, userId: Int):List<ComptesEntity> {
    val compte = database
        .from(Comptes)
        .select()
        .where { Comptes.IdUser eq userId }
        .map { row ->
            ComptesEntity(
                IdCompte = row[Comptes.IdCompte]!!,
                NumeroCompte = row[Comptes.NumeroCompte]!!,
                IdEtablissement = row[Comptes.IdEtablissement]!!,
                IdType = row[Comptes.IdType]!!,
                IdUser = row[Comptes.IdUser]!!
            )
        }
    return compte
}

fun onNavigateToCompte(router: Router, accountId: Int) {
    println("Setting account ID to: $accountId")
    router.accountId = accountId
    println("Navigating to COMPTE with ID: ${router.accountId}")
    router.navigateTo(Routes.COMPTE)
    println("After navigation, account ID is: ${router.accountId}")
}

fun addCompte(database: Database, numeroCompte: String, idUser: Int, idEtablissement: Int, idType: Int) {
    database.insert(Comptes) {
        set(Comptes.NumeroCompte, numeroCompte)
        set(Comptes.IdUser, idUser)
        set(Comptes.IdEtablissement, idEtablissement)
        set(Comptes.IdType, idType)
    }
    println("Compte ajouté avec succès : $numeroCompte")
}

fun fetchEtablissements(database: Database): List<Pair<Int?, String?>> {
    val etablissements = database.from(Etablissements)
        .select()
        .map { row ->
            Pair(row[Etablissements.IdEtablissement], row[Etablissements.NomEtablissement])
        }
    println("Etablissements récupérés : $etablissements") // Ajoutez ce log
    return etablissements
}

fun fetchTypesComptes(database: Database): List<Pair<Int?, String?>> {
    val typesComptes = database.from(TypeComptes)
        .select()
        .map { row ->
            Pair(row[TypeComptes.IdType], row[TypeComptes.NomType])
        }
    println("Types de comptes récupérés : $typesComptes") // Ajoutez ce log
    return typesComptes
}

fun addEtablissement(database: Database, name: String): Int {
    // Insérer l'établissement dans la base de données
    val id = database.insertAndGenerateKey(Etablissements) {
        set(it.NomEtablissement, name)
    }
    println("Établissement ajouté avec succès : $name")
    // Retourner l'ID généré
    return id.toString().toInt()
}

// Ajouter un nouveau type de compte à la base de données
fun addTypeCompte(database: Database, name: String): Int {
    // Insérer le type de compte dans la base de données
    val id = database.insertAndGenerateKey(TypeComptes) {
        set(it.NomType, name)
    }
    println("Type de compte ajouté avec succès : $name")
    // Retourner l'ID généré
    return id.toString().toInt()
}
fun updateCompte(
    database: Database,
    compte: org.example.comptable.ui.ComptesEntity
) {
    database.update(Comptes) {
        set(it.NumeroCompte, compte.NumeroCompte)
        set(it.IdEtablissement, compte.IdEtablissement)
        set(it.IdType, compte.IdType)
        where {
            it.IdCompte eq compte.IdCompte
        }
    }
    println("Compte mis à jour avec succès : ID ${compte.IdCompte}")
}

fun deleteCompte(database: Database, compteId: Int) {
    val deletedRows = database.delete(Comptes) {
        it.IdCompte eq compteId
    }
    if (deletedRows > 0) {
        println("Opération supprimée avec succès : ID $compteId")
    } else {
        println("Aucune opération trouvée avec l'ID $compteId")
    }
}

fun deleteEtablissement(database: Database, idEtablissement: Int): Boolean {
    // Vérifier si l'établissement est utilisé par des comptes
    val comptesUsingEtablissement = database.useConnection { conn ->
        val sql = "SELECT COUNT(*) FROM Comptes WHERE IdEtablissement = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setInt(1, idEtablissement)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getInt(1) else 0
    }

    // Ne pas supprimer si l'établissement est utilisé
    if (comptesUsingEtablissement > 0) {
        return false
    }

    // Supprimer l'établissement
    return database.useConnection { conn ->
        val sql = "DELETE FROM Etablissements WHERE IdEtablissement = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setInt(1, idEtablissement)
        stmt.executeUpdate() > 0
    }
}

fun deleteTypeCompte(database: Database, idType: Int): Boolean {
    // Vérifier si le type est utilisé par des comptes
    val comptesUsingType = database.useConnection { conn ->
        val sql = "SELECT COUNT(*) FROM Comptes WHERE IdType = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setInt(1, idType)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getInt(1) else 0
    }

    // Ne pas supprimer si le type est utilisé
    if (comptesUsingType > 0) {
        return false
    }

    // Supprimer le type
    return database.useConnection { conn ->
        val sql = "DELETE FROM typeComptes WHERE IdType = ?"
        val stmt = conn.prepareStatement(sql)
        stmt.setInt(1, idType)
        stmt.executeUpdate() > 0
    }
}
/**
 * Récupère les opérations pour un compte et une période spécifique
 */
fun fetchOperationsForPeriod(
    database: Database,
    compteId: Int,
    month: Int,
    year: Int
): List<OperationEntity> {
    return database
        .from(Operations)
        .select()
        .where {
            (Operations.IdCompte eq compteId)
        }
        .map { row ->
            OperationEntity(
                IdOperation = row[Operations.IdOperation]!!,
                CommentaireOperation = row[Operations.CommentaireOperation] ?: "",
                PrixOperation = row[Operations.PrixOperation]!!,
                NatureOperation = row[Operations.NatureOperation]!!,
                DateOperation = row[Operations.DateOperation]!!,
                IdCompte = row[Operations.IdCompte]!!,
                IdCategorie = row[Operations.IdCategorie]!!
            )
        }
        .filter {
            it.DateOperation.monthValue == month + 1 &&
                    it.DateOperation.year == year
        }
}

/**
 * Génère les données pour les graphiques des comptes pour un mois spécifique
 */
fun fetchAccountDataForMonth(database: Database, userId: Int, month: Int, year: Int): List<AccountData> {
    val comptes = fetchComptes(database, userId)

    return comptes.map { compte ->
        // Récupérer toutes les opérations pour ce compte et cette période
        val operations = fetchOperationsForPeriod(database, compte.IdCompte, month, year)

        // Calculer les entrées totales
        val totalEntrees = operations
            .filter { it.NatureOperation } // true = entrée
            .sumOf { it.PrixOperation.toDouble() }

        // Calculer les sorties totales
        val totalSorties = operations
            .filter { !it.NatureOperation } // false = sortie
            .sumOf { it.PrixOperation.toDouble() }

        // Calculer le solde (entrées - sorties)
        val solde = totalEntrees - totalSorties

        // Déterminer si c'est majoritairement des entrées ou des sorties
        val isIncome = solde >= 0

        // Choisir la couleur en fonction du type d'opération majoritaire
        val color = if (isIncome) Color(0xFF1dbc7c) else Color(0xFFb61431)

        // Créer l'objet AccountData
        AccountData(
            id = compte.IdCompte,
            name = compte.NumeroCompte,
            balance = solde,
            color = color,
            isIncome = isIncome
        )
    }
}

/**
 * Génère les données pour un graphique de répartition des opérations par catégorie
 */
fun fetchOperationsByCategoryForAccount(
    database: Database,
    compteId: Int,
    month: Int,
    year: Int
): Map<String, Double> {
    // Récupérer toutes les opérations pour ce compte et cette période
    val operations = fetchOperationsForPeriod(database, compteId, month, year)

    // Récupérer les noms des catégories
    val categoriesMap = fetchCategoriesMap(database)

    // Grouper les opérations par catégorie et calculer le montant total
    return operations.groupBy {
        categoriesMap[it.IdCategorie] ?: "Non catégorisé"
    }.mapValues { (_, ops) ->
        ops.sumOf {
            if (it.NatureOperation)
                it.PrixOperation.toDouble()
            else
                -it.PrixOperation.toDouble()
        }
    }
}

/**
 * Récupère un mapping des IDs de catégories vers leurs noms
 */
fun fetchCategoriesMap(database: Database): Map<Int, String> {
    return database
        .from(Categories)
        .select(Categories.IdCategorie, Categories.NomCategorie)
        .map { row ->
            row[Categories.IdCategorie]!! to (row[Categories.NomCategorie] ?: "")
        }
        .toMap()
}

/**
 * Fonction à utiliser depuis HomeScreen pour mettre à jour les données des graphiques
 * quand l'utilisateur change de mois ou d'année
 */
fun refreshAccountsData(database: Database, userId: Int, month: Int, year: Int): List<AccountData> {
    return fetchAccountDataForMonth(database, userId, month, year)
}

fun fetchComptesDataForChart(database: Database, userId: Int): Pair<List<String>, List<List<String?>>> {
    // Définir les colonnes que nous voulons afficher
    val columns = listOf("ID", "Numéro de compte", "Établissement", "Solde", "Type")

    // Récupérer les données de tous les comptes pour cet utilisateur
    val rows = database
        .from(Comptes)
        .leftJoin(Etablissements, on = Comptes.IdEtablissement eq Etablissements.IdEtablissement)
        .leftJoin(TypeComptes, on = Comptes.IdType eq TypeComptes.IdType)
        .select(
            Comptes.IdCompte,
            Comptes.NumeroCompte,
            Etablissements.NomEtablissement,
            TypeComptes.NomType
        )
        .where { Comptes.IdUser eq userId }
        .map { row ->
            // Extraire l'ID du compte
            val compteId = row[Comptes.IdCompte]!!

            // Calculer le solde pour ce comptea
            val solde = calculerSoldeCompte(database, compteId)

            // Formater le solde avec le symbole €
            val formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE)
            val soldeFormate = formatter.format(solde)

            // Créer une ligne avec toutes les informations
            listOf(
                compteId.toString(),               // ID
                row[Comptes.NumeroCompte],         // Numéro de compte
                row[Etablissements.NomEtablissement], // Établissement
                soldeFormate,                      // Solde formaté
                row[TypeComptes.NomType]           // Type de compte
            )
        }

    // Ajouter des logs pour déboguer
    println("fetchComptesDataForChart - Nombre de comptes: ${rows.size}")
    rows.forEachIndexed { index, row ->
        println("Compte $index: $row")
    }

    return Pair(columns, rows)
}