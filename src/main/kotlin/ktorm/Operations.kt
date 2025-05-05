package org.example.comptable.ktorm
import org.ktorm.schema.*

object Operations : Table<OperationEntity>("operations"){
    val IdOperation = int("IdOperation").primaryKey().bindTo { it.IdOperation }
    val CommentaireOperation = varchar("CommentaireOperation").bindTo { it.CommentaireOperation }
    val PrixOperation = decimal("PrixOperation").bindTo { it.PrixOperation }
    val NatureOperation = boolean("NatureOperation").bindTo { it.NatureOperation }
    val DateOperation = datetime("DateOperation").bindTo { it.DateOperation }
    val IdCompte = int("IdCompte").bindTo { it.IdCompte }
    val IdCategorie = int("IdCategorie").bindTo { it.IdCategorie }
}