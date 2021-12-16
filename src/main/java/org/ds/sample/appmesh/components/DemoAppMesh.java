package org.ds.sample.appmesh.components;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.appmesh.*;

import java.util.Arrays;

public class DemoAppMesh {
    public static VirtualNodeListener createListener() {
        return VirtualNodeListener
                .http(HttpVirtualNodeListenerOptions.builder()
                        .port(9080)
                        .healthCheck(HealthCheck.http(
                                HttpHealthCheckOptions.builder()
                                        .path("/ping")
                                        .healthyThreshold(2)
                                        .unhealthyThreshold(2)
                                        .timeout(Duration.millis(2000))
                                        .interval(Duration.millis(5000))
                                        .build()
                        ))
                        .build());
    }

    public static VirtualNode createVirtualNode(Stack stack, String serviceDomain, IMesh mesh, String basename) {
        return VirtualNode.Builder.create(stack, basename + "VirtualNode")
                .mesh(mesh)
                .virtualNodeName(basename + "-vn")
                .listeners(
                        Arrays.asList(createListener())
                )
                .serviceDiscovery(
                        ServiceDiscovery.dns("colorteller-" + basename + "." + serviceDomain)
                )
                .build();
    }

    public static Route createColortellerRoute(Stack stack, IMesh mesh, VirtualRouter colorTellerVirtualRouter,
                                         VirtualNode black, VirtualNode blue, VirtualNode red, VirtualNode white) {
        return Route.Builder.create(stack, "colorTellerRoute")
                .mesh(mesh)
                .routeName("colorteller-route")
                .virtualRouter(colorTellerVirtualRouter)
                .routeSpec(
                        RouteSpec.http(
                                HttpRouteSpecOptions.builder()
                                        .weightedTargets(
                                                Arrays.asList(
                                                        WeightedTarget.builder()
                                                                .virtualNode(black)
                                                                .weight(1)
                                                                .build(),
                                                        WeightedTarget.builder()
                                                                .virtualNode(blue)
                                                                .weight(1)
                                                                .build(),
                                                        WeightedTarget.builder()
                                                                .virtualNode(red)
                                                                .weight(1)
                                                                .build(),
                                                        WeightedTarget.builder()
                                                                .virtualNode(white)
                                                                .weight(1)
                                                                .build()
                                                )
                                        )
                                        .match(
                                                HttpRouteMatch.builder()
                                                        .path(
                                                                HttpRoutePathMatch.startsWith("/")
                                                        )
                                                        .build()
                                        )
                                        .build()
                        )
                )
                .build();
    }

    public static VirtualNode createTcpEchoVirtualNode(Stack stack, String serviceDomain, IMesh mesh) {
        return VirtualNode.Builder.create(stack, "tcpEchoVirtualNode")
                .mesh(mesh)
                .virtualNodeName("tcpecho-vn")
                .listeners(
                        Arrays.asList(
                                VirtualNodeListener.tcp(
                                        TcpVirtualNodeListenerOptions.builder()
                                                .port(2701)
                                                .healthCheck(
                                                        HealthCheck.tcp(
                                                                TcpHealthCheckOptions.builder()
                                                                        .healthyThreshold(2)
                                                                        .unhealthyThreshold(2)
                                                                        .timeout(Duration.millis(2000))
                                                                        .interval(Duration.millis(5000))
                                                                        .build()
                                                        )
                                                )
                                                .build()
                                )
                        )
                )
                .serviceDiscovery(
                        ServiceDiscovery.dns("tcpecho" + "." + serviceDomain)
                )
                .build();

    }
}
