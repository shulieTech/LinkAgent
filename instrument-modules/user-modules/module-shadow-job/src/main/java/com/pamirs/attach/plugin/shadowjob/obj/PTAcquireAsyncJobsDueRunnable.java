package com.pamirs.attach.plugin.shadowjob.obj;

import com.pamirs.pradar.Pradar;
import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.impl.asyncexecutor.AcquiredJobEntities;
import org.activiti.engine.impl.asyncexecutor.AsyncExecutor;
import org.activiti.engine.impl.cmd.AcquireAsyncJobsDueCmd;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author angju
 * @date 2021/10/14 10:59
 */
public class PTAcquireAsyncJobsDueRunnable implements Runnable{
    private static Logger log = LoggerFactory.getLogger(PTAcquireAsyncJobsDueRunnable.class);


    protected final AsyncExecutor asyncExecutor;

    protected volatile boolean isInterrupted = false;
    protected final Object MONITOR = new Object();
    protected final AtomicBoolean isWaiting = new AtomicBoolean(false);

    protected long millisToWait = 0;

    public PTAcquireAsyncJobsDueRunnable(AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public synchronized void run() {
        log.info("starting to pt acquire async jobs due");

        final CommandExecutor commandExecutor = asyncExecutor.getCommandExecutor();

        while (!isInterrupted) {
            Pradar.startServerInvoke("PTAcquireAsyncJobsDueRunnable", "run", null);
            Pradar.setClusterTest(true);
            try {
                AcquiredJobEntities acquiredJobs = commandExecutor.execute(new AcquireAsyncJobsDueCmd(asyncExecutor));

                boolean allJobsSuccessfullyOffered = true;
                for (JobEntity job : acquiredJobs.getJobs()) {
                    boolean jobSuccessFullyOffered = asyncExecutor.executeAsyncJob(job);
                    if (!jobSuccessFullyOffered) {
                        allJobsSuccessfullyOffered = false;
                    }
                }

                // If all jobs are executed, we check if we got back the amount we expected
                // If not, we will wait, as to not query the database needlessly.
                // Otherwise, we set the wait time to 0, as to query again immediately.
                millisToWait = asyncExecutor.getDefaultAsyncJobAcquireWaitTimeInMillis();
                int jobsAcquired = acquiredJobs.size();
                if (jobsAcquired >= asyncExecutor.getMaxAsyncJobsDuePerAcquisition()) {
                    millisToWait = 0;
                }

                // If the queue was full, we wait too (even if we got enough jobs back), as not overload the queue
                if (millisToWait == 0 && !allJobsSuccessfullyOffered) {
                    millisToWait = asyncExecutor.getDefaultQueueSizeFullWaitTimeInMillis();
                }

            } catch (ActivitiOptimisticLockingException optimisticLockingException) {
                if (log.isDebugEnabled()) {
                    log.debug("Optimistic locking exception during async job acquisition. If you have multiple async executors running against the same database, " +
                            "this exception means that this thread tried to acquire a due async job, which already was acquired by another async executor acquisition thread." +
                            "This is expected behavior in a clustered environment. " +
                            "You can ignore this message if you indeed have multiple async executor acquisition threads running against the same database. " +
                            "Exception message: {}", optimisticLockingException.getMessage());
                }
            } catch (Throwable e) {
                log.error("exception during pt async job acquisition: {}", e.getMessage(), e);
                millisToWait = asyncExecutor.getDefaultAsyncJobAcquireWaitTimeInMillis();
            }

            if (millisToWait > 0) {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("pt async job acquisition thread sleeping for {} millis", millisToWait);
                    }
                    synchronized (MONITOR) {
                        if(!isInterrupted) {
                            isWaiting.set(true);
                            MONITOR.wait(millisToWait);
                        }
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("pt async job acquisition thread woke up");
                    }
                } catch (InterruptedException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("pt async job acquisition wait interrupted");
                    }
                } finally {
                    isWaiting.set(false);
                }
            }
            Pradar.endServerInvoke("200");

        }

        log.info("stopped pt async job due acquisition");
    }

    public void stop() {
        synchronized (MONITOR) {
            isInterrupted = true;
            if(isWaiting.compareAndSet(true, false)) {
                MONITOR.notifyAll();
            }
        }
    }

    public long getMillisToWait() {
        return millisToWait;
    }

    public void setMillisToWait(long millisToWait) {
        this.millisToWait = millisToWait;
    }
}
