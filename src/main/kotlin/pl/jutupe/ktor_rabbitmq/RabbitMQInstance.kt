package pl.jutupe.ktor_rabbitmq

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.slf4j.Logger

class RabbitMQInstance(
    val configuration: RabbitMQConfiguration
) {

    val logger: Logger? = configuration.logger

    private val connectionFactory =
        ConnectionFactory().apply {
            setUri(configuration.uri)
        }
    private val connection: Connection = connectionFactory.newConnection(configuration.connectionName)
    private val channel: Channel = connection.createChannel()

    fun initialize() {
        configuration.initializeBlock.invoke(channel)
    }

    fun withChannel(block: Channel.() -> Unit) {
        block.invoke(channel)
    }

    inline fun <reified T> deserialize(bytes: ByteArray): T =
        configuration.deserializeBlock.invoke(bytes, T::class) as T

    fun <T> serialize(body: T): ByteArray =
        configuration.serializeBlock.invoke(body!!)

}
