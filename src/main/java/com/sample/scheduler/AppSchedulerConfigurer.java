package com.sample.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronSequenceGenerator;

import java.util.Date;

@EnableScheduling
@Configuration
public class AppSchedulerConfigurer implements SchedulingConfigurer {

    @Autowired
    private ProducerRunnable fileReportNotificationTask;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        taskRegistrar.addTriggerTask(fileReportNotificationTask, new Trigger() {

            @Override
            public Date nextExecutionTime(TriggerContext triggerContext) {
                Date date = triggerContext.lastScheduledExecutionTime();
                CronSequenceGenerator generator = new CronSequenceGenerator("0 0 23 * * ?");
                return generator.next(new Date());

            }
        });

    }

}

