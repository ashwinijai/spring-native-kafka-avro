package com.sample.kafka;

import com.sample.model.ResponseModel;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Properties;

@Service
@Slf4j
public class ProducerService {
    public void sendMessage(ResponseModel responseModel) throws IOException {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        properties.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        Producer<String, byte[]> producer = new KafkaProducer<>(properties);

        //avro schema
        String eventMessageSchema =
                "{" +
                        "   \"type\": \"record\"," +
                        "   \"name\": \"EventMessage\"," +
                        "   \"namespace\": \"avro.oracle.fsgbu.plato.eventhub.events\"," +
                        "   \"fields\": [" +
                        "       {\"name\": \"id\", \"type\": \"string\"}," +
                        "       {\"name\": \"evtCode\", \"type\": \"string\"}," +
                        "       {\"name\": \"logTime\", \"type\": \"string\"}," +
                        "       {\"name\": \"logType\", \"type\": \"string\"}," +
                        "       {\"name\": \"logDescription\", \"type\": \"string\"}," +
                        "       {\"name\": \"serviceData\", \"type\": \"string\"}," +
                        "       {\"name\": \"publishedTime\", \"type\": \"string\"}" +
                        "   ]" +
                        "}";

        Schema.Parser parser = new Schema.Parser();
        Schema schema = parser.parse(eventMessageSchema);


//prepare the avro record
        GenericRecord avroRecord = new GenericData.Record(schema);
        avroRecord.put("id", responseModel.getId());
        avroRecord.put("evtCode", responseModel.getEvtCode());
        avroRecord.put("logType", responseModel.getLogType());
        avroRecord.put("logDescription", responseModel.getLogDescription());
        avroRecord.put("serviceData",responseModel.getServiceData());
        avroRecord.put("logTime", Instant.now().toString());
        avroRecord.put("publishedTime", Instant.now().toString());
        System.out.println(avroRecord);
        SpecificDatumWriter<GenericRecord>
                datumWriter =
                new SpecificDatumWriter<>(schema);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.reset();
        BinaryEncoder binaryEncoder = new EncoderFactory().binaryEncoder(byteArrayOutputStream, null);
        datumWriter.write(avroRecord, binaryEncoder);

        binaryEncoder.flush();
        byte[] bytes = byteArrayOutputStream.toByteArray();

        //prepare the kafka record
        ProducerRecord<String, byte[]> record = new ProducerRecord<>("avro-topic", null, bytes);

        producer.send(record);
        //ensures record is sent before closing the producer
        producer.flush();

        producer.close();
    }
}
