package org.example.comptable.routing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


enum class Routes (val route: String){
    FIRSTSCREEN("FIRSTSCREEN"),
    LOGIN("LOGIN"),
    HOME("HOME"),
    COMPTE("COMPTE")
}

class Router {
    var currentRoute by mutableStateOf<Routes>(Routes.LOGIN)
        private set
    var accountId: Int? = null
    var userId: Int? = null

    fun navigateTo(route: Routes, accountId: Int?=null){
        currentRoute = route
        println("Navigated to route: $route with account ID: $accountId and IdUser: $userId")
    }
    fun updateUserId(newUserId: Int?) {
        userId = newUserId
    }
}