package com.aidentx.applicationpermissions.roles_management.event.producer;

import com.aidentx.applicationpermissions.roles_management.event.payload.RoleAssignmentBatchEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoleAssignmentBatchProducer {

    private static final String TOPIC = "role-assignment-jobs";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RoleAssignmentBatchProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(RoleAssignmentBatchEvent event) {
        kafkaTemplate.send(
                TOPIC,
                event.getBatchId().toString(),
                event
        );
    }
}
