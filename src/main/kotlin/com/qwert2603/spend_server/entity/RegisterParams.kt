package com.qwert2603.spend_server.entity

import com.qwert2603.spend_server.utils.SpendServerConst

data class RegisterParams(
        val login: String,
        val password: String
) {
    init {
        require(login.length in 1..SpendServerConst.MAX_LOGIN_LENGTH)
        require(password.length in 1..SpendServerConst.MAX_PASSWORD_LENGTH)
    }
}