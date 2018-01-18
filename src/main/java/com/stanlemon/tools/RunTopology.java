package com.stanlemon.tools;

import com.stanlemon.storm.example.ExampleTopology;
import org.apache.storm.LocalCluster;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.utils.Utils;

import java.util.Map;

public class RunTopology {

    public static void main(final String[] args) throws Exception {
        final Map config = Utils.findAndReadConfigFile("config/topology.yaml", true);

        final StormTopology stormTopology = ExampleTopology.build(config);

        final LocalCluster cluster = new LocalCluster();
        cluster.submitTopology("example", config, stormTopology);

        while (true) {
            Thread.sleep((long) 60 * 1000);
        }
    }
}
