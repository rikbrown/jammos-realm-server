package net.jammos.realmserver.network.step

import net.jammos.realmserver.network.message.client.ClientAuthMessage
import net.jammos.realmserver.network.message.server.ServerAuthMessage
import kotlin.reflect.KClass

abstract class Step<T : ClientAuthMessage, out R: ServerAuthMessage>(val messageType: KClass<T>) {

    fun handle(msg: Any): ResponseAndNextStep<R> {
        return when {
            messageType.isInstance(msg) -> handle0(messageType.java.cast(msg))
            else -> throw RuntimeException("Unexpected message type: ${msg::class.qualifiedName}")
        }
    }

    abstract fun handle0(msg: T): ResponseAndNextStep<R>

    data class ResponseAndNextStep<out R: Any>(
            val response: R,
            val nextStep: Step<*, *>? = null)
}

