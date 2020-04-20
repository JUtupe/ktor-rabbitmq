package pl.jutupe.ktor_rabbitmq

import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.DeliverCallback
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
    crossinline rabbitDeliverCallback: (consumerTag: String, body: T) -> Unit
) {
    withChannel {
        basicConsume(
            queue,
            autoAck,
            DeliverCallback { consumerTag, message ->
                runCatching {
                    val mappedEntity = deserialize<T>(message.body)

                    rabbitDeliverCallback.invoke(consumerTag, mappedEntity)
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
