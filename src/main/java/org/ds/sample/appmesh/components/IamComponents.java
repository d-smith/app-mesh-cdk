package org.ds.sample.appmesh.components;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import java.util.Arrays;

public class IamComponents {
    public static Role createTaskIamRole(Stack stack) {
       return Role.Builder.create(stack, "task-role")
                .path("/")
                .assumedBy(ServicePrincipal.Builder.create("ecs-tasks.amazonaws.com").build())
                .managedPolicies(Arrays.asList(
                        ManagedPolicy.fromAwsManagedPolicyName("CloudWatchFullAccess"),
                        ManagedPolicy.fromAwsManagedPolicyName("AWSXRayDaemonWriteAccess"),
                        ManagedPolicy.fromAwsManagedPolicyName("AWSAppMeshEnvoyAccess")
                ))
                .build();
    }

    public static Role createTaskExecutionIamRole(Stack stack) {
        return Role.Builder.create(stack, "task-execution-role")
                .path("/")
                .assumedBy(ServicePrincipal.Builder.create("ecs-tasks.amazonaws.com").build())
                .managedPolicies(Arrays.asList(
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ContainerRegistryReadOnly"),
                        ManagedPolicy.fromAwsManagedPolicyName("CloudWatchLogsFullAccess")
                ))
                .build();
    }
}
