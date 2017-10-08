package net.talpidae.centipede.event;

import net.talpidae.base.util.performance.Metric;

import java.util.Collection;

import lombok.Value;


@Value
public class NewMetrics
{
    private final Collection<Metric> metrics;
}