package edu.dio.zanetti;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosAsyncContainer;
import com.azure.cosmos.CosmosAsyncDatabase;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpExample". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpExample
     * 2. curl "{your host}/api/HttpExample?name=HTTP%20Query"
     */
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        
        if (request.getHttpMethod().equals(HttpMethod.POST)) {
            context.getLogger().info("Requisition POST recognized");
            return Optional.ofNullable(request.getBody().get())
                .map(body -> {
                    SuperheroRequest hero = createSuper(body).orElseThrow();
                    context.getLogger().info("Succefully mapped body to Java POJO");
                    BlobContainerClient containerClient = StorageConnector.getInstance();

                    context.getLogger().info("Succefully received Blob Container instance");
                    byte[] imageBytes = Base64.getDecoder().decode(hero.getImageBase64());
                    String blobName = hero.getName().replaceAll("\s+", "_") + ".png";
                    BlobClient blobClient = containerClient.getBlobClient(blobName);
                    
                    try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
                        BlobHttpHeaders headers = new BlobHttpHeaders().setContentType("image/jpeg");
                        blobClient.upload(inputStream, imageBytes.length, true);
                        blobClient.setHttpHeaders(headers);
                        context.getLogger().info("Image successfully uploaded to Blob Storage");
                    } catch (Exception e) {
                        context.getLogger().severe("Error uploading image: " + e.getMessage());
                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to upload image").build();
                    }

                    SuperheroResponse heroResponse = querySuper(hero, blobClient.getBlobUrl()).get();
                    CosmosAsyncClient cosmosClient = CosmosConnector.getInstance();
                    CosmosAsyncDatabase database = cosmosClient.getDatabase("ToDoList");
                    CosmosAsyncContainer container = database.getContainer("Items");
                    

                    container.createItem(heroResponse).subscribe(
                        response -> context.getLogger().info("Successfully stored hero in Cosmos DB"),
                        error -> context.getLogger().severe("Error storing hero in Cosmos DB: " + error.getMessage())
                    );

                    return request.createResponseBuilder(HttpStatus.OK).body(heroResponse).build();
                })
                .orElseGet(() -> request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body("Passe as características do herói a ser publicado: name, power, alter, imageUrl").build());
        }
            
        final String queryId = request.getQueryParameters().get("id");

        if (queryId == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Passe o ID a ser buscado na consulta").build();
        }

        CosmosAsyncClient cosmosClient = CosmosConnector.getInstance();
        CosmosAsyncDatabase database = cosmosClient.getDatabase("ToDoList");
        CosmosAsyncContainer container = database.getContainer("Items");

        SuperheroResponse heroResponse;
        try {
            heroResponse = container.readItem(queryId, PartitionKey.NONE, SuperheroResponse.class)
                .map(CosmosItemResponse::getItem)
                .toFuture()
                .get();
            ObjectMapper mapper = new ObjectMapper();
            String jsonResponse = mapper.writeValueAsString(heroResponse);
            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(jsonResponse).build();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Fala em converter o herói encontrado em objeto de retorno").build();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Fala em converter o herói encontrado em objeto de retorno").build();
        }
    }

    private Optional<SuperheroRequest> createSuper(String hero) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Optional.of(mapper.readValue(hero, SuperheroRequest.class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Optional.empty();
        } 
    }

    private Optional<SuperheroResponse> querySuper(SuperheroRequest hero, String imgUrl) {
        SuperheroResponse heroResponse = new SuperheroResponse();
        heroResponse.setId(UUID.randomUUID().toString());
        heroResponse.setName(hero.getName());
        heroResponse.setPower(hero.getPower());
        heroResponse.setAlias(hero.getAlias());
        heroResponse.setImageUrl(imgUrl);
        return Optional.of(heroResponse);
    }
}
