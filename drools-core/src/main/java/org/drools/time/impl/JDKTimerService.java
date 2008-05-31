/*
 * Copyright 2008 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.drools.time.impl;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.drools.time.Job;
import org.drools.time.JobContext;
import org.drools.time.JobHandle;
import org.drools.time.TimerService;
import org.drools.time.Trigger;

/**
 * A default Scheduler implementation that uses the
 * JDK built-in ScheduledThreadPoolExecutor as the
 * scheduler and the system clock as the clock.
 * 
 */
public class JDKTimerService
    implements
    TimerService {
    private ScheduledThreadPoolExecutor scheduler;

    public JDKTimerService() {
        this( 3 );
    }

    public JDKTimerService(int size) {
        this.scheduler = new ScheduledThreadPoolExecutor( size );
    }

    /**
     * @inheritDoc
     */
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }

    public JobHandle scheduleJob(Job job,
                                 JobContext ctx,
                                 Trigger trigger) {
        JDKJobHandle jobHandle = new JDKJobHandle();

        Date date = trigger.getNextFireTime();

        if ( date != null ) {
            JDKCallableJob callableJob = new JDKCallableJob( job,
                                                             ctx,
                                                             trigger,
                                                             jobHandle,
                                                             this.scheduler );
            ScheduledFuture future = schedule( date,
                                               callableJob,
                                               this.scheduler );
            jobHandle.setFuture( future );

            return jobHandle;
        } else {
            return null;
        }
    }

    public boolean removeJob(JobHandle jobHandle) {
        return this.scheduler.remove( (Runnable) ((JDKJobHandle) jobHandle).getFuture() );
    }

    private static ScheduledFuture schedule(Date date,
                                            JDKCallableJob callableJob,
                                            ScheduledThreadPoolExecutor scheduler) {
        long then = date.getTime();
        long now = System.currentTimeMillis();
        ScheduledFuture future = null;
        if ( then >= now ) {
            future = scheduler.schedule( callableJob,
                                         then - now,
                                         TimeUnit.MILLISECONDS );
        } else {
            future = scheduler.schedule( callableJob,
                                         0,
                                         TimeUnit.MILLISECONDS );
        }
        return future;
    }

    public static class JDKCallableJob
        implements
        Callable {
        private Job                         job;
        private Trigger                     trigger;
        private JobContext                  ctx;
        private ScheduledThreadPoolExecutor scheduler;
        private JDKJobHandle                handle;

        public JDKCallableJob(Job job,
                              JobContext ctx,
                              Trigger trigger,
                              JDKJobHandle handle,
                              ScheduledThreadPoolExecutor scheduler) {
            this.job = job;
            this.ctx = ctx;
            this.trigger = trigger;
            this.handle = handle;
            this.scheduler = scheduler;
        }

        public Object call() throws Exception {
            this.job.execute( this.ctx );

            // our triggers allow for flexible rescheduling
            Date date = this.trigger.getNextFireTime();
            if ( date != null ) {
                ScheduledFuture future = schedule( date,
                                                   this,
                                                   this.scheduler );
                this.handle.setFuture( future );
            } 

            return null;
        }
    }

    public static class JDKJobHandle
        implements
        JobHandle {
        private ScheduledFuture future;

        public JDKJobHandle() {

        }

        public ScheduledFuture getFuture() {
            return future;
        }

        public void setFuture(ScheduledFuture future) {
            this.future = future;
        }

    }

}