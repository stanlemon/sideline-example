package com.stanlemon.storm.example;

import com.salesforce.storm.spout.sideline.SidelineSpout;
import com.stanlemon.storm.example.bolt.EchoBolt;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.topology.TopologyBuilder;

import java.util.Map;

public final class ExampleTopology {

    public static StormTopology build(final Map config) {
        final TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout(
            "example",
            new SidelineSpout(config),
            1
        );
        builder.setBolt("echo", new EchoBolt(), 1).shuffleGrouping("example");

        return builder.createTopology();
    }
}
