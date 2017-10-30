package net.talpidae.centipede.bean.metric;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


@Getter
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class MetricStatView
{
    /**
     * (Optional) Path prefix to filter by.
     */
    private final String pathPrefix;

    /**
     * Begin timestamp of first MetricStat record returned
     */
    private final OffsetDateTime begin;

    /**
     * End timestamp of last MetricStat record returned.
     * <p>
     * If this and begin are both null, only the last record is returned.
     */
    private final OffsetDateTime end;

    /**
     * Returned MetricStat records.
     */
    private final List<MetricStat> metricStats;

    
    @JsonCreator
    public MetricStatView(@JsonProperty("pathPrefix") @ColumnName("pathPrefix") String pathPrefix,
                          @JsonProperty("begin") @ColumnName("begin") OffsetDateTime begin,
                          @JsonProperty("end") @ColumnName("end") OffsetDateTime end,
                          @JsonProperty("metricStats") @ColumnName("metricStats") List<MetricStat> metricStats)
    {
        this.pathPrefix = pathPrefix;
        this.begin = begin;
        this.end = end;
        this.metricStats = metricStats;
    }
}