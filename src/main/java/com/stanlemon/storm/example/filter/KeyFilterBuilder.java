package com.stanlemon.storm.example.filter;

import com.salesforce.storm.spout.dynamic.filter.FilterChainStep;
import com.salesforce.storm.spout.sideline.trigger.example.FilterChainStepBuilder;

import java.util.List;
import java.util.Map;

public class KeyFilterBuilder implements FilterChainStepBuilder {

    private final static String FILTERED_KEYS = "filteredKeys";

    @Override
    public FilterChainStep build(Map<String, Object> data) {
        if (data.containsKey(FILTERED_KEYS)) {
            final List<String> filteredKeys = (List<String>) data.get(FILTERED_KEYS);

            return new KeyFilter(filteredKeys);
        }
        return null;
    }
}
