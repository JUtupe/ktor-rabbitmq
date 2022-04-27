package pl.jutupe.ktor_rabbitmq

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rabbitmq.client.MessageProperties
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

data class TestTask(val key: String, val delay: Long)

class TestConsumer {
    val consumedTasks = mutableListOf<String>()

    fun consume(task: TestTask, consumer: ConsumerScope) {
        consumedTasks.add(task.key)

        Thread.sleep(100 * task.delay)

        consumer.ack()
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

    @Test
    fun `should consume task when published`() {
        // given
        val consumer1 = TestConsumer()
        val consumer2 = TestConsumer()
        val consumer3 = TestConsumer()

        testApplication {
            application {
                testModule(rabbit.host, rabbit.amqpPort)

                rabbitConsumer {
                    consume<TestTask>("workQueue", false, 1) { body ->
                        consumer1.consume(body, this)
                    }

                    consume<TestTask>("workQueue", false, 1) { body ->
                        consumer2.consume(body, this)
                    }

                    consume<TestTask>("workQueue", false, 1) { body ->
                        consumer3.consume(body, this)
                    }
                }

                // when
                withChannel {
                    repeat(times = 6) { index ->
                        basicPublish(
                            "exchange",
                            "routingKey",
                            MessageProperties.PERSISTENT_TEXT_PLAIN,
                            getPayload("Task$index")
                        )
                    }
                }
            }
        }

        Thread.sleep(1000)

        assertEquals(2, consumer1.consumedTasks.size)
        assertEquals(2, consumer2.consumedTasks.size)
        assertEquals(2, consumer3.consumedTasks.size)

        val task1 = consumer1.consumedTasks[0]
        val task2 = consumer2.consumedTasks[0]
        val task3 = consumer3.consumedTasks[0]
        val task1a = consumer1.consumedTasks[1]
        val task2a = consumer2.consumedTasks[1]
        val task3a = consumer3.consumedTasks[1]
        assertNotEquals(task1, task1a)
        assertNotEquals(task2, task2a)
        assertNotEquals(task3, task3a)
        assertNotEquals(task1, task2)
        assertNotEquals(task1, task3)
        assertNotEquals(task2, task3)
        assertNotEquals(task1a, task2a)
        assertNotEquals(task1a, task3a)
        assertNotEquals(task2a, task3a)
    }

    private fun getPayload(key: String, delay: Long = 1L): ByteArray {
        val body = TestTask(key, delay)
        return jacksonObjectMapper().writeValueAsBytes(body)
    }
}
