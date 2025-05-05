package org.example.comptable.ktorm

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object Comptes : Table<ComptesEntity>("comptes"){
    val IdCompte = int("IdCompte").primaryKey().bindTo { it.IdCompte }
    val NumeroCompte = varchar("NumeroCompte").bindTo { it.NumeroCompte }
    val IdUser = int("IdUser").bindTo { it.IdUser }
    val IdEtablissement = int("IdEtablissement").bindTo { it.IdEtablissement }
    val IdType = int("IdType").bindTo { it.IdType }
}