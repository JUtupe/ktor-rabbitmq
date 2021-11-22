package pl.jutupe.ktor_rabbitmq

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Envelope
import io.ktor.application.Application
import io.ktor.application.feature
import io.ktor.util.pipeline.ContextDsl

@ContextDsl
fun Application.rabbitConsumer(configuration: RabbitMQ.() -> Unit): RabbitMQ =
    feature(RabbitMQ).apply(configuration)

@ContextDsl
inline fun <reified T> RabbitMQ.consume(
    queue: String,
    autoAck: Boolean = true,
    crossinline rabbitDeliverCallback: (consumerTag: String, body: T, channel: Channel, envelope: Envelope) -> Unit,
    basicQos: Int = -1,
) {
    withChannel {
        if (basicQos != -1) {
            this.basicQos(basicQos)
        }
        basicConsume(
            queue,
            autoAck,
            DeliverCallback { consumerTag, message ->
                runCatching {
                    val mappedEntity = deserialize<T>(message.body)

                    rabbitDeliverCallback.invoke(consumerTag, mappedEntity, this, message.envelope)
                }.getOrElse {
                    it.printStackTrace()
                }
            },
            CancelCallback {
                println("Consume cancelled: $it")
            }
        )
    }
}
