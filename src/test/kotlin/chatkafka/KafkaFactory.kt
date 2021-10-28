package chatkafka

import io.cloudevents.CloudEvent
import io.cloudevents.kafka.CloudEventDeserializer
import io.cloudevents.kafka.CloudEventSerializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import javax.annotation.PreDestroy


@Component
class TestKafkaContainer : KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:6.2.1")) {

    init {
        start()
    }

    @PreDestroy
    fun destroy() {
        stop()
    }

}

// forwards messages from chat_in -> chat_out
@Component
class Forwarder(bootstrapServers: BootstrapServers) {

    private val consumerProps = mapOf(
        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers.stringList,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to CloudEventDeserializer::class.java,
        ConsumerConfig.GROUP_ID_CONFIG to "test",
    )

    private val producerProps = mapOf(
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers.stringList,
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to CloudEventSerializer::class.java,
    )

    private val receiverOptions = ReceiverOptions.create<String, CloudEvent>(consumerProps)
        .consumerProperty(ConsumerConfig.CLIENT_ID_CONFIG, "test")
        .subscription(listOf("chat_in"))

    private val kafkaReceiver = KafkaReceiver.create(receiverOptions).receive().log()

    private val inToOut = kafkaReceiver.map { SenderRecord.create(ProducerRecord("chat_out", "", it.value()), null) }

    private val senderOptions = SenderOptions.create<String, CloudEvent>(producerProps)

    private val kafkaSender = KafkaSender.create(senderOptions)

    private val disposable = kafkaSender.send(inToOut).log().subscribe()

    @PreDestroy
    fun destroy() {
        disposable.dispose()
    }

}

@Configuration
class TestKafkaReceiverOptions {

    @Bean
    fun bootstrapServers(testKafkaContainer: TestKafkaContainer): BootstrapServers {
        return BootstrapServers(testKafkaContainer.bootstrapServers)
    }

}