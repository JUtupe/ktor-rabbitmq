package pl.jutupe.ktor_rabbitmq

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import org.slf4j.Logger

class RabbitMQ(
    val configuration: RabbitMQConfiguration
) {

    val logger: Logger? = configuration.logger

    private val connectionFactory =
        ConnectionFactory().apply {
            setUri(configuration.uri)
        }
    private val connection: Connection = connectionFactory.newConnection(configuration.connectionName)
    private val channel: Channel = connection.createChannel()

    private fun initialize() {
        configuration.initializeBlock.invoke(channel)
    }

    fun withChannel(block: Channel.() -> Unit) {
        block.invoke(channel)
    }

    inline fun <reified T> deserialize(bytes: ByteArray): T =
        configuration.deserializeBlock.invoke(bytes, T::class) as T

    fun <T> serialize(body: T): ByteArray =
        configuration.serializeBlock.invoke(body!!)

    companion object Feature : ApplicationFeature<Application, RabbitMQConfiguration, RabbitMQ> {

        val RabbitMQKey = AttributeKey<RabbitMQ>("RabbitMQ")

        override val key: AttributeKey<RabbitMQ>
            get() = RabbitMQKey

        override fun install(
            pipeline: Application,
            configure: RabbitMQConfiguration.() -> Unit
        ): RabbitMQ {
            val configuration = RabbitMQConfiguration.create()
            configuration.apply(configure)

            val rabbit = RabbitMQ(configuration).apply {
                initialize()
            }

            pipeline.attributes.put(key, rabbit)

            return rabbit.apply {
                logger?.info("RabbitMQ initialized")
            }
        }
    }
}