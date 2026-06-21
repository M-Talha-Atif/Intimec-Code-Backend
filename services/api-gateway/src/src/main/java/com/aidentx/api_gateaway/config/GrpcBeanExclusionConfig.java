package com.aidentx.api_gateaway.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Removes Spring Cloud Gateway's gRPC-related beans that conflict with grpc-netty-shaded.
 * This prevents the ClassNotFoundException for io.grpc.netty.NettyChannelBuilder.
 */
@Configuration
public class GrpcBeanExclusionConfig implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof DefaultListableBeanFactory) {
            DefaultListableBeanFactory registry = (DefaultListableBeanFactory) beanFactory;
            
            // Remove Gateway's gRPC beans that depend on non-shaded grpc-netty
            String[] beansToRemove = {
                "grpcSslConfigurer",
                "jsonToGRPCFilterFactory"
            };
            
            for (String beanName : beansToRemove) {
                if (registry.containsBeanDefinition(beanName)) {
                    registry.removeBeanDefinition(beanName);
                }
            }
        }
    }
}
