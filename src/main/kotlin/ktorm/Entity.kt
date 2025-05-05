package org.example.comptable.ktorm

import org.ktorm.entity.Entity
import java.math.BigDecimal
import java.time.LocalDateTime


interface UsersEntity : Entity<UsersEntity>{
    companion object : Entity.Factory<UsersEntity>()

    var IdUser : Int
    var NomUser : String
    var PrenomUser : String
    var Login : String
    var MdpUser : String
    var MailUser : String
}

interface ComptesEntity : Entity<ComptesEntity>{
    companion object : Entity.Factory<ComptesEntity>()

    var IdCompte : Int
    var NumeroCompte : String
    var IdUser : Int
    var IdEtablissement : Int
    var IdType : Int
}

interface TypeComptesEntity : Entity<TypeComptesEntity>{
    companion object : Entity.Factory<TypeComptesEntity>()

    var IdType : Int
    var NomType : String
}

interface OperationEntity : Entity<OperationEntity>{
    companion object : Entity.Factory<OperationEntity>()

    var IdOperation : Int
    var CommentaireOperation : String
    var PrixOperation : BigDecimal
    var NatureOperation : Boolean
    var DateOperation : LocalDateTime
    var IdCompte : Int
    var IdCategorie : Int
}

interface EtablissementsEntity : Entity<EtablissementsEntity>{
    companion object : Entity.Factory<EtablissementsEntity>()

    var IdEtablissement : Int
    var NomEtablissement : String
}

interface CategorieEntity : Entity<CategorieEntity>{
    companion object : Entity.Factory<CategorieEntity>()

    var IdCategorie : Int
    var NomCategorie : String
}