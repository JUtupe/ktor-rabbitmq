package pl.jutupe.ktor_rabbitmq

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.request.get
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

private fun Application.testModule(host: String, port: Int) {
    install(RabbitMQ) {
        uri = "amqp://guest:guest@$host:$port"
        connectionName = "Connection name"

        serialize { jacksonObjectMapper().writeValueAsBytes(it) }
        deserialize { bytes, type -> jacksonObjectMapper().readValue(bytes, type.javaObjectType) }

        initialize {
            exchangeDeclare("exchange", "direct", true)
            queueDeclare("queue", true, false, false, emptyMap())
            queueBind("queue", "exchange", "routingKey")
        }
    }

    routing {
        get("/test") {
            val payload = IntegrationTest.TestObject(key = "value2")

            call.publish("exchange", "routingKey", null, payload)
        }
    }
}

class PublishTest : IntegrationTest() {

    @Test
    fun `should publish message when feature publish called`() =
        testApplication {
            application {
                testModule(rabbit.host, rabbit.amqpPort)

                // given
                val exchange = "exchange"
                val routingKey = "routingKey"
                val body = TestObject("value")

                // when
                attributes[RabbitMQ.RabbitMQKey].publish(exchange, routingKey, null, body)

                // then
                verifyMessages("queue", routingKey, listOf("{\"key\":\"value\"}"))
            }
        }

    @Test
    fun `should publish message when call publish called`() =
        testApplication {
            application {
                testModule(rabbit.host, rabbit.amqpPort)
            }

            // given
            val queue = "queue"
            val routingKey = "routingKey"

            // when
            client.get("/test")

            // then
            verifyMessages(queue, routingKey, listOf("{\"key\":\"value2\"}"))
        }
}