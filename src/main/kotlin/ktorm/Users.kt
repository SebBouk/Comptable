package org.example.comptable.ktorm

import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object Users : Table<UsersEntity>("Users"){
    val IdUser = int("IdUser").primaryKey().bindTo { it.IdUser }
    val NomUser = varchar("NomUser").bindTo { it.NomUser }
    val PrenomUser = varchar("PrenomUser").bindTo { it.PrenomUser }
    val Login = varchar("Login").bindTo { it.Login }
    val MdpUser = varchar("MdpUser").bindTo { it.MdpUser }
    val MailUser = varchar("MailUser").bindTo { it.MailUser }
}