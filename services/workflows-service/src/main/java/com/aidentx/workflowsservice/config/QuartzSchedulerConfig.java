package com.aidentx.workflowsservice.config;

import org.quartz.spi.JobFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

import javax.sql.DataSource;

/**
 * Quartz Scheduler configuration for time-based workflow triggers
 * All Quartz properties are configured in application.properties
 */
@Configuration
public class QuartzSchedulerConfig {

    private final DataSource dataSource;
    private final ApplicationContext applicationContext;

    public QuartzSchedulerConfig(DataSource dataSource, ApplicationContext applicationContext) {
        this.dataSource = dataSource;
        this.applicationContext = applicationContext;
    }

    @Bean
    public JobFactory jobFactory() {
        SpringBeanJobFactory jobFactory = new SpringBeanJobFactory();
        jobFactory.setApplicationContext(applicationContext);
        return jobFactory;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setJobFactory(jobFactory());
        factory.setDataSource(dataSource);
        factory.setWaitForJobsToCompleteOnShutdown(true);
        factory.setAutoStartup(true);
        factory.setOverwriteExistingJobs(true);
        return factory;
    }
}
