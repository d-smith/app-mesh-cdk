package org.ds.sample.appmesh.components;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

public class LogGroups {
    public static LogGroup createLogGroup(Stack stack) {
        return LogGroup.Builder.create(stack, "ecs-service-log-group")
                .retention(RetentionDays.TWO_WEEKS)
                .build();

    }
}
