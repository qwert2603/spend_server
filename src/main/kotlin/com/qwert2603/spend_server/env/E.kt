package com.qwert2603.spend_server.env

object E {
    val env: EnvInterface = Test
}

interface EnvInterface {
    val dbName: String
    val port: Int
    val forTesting: Boolean
}

private object Test : EnvInterface {
    override val dbName = "test_spend"
    override val port = 8359
    override val forTesting = true
}

private object Prod : EnvInterface {
    override val dbName = "spend"
    override val port = 8354
    override val forTesting = false
}

private object Mother : EnvInterface {
    override val dbName = "spend_mother"
    override val port = 8361
    override val forTesting = false
}