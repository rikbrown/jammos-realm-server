package net.jammos.realmserver.handler

import net.jammos.realmserver.auth.AuthCommand
import java.io.DataInput

interface Handler {
    val input: DataInput

    fun handle(): Handler?

    fun requireCommand(requiredCommand: AuthCommand) {
        System.err.println("Waiting for " + requiredCommand)
        val command = AuthCommand.read(input)
        if (command != requiredCommand) {
            throw RuntimeException("Unexpected command: " + command)
        }
    }
}

