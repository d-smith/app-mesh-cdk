package org.ds.sample.appmesh;

import org.ds.sample.appmesh.components.IamComponents;
import org.ds.sample.appmesh.components.LogGroups;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.appmesh.*;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;

import java.util.Arrays;

public class CdkAppMeshStack extends Stack {
    public CdkAppMeshStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    private String serviceDomain = ".colors";

    private VirtualNodeListener createListener() {
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

    private VirtualNode createVirtualNode(IMesh mesh, String basename) {
        return VirtualNode.Builder.create(this, basename + "VirtualNode")
                .mesh(mesh)
                .virtualNodeName(basename + "-vn")
                .listeners(
                        Arrays.asList(createListener())
                )
                .serviceDiscovery(
                        ServiceDiscovery.dns("colorteller-" + basename + serviceDomain)
                )
                .build();
    }

    private Route createColortellerRoute(IMesh mesh, VirtualRouter colorTellerVirtualRouter,
                                         VirtualNode black, VirtualNode blue, VirtualNode red, VirtualNode white) {
        return Route.Builder.create(this, "colorTellerRoute")
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

    private VirtualNode createTcpEchoVirtualNode(IMesh mesh) {
        return VirtualNode.Builder.create(this, "tcpEchoVirtualNode")
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
                        ServiceDiscovery.dns("tcpecho" + serviceDomain)
                )
                .build();

    }

    public CdkAppMeshStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Vpc vpc = Vpc.Builder.create(this, "MyVpc")
                .maxAzs(3)  // Default is all AZs in region
                .build();

        String applicationMeshName = "colorsMesh";


        Mesh mesh = Mesh.Builder.create(this, applicationMeshName)
                .meshName(applicationMeshName)
                .build();

        Cluster cluster = Cluster.Builder.create(this, "colors-cluster").vpc(vpc).build();

        Role taskRole = IamComponents.createTaskIamRole(this);
        Role taskServiceRole = IamComponents.createTaskExecutionIamRole(this);
        LogGroup logGroup = LogGroups.createLogGroup(this);

        /*
        VirtualNode blackVirtualNode = createVirtualNode(mesh, "black");
        VirtualNode blueVirtualNode = createVirtualNode(mesh, "blue");
        VirtualNode redVirtualNode = createVirtualNode(mesh, "red");
        VirtualNode whiteVirtualNode = createVirtualNode(mesh, "white");

        VirtualRouter colorTellerVirtualRouter =
                VirtualRouter.Builder.create(this, "colorTellerVirtualRouter")
                    .virtualRouterName("colorteller-vr")
                .mesh(mesh)
                .listeners(Arrays.asList(
                        VirtualRouterListener.http(9080)
                ))
                .build();

        Route colorTellerRoute = createColortellerRoute(
                mesh, colorTellerVirtualRouter,
                blackVirtualNode, blueVirtualNode, redVirtualNode, whiteVirtualNode
        );

        VirtualService colorTellerService = VirtualService.Builder.create(this, "colorTellerService")
                .virtualServiceName("colorteller" + serviceDomain)
                .virtualServiceProvider(VirtualServiceProvider.virtualRouter(colorTellerVirtualRouter))
                .build();

        VirtualNode tcpEchoVirtualNode = createTcpEchoVirtualNode(mesh);

        VirtualService tcpEchoVirtualService = VirtualService.Builder.create(this, "tcp-vs")
                .virtualServiceName("tcpecho" + serviceDomain)
                .virtualServiceProvider(
                        VirtualServiceProvider.virtualNode(tcpEchoVirtualNode)
                )
                .build();

        VirtualNode colorGatewayVN = VirtualNode.Builder.create(this, "colorGatewayVN")
                .mesh(mesh)
                .virtualNodeName("colorgateway-vn")
                .listeners(
                        Arrays.asList(
                                VirtualNodeListener.http(HttpVirtualNodeListenerOptions.builder()
                                        .port(9080)
                                        .build()
                                )
                        )
                )
                .serviceDiscovery(
                        ServiceDiscovery.dns("colorgateway" + serviceDomain)
                )
                .backends(
                        Arrays.asList(
                                Backend.virtualService(colorTellerService),
                                Backend.virtualService(tcpEchoVirtualService)
                        )
                )
                .build();

        // -------- ECS Services ---------
*/

    }
}
