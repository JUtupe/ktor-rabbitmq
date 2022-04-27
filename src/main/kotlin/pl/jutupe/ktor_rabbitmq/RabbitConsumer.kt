package pl.jutupe.ktor_rabbitmq

import com.rabbitmq.client.*
import io.ktor.server.application.Application
import io.ktor.server.application.plugin

@RabbitMQMarker
fun Application.rabbitConsumer(configuration: RabbitMQInstance.() -> Unit): RabbitMQInstance =
    plugin(RabbitMQ).apply(configuration)

@RabbitMQMarker
inline fun <reified T> RabbitMQInstance.consume(
    queue: String,
    autoAck: Boolean = true,
    basicQos: Int? = null,
    crossinline rabbitDeliverCallback: ConsumerScope.(body: T) -> Unit,
) {
    withChannel {
        basicQos?.let { this.basicQos(it) }
        basicConsume(
            queue,
            autoAck,
            { consumerTag, message ->
                runCatching {
                    val mappedEntity = deserialize<T>(message.body)

                    val scope = ConsumerScope(
                        channel = this,
                        message = message
                    )

                    rabbitDeliverCallback.invoke(scope, mappedEntity)
                }.getOrElse { throwable ->
                    logger?.error(
                        "DeliverCallback error: (" +
                            "messageId = ${message.properties.messageId}, " +
                            "consumerTag = $consumerTag)",
                        throwable,
                    )
                }
            },
            { consumerTag ->
                logger?.error("Consume cancelled: (consumerTag = $consumerTag)")
            }
        )
    }
}

class ConsumerScope(
    private val channel: Channel,
    private val message: Delivery,
) {

    fun ack(multiple: Boolean = false) {
        channel.basicAck(message.envelope.deliveryTag, multiple)
    }

    fun nack(multiple: Boolean = false, requeue: Boolean = false) {
        channel.basicNack(message.envelope.deliveryTag, multiple, requeue)
    }

    fun reject(requeue: Boolean = false) {
        channel.basicReject(message.envelope.deliveryTag, requeue)
    }
}
