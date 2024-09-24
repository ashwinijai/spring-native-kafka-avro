package com.sample.scheduler;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
@Service
public  class ProducerRunnable implements Runnable {

    @SuppressWarnings("rawtypes")
    ScheduledFuture scheduledFuture;

    TaskScheduler taskScheduler ;

    //this method will kill previous scheduler if exists and will create a new scheduler with given cron expression
    public  void reSchedule(String cronExpressionStr){
        if(taskScheduler== null){
            this.taskScheduler = new ConcurrentTaskScheduler();
        }
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(true);
        }
        this.scheduledFuture = this.taskScheduler.schedule(this, new CronTrigger(cronExpressionStr));
    }

    @Override
    public  void run(){
System.out.println("Inside producer. Time is - "+ Instant.now());
    }

    //if you want on application to read data on startup from db and schedule the schduler use following method
    @PostConstruct
    public void initializeScheduler() {
        //@postcontruct method will be called after creating all beans in application context
        // read user config map from db
        // get cron expression created
       // this.reSchedule("cronExp");
    }

}
