package io.jenkins.blueocean.service.embedded.rest;

import hudson.model.Cause;
import hudson.model.CauseAction;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.queue.ScheduleResult;
import hudson.util.RunList;
import io.jenkins.blueocean.commons.ServiceException;
import io.jenkins.blueocean.rest.RunLoader;
import io.jenkins.blueocean.rest.hal.Link;
import io.jenkins.blueocean.rest.model.BluePipeline;
import io.jenkins.blueocean.rest.model.BlueQueueItem;
import io.jenkins.blueocean.rest.model.BlueRun;
import io.jenkins.blueocean.rest.model.BlueRunContainer;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.util.Iterator;

/**
 * @author Vivek Pandey
 */
public class RunContainerImpl extends BlueRunContainer {

    private final Job job;
    private final BluePipeline pipeline;

    public RunContainerImpl(@Nonnull BluePipeline pipeline, @Nonnull Job job) {
        this.job = job;
        this.pipeline = pipeline;
    }

    @Override
    public Link getLink() {
        return pipeline.getLink().rel("runs");
    }

    @Override
    public BlueRun get(String name) {
        return RunLoader.get().getRun(name, job, pipeline);
    }

    @Override
    public Iterator<BlueRun> iterator() {
        return RunLoader.get().getRuns(job, pipeline.getLink()).iterator();
    }

    @Override
    public BluePipeline getPipeline(String name) {
        return pipeline;
    }

    /**
     * Schedules a build. If build already exists in the queue and the pipeline does not
     * support running multiple builds at the same time, return a reference to the existing
     * build.
     *
     * @return Queue item.
     */
    @Override
    public BlueQueueItem create() {
        job.checkPermission(Item.BUILD);
        if (job instanceof Queue.Task) {
            ScheduleResult scheduleResult = Jenkins.getInstance()
                .getQueue()
                .schedule2((Queue.Task)job, 0, new CauseAction(new Cause.UserIdCause()));

            if(scheduleResult.isAccepted()) {
                final Queue.Item item = scheduleResult.getItem();

                BlueQueueItem queueItem = QueueContainerImpl.getQueuedItem(item, job);

                if (queueItem == null) {
                    throw new ServiceException.UnexpectedErrorException("The queue item does not exist in the queue");
                } else {
                    return queueItem;
                }
            } else {
                throw new ServiceException.UnexpectedErrorException("Queue item request was not accepted");
            }
        } else {
            throw new ServiceException.NotImplementedException("This pipeline type does not support being queued.");
        }
    }
}
