package com.qwert2603.spend_server.repo_impl

import com.qwert2603.spend_server.repo.IntRepo

class IntRepoImpl : IntRepo {
    @Volatile
    private var i = 0

    override fun getI() = i

    override fun addI(add: Int) {
        i += add
    }
}