package io.ebean.metrics.collectd;

class MetaData {

    private static final String COLLECTD_TYPE_GAUGE = "gauge";

    private final String host;
    private final long timestamp;
    private final long period;
    private String plugin;
    private String pluginInstance;
    private String type = COLLECTD_TYPE_GAUGE;
    private String typeInstance;

    MetaData(String host, long epochSecs, long period) {
        this.host = host;
        this.timestamp = epochSecs;
        this.period = period;
    }

    String getHost() {
        return host;
    }

    long getTimestamp() {
        return timestamp;
    }

    long getPeriod() {
        return period;
    }

    String getPlugin() {
        return plugin;
    }

    String getPluginInstance() {
        return pluginInstance;
    }

    String getType() {
        return type;
    }

    String getTypeInstance() {
        return typeInstance;
    }

    MetaData plugin(String name) {
        plugin = Sanitize.name(name);
        return this;
    }

    MetaData pluginInstance(String name) {
        pluginInstance = Sanitize.instanceName(name);
        return this;
    }

    MetaData type(String name) {
        type = Sanitize.name(name);
        return this;
    }

    MetaData typeInstance(String name) {
        typeInstance = Sanitize.instanceName(name);
        return this;
    }

    MetaData pluginRaw(String name) {
        plugin = name;
        return this;
    }

    MetaData typeInstanceRaw(String name) {
        typeInstance = name;
        return this;
    }
}
