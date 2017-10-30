package net.talpidae.centipede.service;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import net.talpidae.base.event.Shutdown;
import net.talpidae.centipede.bean.Api;
import net.talpidae.centipede.bean.metric.MetricStatView;
import net.talpidae.centipede.database.CentipedeRepository;
import net.talpidae.centipede.event.DependenciesModified;
import net.talpidae.centipede.event.NewMetricStats;
import net.talpidae.centipede.event.ServicesModified;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class EventForwarder
{
    private final EventBus eventBus;

    private final ApiBroadcastQueue apiBroadcastQueue;

    private final CentipedeRepository centipedeRepository;


    @Inject
    public EventForwarder(EventBus eventBus, ApiBroadcastQueue apiBroadcastQueue, CentipedeRepository centipedeRepository)
    {
        this.eventBus = eventBus;
        this.apiBroadcastQueue = apiBroadcastQueue;
        this.centipedeRepository = centipedeRepository;

        eventBus.register(this);
    }


    @Subscribe
    public void onServiceModified(ServicesModified serviceModifiedEvent)
    {
        apiBroadcastQueue.add(Api.builder()
                .services(centipedeRepository.findAll())
                .build());
    }


    @Subscribe
    public void onDependenciesModified(DependenciesModified dependenciesModified)
    {
        apiBroadcastQueue.add(Api.builder()
                .dependencies(centipedeRepository.findAllDependencies())
                .build());
    }


    @Subscribe
    public void onNewMetricStats(NewMetricStats newMetricStats)
    {
        apiBroadcastQueue.add(Api.builder()
                .metricStats(MetricStatView.builder()
                        .metricStats(centipedeRepository.findMostRecentMetricStatsByPathPrefix(null))
                        .build())
                .build());
    }


    @Subscribe
    public void shutdown(Shutdown shutdownEvent)
    {
        // make sure we stop forwarding events
        eventBus.unregister(this);
    }
}
