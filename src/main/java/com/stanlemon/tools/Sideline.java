package com.stanlemon.tools;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.salesforce.storm.spout.dynamic.Tools;
import com.salesforce.storm.spout.dynamic.persistence.zookeeper.CuratorFactory;
import com.salesforce.storm.spout.dynamic.persistence.zookeeper.CuratorHelper;
import com.salesforce.storm.spout.sideline.trigger.SidelineType;
import com.salesforce.storm.spout.sideline.recipes.trigger.zookeeper.Config;
import com.salesforce.storm.spout.sideline.recipes.trigger.TriggerEvent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.storm.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class Sideline {

    private static final Logger logger = LoggerFactory.getLogger(Sideline.class);

    public static void main(String[] args) throws Exception {
        final CommandLine cmd = getArguments(args);

        final String sidelineId = cmd.getOptionValue("i");
        final SidelineType sidelineType = getSidelineType(cmd.getOptionValue("type"));

        Preconditions.checkArgument(
            !sidelineType.equals(SidelineType.START)
                || (sidelineType.equals(SidelineType.START) && cmd.hasOption("c") && cmd.hasOption("r") && cmd.hasOption("d")),
            "When starting a sideline you must specify createdby, reason and data options"
        );

        Preconditions.checkArgument(
            sidelineType.equals(SidelineType.START) || (!sidelineType.equals(SidelineType.START) && cmd.hasOption("i")),
            "When resuming or stopping a sideline you must specify the sideline id"
        );

        final Map<String, Object> config = Utils.findAndReadConfigFile("config/topology.yaml", true);

        final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

        try (final CuratorFramework curator = CuratorFactory.createNewCuratorInstance(
            Tools.stripKeyPrefix(Config.PREFIX, config),
            Sideline.class.getSimpleName()
        )) {
            final CuratorHelper curatorHelper = new CuratorHelper(curator);

            @SuppressWarnings("unchecked")
            final String zkRoot = ((List<String>) config.get(Config.ZK_ROOTS)).get(0);

            if (sidelineType.equals(SidelineType.START)) {
                final LocalDateTime createdAt = LocalDateTime.now();
                final String dataJson = cmd.getOptionValue("data");

                final TriggerEvent triggerEvent = new TriggerEvent(
                    sidelineType,
                    gson.fromJson(cmd.getOptionValue("data"), Map.class),
                    createdAt,
                    cmd.getOptionValue("createdby"),
                    cmd.getOptionValue("reason"),
                    false,
                    createdAt
                );

                // Use the data map, which should be things unique to define this criteria to generate our id
                final String id = DatatypeConverter.printHexBinary(MessageDigest.getInstance("MD5")
                    .digest(dataJson.getBytes("UTF-8")));

                curatorHelper.writeJson(zkRoot + "/" + id, triggerEvent);

                logger.info("Sideline {} saved!", id);
            } else {
                final TriggerEvent originalTriggerEvent = curatorHelper.readJson(zkRoot + "/" + sidelineId, TriggerEvent.class);

                Preconditions.checkNotNull(originalTriggerEvent, "Could not find the original trigger event!");

                logger.info("Loaded {} {}", zkRoot + "/" + sidelineId, originalTriggerEvent);
                final LocalDateTime updatedAt = LocalDateTime.now();
                final TriggerEvent triggerEvent = new TriggerEvent(
                    sidelineType,
                    originalTriggerEvent.getData(),
                    originalTriggerEvent.getCreatedAt(),
                    originalTriggerEvent.getCreatedBy(),
                    originalTriggerEvent.getDescription(),
                    false,
                    updatedAt
                );

                curatorHelper.writeJson(zkRoot + "/" + sidelineId, triggerEvent);

                logger.info("Sideline {} updated!", sidelineId);
            }
        }
    }

    private static CommandLine getArguments(String[] args) {
        final Options options = new Options();

        final Option type = new Option("t", "type", true, "example type");
        type.setRequired(true);
        options.addOption(type);

        final Option createdBy = new Option("c", "createdby", true, "who the example was created by");
        options.addOption(createdBy);

        final Option reason = new Option("r", "reason", true, "reason for the example");
        options.addOption(reason);

        final Option data = new Option("d", "data", true, "data to pass along to the filter when sidelining");
        options.addOption(data);

        final Option sidelineId = new Option("i", "id", true, "sideline id to update with a new type");
        options.addOption(sidelineId);

        final CommandLineParser parser = new DefaultParser();
        final HelpFormatter formatter = new HelpFormatter();

        try {
            return parser.parse(options, args);
        } catch (final ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("sideline", options);

            System.exit(1);
            return null;
        }
    }

    private static SidelineType getSidelineType(final String sidelineType) {
        switch (sidelineType.toLowerCase()) {
            case "start":
                return SidelineType.START;
            case "resume":
                return SidelineType.RESUME;
            case "resolve":
                return SidelineType.RESOLVE;
            default:
                throw new IllegalArgumentException("Provide \"" + sidelineType + "\" is not a valid example type!");
        }
    }
}
