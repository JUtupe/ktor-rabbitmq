# ktor-rabbitmq
[![](https://jitpack.io/v/JUtupe/ktor-rabbitmq.svg)](https://jitpack.io/#JUtupe/ktor-rabbitmq)

## Installing Ktor RabbitMQ feature

`implementation "com.github.JUtupe:ktor-rabbitmq:$ktor_rabbitmq_feature"`

`implementation "com.rabbitmq:amqp-client:$rabbitmq_version"`

```kotlin
install(RabbitMQ) {
    uri = "amqp://guest:guest@localhost:5672"
    connectionName = "Connection name"

    //serialize and deserialize functions are required
    serialize { jacksonObjectMapper().writeValueAsBytes(it) }
    deserialize { bytes, type -> jacksonObjectMapper().readValue(bytes, type.javaObjectType) }

    //example initialization logic
    initialize {
        exchangeDeclare("exchange", "direct", true)
        queueDeclare("queue", true, false, false, emptyMap())
        queueBind(
            "queue",
            "exchange",
            "routingKey"
        )
    }
}

//publish example
routing {
    get("anyEndpoint") {
        call.publish("exchange", "routingKey", null, MyObject("test name"))
    }
}

//consume with autoack example
rabbitConsumer {
    consume<MyObject>("queue") { body ->
        println("Consumed message $body")
    }
}

//consume work queue with manual ack example
rabbitConsumer {
    consume<MyObject>("work_queue", false, 1) { body ->
        println("Consumed task $body")
        
        // We can omit 'this' part
        this.ack(multiple = false)
    }
}
```

## Passing pre-initialized RabbitMQ instance

In case you need to initialize RabbitMQ prior to starting it (e. g. to kickstart your DI injection), you
can pass pre-created RabbitMQ instance to the plugin:

```kotlin
install(RabbitMQ) {
    rabbitMQInstance = RabbitMQInstance(RabbitMQConfiguration.create()
        .apply {
            uri = "amqp://guest:guest@${rabbit.host}:${rabbit.amqpPort}"
            connectionName = "Connection name"

            serialize { jacksonObjectMapper().writeValueAsBytes(it) }
            deserialize { bytes, type -> jacksonObjectMapper().readValue(bytes, type.javaObjectType) }

            initialize {
                exchangeDeclare("exchange", "direct", true)
                queueDeclare("queue", true, false, false, emptyMap())
                queueBind("queue", "exchange", "routingKey")
            }
        })
}
```
