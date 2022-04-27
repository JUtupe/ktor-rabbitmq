package pl.jutupe.ktor_rabbitmq

import io.ktor.server.application.Application
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.util.AttributeKey

class RabbitMQ {

    companion object Feature : BaseApplicationPlugin<Application, RabbitMQConfiguration, RabbitMQInstance> {

        val RabbitMQKey = AttributeKey<RabbitMQInstance>("RabbitMQ")

        override val key: AttributeKey<RabbitMQInstance>
            get() = RabbitMQKey

        override fun install(
            pipeline: Application,
            configure: RabbitMQConfiguration.() -> Unit
        ): RabbitMQInstance {
            val configuration = RabbitMQConfiguration.create()
            configuration.apply(configure)

            val rabbit = configuration.rabbitMQInstance ?: RabbitMQInstance(configuration)
            rabbit.apply { initialize() }

            pipeline.attributes.put(key, rabbit)

            return rabbit.apply {
                logger?.info("RabbitMQ initialized")
            }
        }
    }
}
