package com.aidentx.shared.dgraph;

import io.dgraph.DgraphClient;
import io.dgraph.DgraphGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DgraphProperties.class)
@ConditionalOnProperty(prefix = "dgraph", name = "hosts")
public class DgraphAutoConfiguration {

    @Bean
    public DgraphClient dgraphClient(DgraphProperties properties) {
        String[] hosts = properties.getHosts().split(",");
        DgraphGrpc.DgraphStub[] stubs = new DgraphGrpc.DgraphStub[hosts.length];
        
        for (int i = 0; i < hosts.length; i++) {
            String[] hostPort = hosts[i].trim().split(":");
            String hostname = hostPort[0];
            int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 9080;
            
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(hostname, port)
                    .usePlaintext()
                    .build();
            stubs[i] = DgraphGrpc.newStub(channel);
        }
        
        return new DgraphClient(stubs);
    }
}