package org.sigdevops.kafka.producer

import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.joda.time.DateTime
import org.sigdevops.kafka.avro.Gender
import org.sigdevops.kafka.avro.QuestionnaireResult
import java.util.*

private val BOOTSTRAP_SERVER = "broker-1:9092"

private val SCHEMA_REGISTRY_URL = "http://schema-registry:8083"

fun main(args: Array<String>) {

    // produce()
    consume()
}

private fun consume()
{
    val props = Properties()
    with(props){
        put("bootstrap.servers", "broker-1:9092")
        put("group.id", "QuestionnaireResultsReader")
        put("auto.commit.enable", "false")
        put("auto.offset.reset", "earliest")
        put("schema.registry.url", "http://schema-registry:8083")
        put("specific.avro.reader", true)
        put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        put("value.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer")
    }

    val consumer = KafkaConsumer<String, QuestionnaireResult>(props)
    consumer.subscribe(listOf("results"))

    try{
        while (true)
        {
            val records = consumer.poll(100)
            if(records.isEmpty){
                break
            }
            records.forEach { r -> println("${r.key()} ${r.value().getGender()} ${r.value().getSatisfaction()} ${r.value().getCreatedAt()}") }
        }

    }finally {
        consumer.close()
    }
}

private fun produce() {
    val props = Properties()
    with(props) {
        put("bootstrap.servers", BOOTSTRAP_SERVER)
        put("acks", "all")
        put("retries", 0);
        put("key.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer")
        put("value.serializer", "io.confluent.kafka.serializers.KafkaAvroSerializer")
        put("schema.registry.url", "http://schema-registry:8083")
    }

    val producer = KafkaProducer<String, QuestionnaireResult>(props)

    val questionnaireRecords = (1..100)
            .map { createQuestionnaireResult() }
            .map { r -> ProducerRecord<String, QuestionnaireResult>("results", r.getId(), r) }
    for (questionnaireResult in questionnaireRecords) {
        producer.send(questionnaireResult).get()
    }
}

private fun createQuestionnaireResult(): QuestionnaireResult {
    var result = QuestionnaireResult()
    with(result) {
        setGender(if (Random().nextBoolean()) Gender.FEMALE else Gender.MALE)
        setCreatedAt(DateTime.now())
        setSatisfaction(Random().nextInt(5) + 1)
        setId(UUID.randomUUID().toString())
    }
    return result
}