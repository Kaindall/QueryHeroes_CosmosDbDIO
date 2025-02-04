package edu.dio.zanetti;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;

public class CosmosConnector {
    private static CosmosAsyncClient instance;

    private CosmosConnector() {}

    public static CosmosAsyncClient getInstance() {

        System.out.println("Instanciando Cosmos Client!");

        if (instance == null) {
             instance = new CosmosClientBuilder()
                .endpoint(System.getenv("COSMOS_URI"))
                .key(System.getenv("COSMOS_KEY"))
                .buildAsyncClient();
        }
        return instance;
    }
}
