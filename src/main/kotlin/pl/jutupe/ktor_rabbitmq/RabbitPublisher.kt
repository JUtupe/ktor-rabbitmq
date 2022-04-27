package pl.jutupe.ktor_rabbitmq

import com.rabbitmq.client.AMQP
import io.ktor.server.application.ApplicationCall

fun <T> ApplicationCall.publish(exchange: String, routingKey: String, props: AMQP.BasicProperties?, body: T) {
    application.attributes[RabbitMQ.RabbitMQKey].publish(exchange, routingKey, props, body)
}

fun <T> RabbitMQInstance.publish(exchange: String, routingKey: String, props: AMQP.BasicProperties?, body: T) {
    withChannel {
        val bytes = serialize(body)

        basicPublish(exchange, routingKey, props, bytes)
    }
}
