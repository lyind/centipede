package net.talpidae.centipede.bean.metric;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


@Getter
@EqualsAndHashCode
@ToString
@Builder
public class MetricStat
{
    private final String pathPrefix;

    private final OffsetDateTime begin;

    private final OffsetDateTime end;

    private final int total;

    private final double minTime;

    private final double avgTime;

    private final double maxTime;

    private final double status2xx;

    private final double status3xx;

    private final double status4xx;

    private final double status5xx;

    private final double maxHeap;

    private final double maxNonHeap;


    @JsonCreator
    public MetricStat(@JsonProperty("pathPrefix") @ColumnName("pathPrefix") String pathPrefix,
                      @JsonProperty("begin") @ColumnName("begin") OffsetDateTime begin,
                      @JsonProperty("end") @ColumnName("end") OffsetDateTime end,
                      @JsonProperty("total") @ColumnName("total") int total,
                      @JsonProperty("minTime") @ColumnName("minTime") double minTime,
                      @JsonProperty("avgTime") @ColumnName("avgTime") double avgTime,
                      @JsonProperty("maxTime") @ColumnName("maxTime") double maxTime,
                      @JsonProperty("status2xx") @ColumnName("status2xx") double status2xx,
                      @JsonProperty("status3xx") @ColumnName("status3xx") double status3xx,
                      @JsonProperty("status4xx") @ColumnName("status4xx") double status4xx,
                      @JsonProperty("status5xx") @ColumnName("status5xx") double status5xx,
                      @JsonProperty("maxHeap") @ColumnName("maxHeap") double maxHeap,
                      @JsonProperty("maxNonHeap") @ColumnName("maxNonHeap") double maxNonHeap)
    {
        this.pathPrefix = pathPrefix;
        this.begin = begin;
        this.end = end;
        this.total = total;
        this.minTime = minTime;
        this.avgTime = avgTime;
        this.maxTime = maxTime;
        this.status2xx = status2xx;
        this.status3xx = status3xx;
        this.status4xx = status4xx;
        this.status5xx = status5xx;
        this.maxHeap = maxHeap;
        this.maxNonHeap = maxNonHeap;
    }
}