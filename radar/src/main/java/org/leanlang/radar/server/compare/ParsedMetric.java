package org.leanlang.radar.server.compare;

import java.util.Optional;

public record ParsedMetric(String topic, Optional<String> category) {

    /**
     * Split a metric {@code <topic>//<category>} into its topic and category.
     * If no {@code //} exists, the category is undefined.
     */
    public static ParsedMetric parse(String metric) {
        int i = metric.indexOf("//");
        if (i < 0) return new ParsedMetric(metric, Optional.empty());
        String topic = metric.substring(0, i);
        String category = metric.substring(i + 2);
        return new ParsedMetric(topic, Optional.of(category));
    }

    public String format() {
        return category.map(it -> topic + "//" + it).orElse(topic);
    }

    public ParsedMetric withCategory(String category) {
        return new ParsedMetric(this.topic, Optional.of(category));
    }
}
