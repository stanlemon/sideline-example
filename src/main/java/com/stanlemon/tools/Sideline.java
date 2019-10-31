package com.stanlemon.tools;

import com.google.common.base.Preconditions;
import com.salesforce.storm.spout.dynamic.JSON;
import com.salesforce.storm.spout.sideline.recipes.trigger.KeyFilter;
import com.salesforce.storm.spout.sideline.recipes.trigger.TriggerEventHelper;
import com.salesforce.storm.spout.sideline.trigger.SidelineType;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.storm.utils.Utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Sideline {

    public static void main(String[] args) {
        final CommandLine cmd = getArguments(args);

        final String sidelineId = cmd.getOptionValue("i");
        final SidelineType sidelineType = SidelineType.fromValue(cmd.getOptionValue("type"));

        Preconditions.checkArgument(
            !sidelineType.equals(SidelineType.START)
                || (sidelineType.equals(SidelineType.START) && cmd.hasOption("c") && cmd.hasOption("r") && cmd.hasOption("k")),
            "When starting a sideline you must specify createdby, reason and data options"
        );

        Preconditions.checkArgument(
            sidelineType.equals(SidelineType.START) || (!sidelineType.equals(SidelineType.START) && cmd.hasOption("i")),
            "When resuming or stopping a sideline you must specify the sideline id"
        );

        final Map<String, Object> config = Utils.findAndReadConfigFile("config/topology.yaml", true);

        // Turn the supplied data for the filter chain step into a map
        @SuppressWarnings("unchecked")
        final Map<String, Object> data = new JSON(new HashMap<>()).from(cmd.getOptionValue("data"), Map.class);

        final TriggerEventHelper triggerEventHelper = new TriggerEventHelper(config);

        switch (sidelineType) {
            case START:
                final List<String> keys = Arrays.stream(cmd.getOptionValue("keys").split(","))
                    .collect(Collectors.toList());
                triggerEventHelper.startTriggerEvent(
                    new KeyFilter(keys),
                    cmd.getOptionValue("createdby"),
                    cmd.getOptionValue("reason")
                );
                break;
            case RESUME:
                triggerEventHelper.resumeTriggerEvent(sidelineId);
                break;
            case RESOLVE:
                triggerEventHelper.resolveTriggerEvent(sidelineId);
                break;
        }

        triggerEventHelper.close();
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

        final Option keys = new Option("k", "keys", true, "keys to filter when sidelining, separated by commas (no spaces), eg. key1,key2,key3");
        options.addOption(keys);

        final Option sidelineId = new Option("i", "id", true, "sideline id to update with a new type");
        options.addOption(sidelineId);

        final CommandLineParser parser = new DefaultParser();
        final HelpFormatter formatter = new HelpFormatter();

        try {
            return parser.parse(options, args);
        } catch (final ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp(
                "sideline",
                "This command starts, resumes and stops sidelines for the example topology.",
                options,
                "If you're using maven to run this command you will need to use the -Dexec.args parameters to pass arguments, like this:\n"
                    + "mvn clean compile exec:java@sideline -Dexec.args=\"-t start -c Stan Lemon -r Testing -d {\\\"filteredKeys\\\":[\\\"key2\\\"]}\""
            );

            System.exit(1);
            return null;
        }
    }
}
