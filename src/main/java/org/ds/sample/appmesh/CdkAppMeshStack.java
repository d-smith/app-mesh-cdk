package org.ds.sample.appmesh;

import org.ds.sample.appmesh.components.DemoAppMesh;
import org.ds.sample.appmesh.components.IamComponents;
import org.ds.sample.appmesh.components.LogGroups;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.appmesh.*;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.HealthCheck;
import software.amazon.awscdk.services.events.targets.EcsTask;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.servicediscovery.PrivateDnsNamespace;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class CdkAppMeshStack extends Stack {
    public CdkAppMeshStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    private final String serviceDomain = "colors.local";


    public CdkAppMeshStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);


        Vpc vpc = Vpc.Builder.create(this, "MyVpc")
                .maxAzs(3)  // Default is all AZs in region
                .build();

        String applicationMeshName = "colorsMesh";


        Mesh mesh = Mesh.Builder.create(this, applicationMeshName)
                .meshName(applicationMeshName)
                .build();

        Cluster cluster = Cluster.Builder.create(this, "colors-cluster")
                .vpc(vpc)
                .build();

        PrivateDnsNamespace serviceNamespace = PrivateDnsNamespace.Builder.create(this, "service-ns")
                .name(serviceDomain)
                .vpc(vpc)
                .build();

        Role taskRole = IamComponents.createTaskIamRole(this);
        Role taskServiceRole = IamComponents.createTaskExecutionIamRole(this);
        LogGroup logGroup = LogGroups.createLogGroup(this);


        VirtualNode blackVirtualNode = DemoAppMesh.createVirtualNode(this, serviceDomain, mesh, "black");
        VirtualNode blueVirtualNode = DemoAppMesh.createVirtualNode(this, serviceDomain, mesh, "blue");
        VirtualNode redVirtualNode = DemoAppMesh.createVirtualNode(this, serviceDomain, mesh, "red");
        VirtualNode whiteVirtualNode = DemoAppMesh.createVirtualNode(this, serviceDomain, mesh, "white");

        VirtualRouter colorTellerVirtualRouter =
                VirtualRouter.Builder.create(this, "colorTellerVirtualRouter")
                        .virtualRouterName("colorteller-vr")
                        .mesh(mesh)
                        .listeners(Arrays.asList(
                                VirtualRouterListener.http(9080)
                        ))
                        .build();

        Route colorTellerRoute = DemoAppMesh.createColortellerRoute(
                this, mesh, colorTellerVirtualRouter,
                blackVirtualNode, blueVirtualNode, redVirtualNode, whiteVirtualNode
        );

        VirtualService colorTellerService = VirtualService.Builder.create(this, "colorTellerService")
                .virtualServiceName("colorteller" + "." + serviceDomain)
                .virtualServiceProvider(VirtualServiceProvider.virtualRouter(colorTellerVirtualRouter))
                .build();

        VirtualNode tcpEchoVirtualNode = DemoAppMesh.createTcpEchoVirtualNode(this, serviceDomain, mesh);

        VirtualService tcpEchoVirtualService = VirtualService.Builder.create(this, "tcp-vs")
                .virtualServiceName("tcpecho" + "." + serviceDomain)
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
                        ServiceDiscovery.dns("colorgateway" + "." + serviceDomain)
                )
                .backends(
                        Arrays.asList(
                                Backend.virtualService(colorTellerService),
                                Backend.virtualService(tcpEchoVirtualService)
                        )
                )
                .build();

        // -------- ECS Services ---------
        TaskDefinition colorTaskDef = TaskDefinition.Builder.create(this, "color-task")
                .family("task")
                .compatibility(Compatibility.EC2_AND_FARGATE)
                .proxyConfiguration(
                        AppMeshProxyConfiguration.Builder.create()
                                .containerName("envoy")
                                .properties(
                                        AppMeshProxyConfigurationProps.builder()
                                                .ignoredUid(1337)
                                                .proxyIngressPort(15000)
                                                .proxyEgressPort(15001)
                                                .appPorts(Arrays.asList(9000))
                                                .egressIgnoredIPs(Arrays.asList("169.254.170.2", "169.254.169.254"))
                                                .build()
                                )
                                .build()
                )
                .cpu("512")
                .memoryMiB("1024")
                .build();

        IRepository ctRepo = Repository.fromRepositoryName(this, "ctrepo", "colorteller");
        EcrImage ctImage = RepositoryImage.fromEcrRepository(ctRepo, "latest");

        ContainerDefinition black = colorTaskDef.addContainer("app",
                ContainerDefinitionOptions.builder()
                        .containerName("app")
                        .image(ctImage)
                        .portMappings(
                                Arrays.asList(PortMapping.builder()
                                        .containerPort(9080)
                                        .hostPort(9080)
                                        .protocol(Protocol.TCP)
                                        .build())
                        )
                        .environment(Map.of(
                                "COLOR", "black",
                                "SERVER_PORT", "9080"
                        ))
                        .logging(
                                LogDriver.awsLogs(
                                        AwsLogDriverProps.builder()
                                                .logGroup(logGroup)
                                                .streamPrefix("bb")
                                                .build()
                                )
                        )
                        .essential(true)
                        .memoryLimitMiB(512)
                        .build()
        );

        IRepository envoyRepo = Repository.fromRepositoryArn(this, "envoyRepo", "arn:aws:ecr:us-west-2:840364872350:repository/aws-appmesh-envoy");
        EcrImage envoyImage = RepositoryImage.fromEcrRepository(envoyRepo, "v1.20.0.1-prod");
        ContainerDefinition envoy = colorTaskDef.addContainer("envoy",
                ContainerDefinitionOptions.builder()
                        .containerName("envoy")
                        .image(envoyImage)
                        .user("1337")
                        .essential(true)
                        .memoryLimitMiB(512)
                        .portMappings(
                                Arrays.asList(
                                        PortMapping.builder()
                                                .containerPort(9901)
                                                .hostPort(9901)
                                                .protocol(Protocol.TCP)
                                                .build(),
                                        PortMapping.builder()
                                                .containerPort(15000)
                                                .hostPort(15000)
                                                .protocol(Protocol.TCP)
                                                .build(),
                                        PortMapping.builder()
                                                .containerPort(15001)
                                                .hostPort(15001)
                                                .protocol(Protocol.TCP)
                                                .build()
                                )
                        )
                        .environment(
                                Map.of(
                                        "APPMESH_VIRTUAL_NODE_NAME", "mesh/" + applicationMeshName + "/virtualNode/colorteller-black-vn",
                                        "ENVOY_LOG_LEVEL", "debug"
                                )
                        )
                        .logging(
                                LogDriver.awsLogs(
                                        AwsLogDriverProps.builder()
                                                .logGroup(logGroup)
                                                .streamPrefix("ee")
                                                .build()
                                )
                        )
                        .healthCheck(
                                HealthCheck.builder()
                                        .command(List.of(
                                                "CMD-SHELL",
                                                "curl -s http://localhost:9901/server_info | grep state | grep -q LIVE"
                                        ))
                                        .interval(Duration.seconds(5))
                                        .timeout(Duration.seconds(2))
                                        .retries(3)
                                        .build()
                        )
                        .build()

        );

        envoy.addUlimits(Ulimit.builder()
                .name(UlimitName.NOFILE)
                .hardLimit(15000)
                .softLimit(15000)
                .build());

        black.addContainerDependencies(
                ContainerDependency.builder()
                    .container(envoy)
                    .condition(ContainerDependencyCondition.HEALTHY)
                    .build()
        );

    }
}
