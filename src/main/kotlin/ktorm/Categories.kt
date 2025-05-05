package org.example.comptable.ktorm

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object Categories : Table<CategorieEntity>("categories"){

    val IdCategorie = int("IdCategorie").primaryKey().bindTo { it.IdCategorie }
    val NomCategorie = varchar("NomCategorie").bindTo { it.NomCategorie }
}