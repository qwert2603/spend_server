package com.qwert2603.spend_server.env

object E {
    val env: EnvInterface = Debug
}

abstract class EnvInterface {
    abstract val dbName: String
    abstract val port: Int
    abstract val forTesting: Boolean
    open var salt: String? = null
    open var sudoUserId: Long? = null
}

private object Debug : EnvInterface() {
    override val dbName = "spend_debug"
    override val port = 8359
    override val forTesting = true
    override var salt: String? = "salt"
    override var sudoUserId: Long? = 1
}

private object Test : EnvInterface() {
    override val dbName = "spend_test"
    override val port = 8359
    override val forTesting = true
    override var salt: String? = "salt"
    override var sudoUserId: Long? = 1
}

private object Prod : EnvInterface() {
    override val dbName = "spend"
    override val port = 8354
    override val forTesting = false
    override var sudoUserId: Long? = 1
}