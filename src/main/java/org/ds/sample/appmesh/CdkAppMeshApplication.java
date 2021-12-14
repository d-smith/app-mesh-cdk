package org.ds.sample.appmesh;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class CdkAppMeshApplication {
    public static void main(final String[] args) {
        App app = new App();
        Environment env = Environment.builder()
                .account(System.getenv("PA_ACCOUNT_NO"))
                .region(System.getenv("AWS_REGION"))
                .build();

        new CdkAppMeshStack(app, "VpcStack", StackProps.builder()
                .env(env)
                .build());

        app.synth();
    }
}

