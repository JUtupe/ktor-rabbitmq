package pl.jutupe.ktor_rabbitmq

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import com.rabbitmq.client.MessageProperties
import io.ktor.application.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

data class TestTask(val key: String, val delay: Long)

class Consumer {
    val consumedTasks = HashSet<String>()

    fun consume(task: TestTask, channel: Channel, envelope: Envelope) {
        consumedTasks.add(task.key)
        Thread.sleep(100 * task.delay)
        channel.basicAck(envelope.deliveryTag, false)
    }
}

private fun Application.testModule(host: String, port: Int) {
    install(RabbitMQ) {
        uri = "amqp://guest:guest@$host:$port"
        connectionName = "Connection name"

        serialize { jacksonObjectMapper().writeValueAsBytes(it) }
        deserialize { bytes, type -> jacksonObjectMapper().readValue(bytes, type.javaObjectType) }

        initialize {
            exchangeDeclare("exchange", "direct", true)
            queueDeclare("workQueue", true, false, false, emptyMap())
            queueBind("workQueue", "exchange", "routingKey")
        }
    }
}

class WorkQueueTest : IntegrationTest() {

    fun getPayload(key: String, delay: Long): ByteArray {
        val body = TestTask(key, delay)
        return jacksonObjectMapper().writeValueAsBytes(body)
    }

    @Test
    fun `should consume task when published`() {
        val consumer1 = Consumer()
        val consumer2 = Consumer()
        val consumer3 = Consumer()
        val consume1 = { consumerTag: String, body: TestTask, channel: Channel, envelope: Envelope -> consumer1.consume(body, channel, envelope) }
        val consume2 = { consumerTag: String, body: TestTask, channel: Channel, envelope: Envelope -> consumer2.consume(body, channel, envelope) }
        val consume3 = { consumerTag: String, body: TestTask, channel: Channel, envelope: Envelope -> consumer3.consume(body, channel, envelope) }

        withTestApplication({
            testModule(rabbit.host, rabbit.amqpPort)

            rabbitConsumer {
                consume("workQueue", false, consume1, 1)
            }
            rabbitConsumer {
                consume("workQueue", false, consume2, 1)
            }
            rabbitConsumer {
                consume("workQueue", false, consume3, 1)
            }
        }) {
            // when
            withChannel {
                basicPublish("exchange", "routingKey", MessageProperties.PERSISTENT_TEXT_PLAIN, getPayload("Task1", 1))
                basicPublish("exchange", "routingKey", MessageProperties.PERSISTENT_TEXT_PLAIN, getPayload("Task2", 1))
                basicPublish("exchange", "routingKey", MessageProperties.PERSISTENT_TEXT_PLAIN, getPayload("Task3", 1))
                basicPublish("exchange", "routingKey", MessageProperties.PERSISTENT_TEXT_PLAIN, getPayload("Task4", 1))
                basicPublish("exchange", "routingKey", MessageProperties.PERSISTENT_TEXT_PLAIN, getPayload("Task5", 1))
                basicPublish("exchange", "routingKey", MessageProperties.PERSISTENT_TEXT_PLAIN, getPayload("Task6", 1))
            }

            Thread.sleep(1000)

            expectThat(consumer1.consumedTasks.size).isEqualTo(2)
            expectThat(consumer2.consumedTasks.size).isEqualTo(2)
            expectThat(consumer3.consumedTasks.size).isEqualTo(2)
            val task1 = consumer1.consumedTasks.toArray()[0]
            val task2 = consumer2.consumedTasks.toArray()[0]
            val task3 = consumer3.consumedTasks.toArray()[0]
            val task1a = consumer1.consumedTasks.toArray()[1]
            val task2a = consumer2.consumedTasks.toArray()[1]
            val task3a = consumer3.consumedTasks.toArray()[1]
            expectThat(task1).isNotEqualTo(task1a)
            expectThat(task2).isNotEqualTo(task2a)
            expectThat(task3).isNotEqualTo(task3a)
            expectThat(task1).isNotEqualTo(task2)
            expectThat(task1).isNotEqualTo(task3)
            expectThat(task2).isNotEqualTo(task3)
            expectThat(task1a).isNotEqualTo(task2a)
            expectThat(task1a).isNotEqualTo(task3a)
            expectThat(task2a).isNotEqualTo(task3a)
        }
    }
}
