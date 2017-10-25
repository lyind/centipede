package net.talpidae.centipede.bean.configuration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.talpidae.centipede.bean.service.Service;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


/**
 * Centipede configuration format (init.json file format).
 */
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class Configuration
{
    private static final long DEFAULT_KEEP_METRICS_MINUTES = TimeUnit.DAYS.toMinutes(3);

    private static final long DEFAULT_MAINTENANCE_INTERVAL_MINUTES = 15L;

    @Getter
    private final Map<String, String> environment;

    @Getter
    private final List<Service> initialServices;

    @Getter
    private final Long maintenanceIntervalMinutes;
    
    @Getter
    private final Long keepMetricsMinutes;


    @JsonCreator
    public Configuration(@JsonProperty("environment") @ColumnName("environment") Map<String, String> environment,
                         @JsonProperty("initialServices") @ColumnName("initialServices") List<Service> initialServices,
                         @JsonProperty("maintenanceIntervalMinutes") @ColumnName("maintenanceIntervalMinutes") Long maintenanceIntervalMinutes,
                         @JsonProperty("keepMetricsMinutes") @ColumnName("keepMetricsMinutes") Long keepMetricsMinutes)
    {
        this.environment = environment != null ? environment : Collections.emptyMap();
        this.initialServices = initialServices != null ? initialServices : Collections.emptyList();
        this.maintenanceIntervalMinutes = maintenanceIntervalMinutes != null ? Math.max(1, maintenanceIntervalMinutes) : DEFAULT_MAINTENANCE_INTERVAL_MINUTES;
        this.keepMetricsMinutes = keepMetricsMinutes != null ? Math.max(keepMetricsMinutes, this.maintenanceIntervalMinutes) : DEFAULT_KEEP_METRICS_MINUTES;
    }
}