package org.example.comptable.ktorm

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object Etablissements : Table<EtablissementsEntity>("etablissements"){

    val IdEtablissement = int("IdEtablissement").primaryKey().bindTo { it.IdEtablissement }
    val NomEtablissement = varchar("NomEtablissement").bindTo { it.NomEtablissement }
}