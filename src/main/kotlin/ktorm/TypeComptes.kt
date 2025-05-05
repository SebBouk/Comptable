package org.example.comptable.ktorm

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object TypeComptes : Table<TypeComptesEntity>("TypeComptes"){

    val IdType = int("IdType").primaryKey().bindTo { it.IdType }
    val NomType = varchar("NomType").bindTo { it.NomType }
}