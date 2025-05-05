package org.example.comptable.repositorie


import androidx.compose.runtime.Composable
import org.example.comptable.ktorm.Users
import org.example.comptable.ktorm.Users.IdUser
import org.example.comptable.ui.DynamicTable
import org.ktorm.database.Database
import org.ktorm.dsl.*

fun fetchUserData(database: Database): Pair<List<String>, List<List<String?>>> {
    val columns = listOf("ID", "Login")
    val rows = database
        .from(Users)
        .select()
        .map { row ->
            listOf(
                row[Users.IdUser].toString(),
                row[Users.Login],
            )
        }
    return Pair(columns, rows)
}


@Composable
fun UserTable(database: Database) {
    val (columns, rows) = fetchUserData(database)
    DynamicTable(columns = columns, rows = rows)
}