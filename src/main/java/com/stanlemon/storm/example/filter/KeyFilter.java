package com.stanlemon.storm.example.filter;

import com.salesforce.storm.spout.dynamic.Message;
import com.salesforce.storm.spout.dynamic.filter.FilterChainStep;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class KeyFilter implements FilterChainStep, Serializable {

    private final List<String> filteredKeys;

    public KeyFilter(final List<String> filteredKeys) {
        this.filteredKeys = filteredKeys;
    }

    @Override
    public boolean filter(Message message) {
        final String key = String.valueOf(message.getValues().get(0));
        // If the key for this message is contained in our list we filter it out
        return filteredKeys.contains(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyFilter keyFilter = (KeyFilter) o;
        return Objects.equals(filteredKeys, keyFilter.filteredKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filteredKeys);
    }
}
