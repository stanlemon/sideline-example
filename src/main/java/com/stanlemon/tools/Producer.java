package com.stanlemon.tools;

import com.salesforce.storm.spout.dynamic.kafka.KafkaConsumerConfig;
import org.apache.commons.cli.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Producer {

    private static final Logger logger = LoggerFactory.getLogger(Producer.class);

    public static void main(String[] args) {
        final Options options = new Options();

        final Option number = new Option("n", "number", true, "number of messages to generate");
        number.setRequired(true);
        options.addOption(number);

        final Option maxKeys = new Option("m", "max-keys", true, "maximum number of keys, defaults to 3");
        options.addOption(maxKeys);

        final Option sleepTime = new Option("s", "sleep", true, "time to sleep between publishes in ms, defaults to 1000");
        options.addOption(sleepTime);

        final CommandLineParser parser = new DefaultParser();
        final HelpFormatter formatter = new HelpFormatter();
        final CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (final ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("producer", options);

            System.exit(1);
            return;
        }

        final Map config = Utils.findAndReadConfigFile("config/topology.yaml", true);

        final String kafkaBroker = ((List<String>) config.get(KafkaConsumerConfig.KAFKA_BROKERS)).get(0);
        final String kafkaTopic = (String) config.get(KafkaConsumerConfig.KAFKA_TOPIC);

        final Properties producerProperties = new Properties();
        producerProperties.put("bootstrap.servers", kafkaBroker);
        producerProperties.put("acks", "all");
        producerProperties.put("retries", 3);
        producerProperties.put("batch.size", 16384);
        producerProperties.put("linger.ms", 1);
        producerProperties.put("buffer.memory", 33554432);
        producerProperties.put("key.serializer", org.apache.kafka.common.serialization.StringSerializer.class.getName());
        producerProperties.put("value.serializer", org.apache.kafka.common.serialization.StringSerializer.class.getName());

        final KafkaProducer<String, String> producer = new KafkaProducer<>(producerProperties);

        final int sleep = Integer.valueOf(cmd.getOptionValue("s", "1000"));

        final int min = 1;
        final int max = Integer.valueOf(cmd.getOptionValue("m", "3"));

        final int totalIterations = Integer.valueOf(cmd.getOptionValue("n"));
        int i = 0;
        int keyIndex = min;

        while (i < totalIterations) {
            final String key = "key" + keyIndex;
            final String value = "record-" + System.currentTimeMillis();

            producer.send(new ProducerRecord<>(kafkaTopic, key, value));

            logger.info("Publishing to {}, {} = {}", kafkaTopic, key, value);

            i++;

            if (keyIndex == max) {
                keyIndex = min;
            } else {
                keyIndex++;
            }

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException ex) {
                producer.close();
                break;
            }
        }

        logger.info("All done publishing!");

        producer.close();
    }
}
