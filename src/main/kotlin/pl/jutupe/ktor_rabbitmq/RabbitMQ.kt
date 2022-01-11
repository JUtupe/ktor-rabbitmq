package pl.jutupe.ktor_rabbitmq

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import io.ktor.utils.io.core.Closeable
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.slf4j.Logger

class RabbitMQ(
    val configuration: RabbitMQConfiguration
) : Closeable {

    val logger: Logger? = configuration.logger

    private val shutdownExecutor: ExecutorService = ThreadPoolExecutor(1, 1, configuration.shutdownTimeout, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
    private val connectionFactory =
        ConnectionFactory().apply {
            setUri(configuration.uri)
            setShutdownExecutor(shutdownExecutor)
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

    override fun close() {
        logger?.info("RabbitMQ stopping")
        shutdownExecutor.awaitTermination(configuration.shutdownTimeout, TimeUnit.MILLISECONDS)
        logger?.info("RabbitMQ stopped")
    }

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
            configuration.logger?.info("RabbitMQ initializing")

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