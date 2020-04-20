package pl.jutupe

import com.rabbitmq.client.AMQP
import io.ktor.application.ApplicationCall

fun <T> ApplicationCall.publish(exchange: String, routingKey: String, props: AMQP.BasicProperties?, body: T) {
    application.attributes[RabbitMQ.RabbitMQKey].publish(exchange, routingKey, props, body)
}

fun <T> RabbitMQ.publish(exchange: String, routingKey: String, props: AMQP.BasicProperties?, body: T) {
    withChannel {
        val bytes = serialize(body)

        basicPublish(exchange, routingKey, props, bytes)
    }
}