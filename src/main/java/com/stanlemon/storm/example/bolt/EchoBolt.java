package com.stanlemon.storm.example.bolt;

import com.codahale.metrics.Counter;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class EchoBolt extends BaseRichBolt {

    private static final Logger logger = LoggerFactory.getLogger(EchoBolt.class);

    private transient OutputCollector collector;

    private transient Counter tupleCounter;

    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        this.tupleCounter = context.registerCounter("echoCount");
    }

    @Override
    public void execute(Tuple input) {
        logger.info("Tuple = {}", input);

        this.tupleCounter.inc();

        collector.ack(input);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {

    }
}
