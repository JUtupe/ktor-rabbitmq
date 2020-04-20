# ktor-rabbitmq
[![](https://jitpack.io/v/JUtupe/ktor-rabbitmq.svg)](https://jitpack.io/#JUtupe/ktor-rabbitmq)

Ktor RabbitMQ feature

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
            exchangeDeclare("exchange, "direct", true)
            queueDeclare("queue, true, false, false, emptyMap())
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
    
    //consume example
    rabbitConsumer {
      consume<MyObject>("queue") { consumerTag, body ->
         println("Consumed message $body, consumer tag: $consumerTag")
      }
    }
```
