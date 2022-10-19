import com.github.fge.jsonschema.SchemaVersion;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import helpers.ProductHelper;
import helpers.SessionHelper;
import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import model.Product;
import model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.ArrayList;
import java.util.Arrays;

import static helpers.JsonHelper.pojoToJson;
import static io.restassured.module.jsv.JsonSchemaValidatorSettings.settings;
import static model.User.ADMIN_ROLE;
import static model.User.USER_ROLE;
import static steps.AllureSteps.logToAllure;
import static steps.Assertions.*;

@Tag("Product")
@DisplayName("Product API test")
public class ProductApiTest {

    private static final RequestSpecification baseProductSpec = RestAssured.given()
            .baseUri("http://localhost:80/product").header("Content-Type", "application/json");
    private static final JsonSchemaFactory JSON_FACTORY = JsonSchemaFactory.newBuilder()
            .setValidationConfiguration(ValidationConfiguration.newBuilder()
                    .setDefaultVersion(SchemaVersion.DRAFTV4).freeze()).freeze();

    @BeforeAll()
    public static void setup() {
        JsonSchemaValidator.settings = settings().with().jsonSchemaFactory(JSON_FACTORY)
                .and().with().checkedValidation(false);

        cleanProductData();
    }


    @Test()
    @DisplayName("Post poroduct without authentification")
    @Description("There's no token in request, response status code shold be 401")
    public void postProduct_unauthorized() {
        RequestSpecification postProductRequest = RestAssured.given(baseProductSpec);

        postProductRequest.body(pojoToJson(new Product()));

        Response response = postProductRequest.post("");

        checkResponseCode(response, 401);
        checkThatResponseBodyIsEmpty(response);
    }

    @Test()
    @DisplayName("Post product with invalid token")
    @Description("There is no session created for this token, response should be 401")
    public void postProduct_invalidToken() {
        RequestSpecification postProductRequest = RestAssured.given(baseProductSpec);

        postProductRequest.body(pojoToJson(new Product()));
        postProductRequest.header("Authorization", "Bearer " , "invalidToken");

        Response response = postProductRequest.post("");
        logToAllure("Response", response.body().asString());

        checkResponseCode(response, 401);
        checkThatResponseBodyIsEmpty(response);
    }

    @ParameterizedTest(name = "Post valid product id = {1}, name = {2}, description = {3}, value = {4} as regular user")
    @CsvFileSource(resources = "/data/product/post/postValidProducts.csv", numLinesToSkip = 0)
    @Description("Result shoud have status code 200 and get method should return the product")
    public void postValidProduct(String role, Long id, String name, String description, Double price, String scheme) {
        RequestSpecification postProductRequest = RestAssured.given(baseProductSpec);

        Product requestProduct = new Product(id, name, description, price);
        postProductRequest.body(pojoToJson(requestProduct));
        String sessionToken;
        switch(role) {
            case ADMIN_ROLE:
                sessionToken = SessionHelper.getAdminSession().getToken();
                break;
            case USER_ROLE:
                sessionToken = SessionHelper.getUserSession().getToken();
                break;
            default:
                logToAllure("Unknown role", role);
                sessionToken = "";
        }
        postProductRequest.header("Authorization", "Bearer " + sessionToken);

        Response response = postProductRequest.post("");
        logToAllure("Response", response.body().asString());
        checkResponseCode(response, 200);
        assertProductEqualsNoId(requestProduct, response);
    }

    @ParameterizedTest(name = "Post invalid product Id = {1}, name = {2}, Description = {3}, Price = {4} as a regular user")
    @CsvFileSource(resources = "/data/product/post/postInvalidProducts.csv", numLinesToSkip = 0)
    @Description("Invalid Product, response should be 400")
    public void postInvalidProducts(String role, Long id, String name, String description, Double price, String schemePath) {
        RequestSpecification postProductRequest = RestAssured.given(baseProductSpec);

        Product product = new Product(id, name, description, price);

        postProductRequest.body(pojoToJson(product));
        postProductRequest.header("Authorization", "Bearer " + SessionHelper.getUserSession().getToken());

        Response response = postProductRequest.post("");
        logToAllure("Response", response.body().asString());

        checkResponseCode(response, 400);
        validateResponseWithJsonSchema(response, schemePath);
    }

    @ParameterizedTest(name = "Delete product as {0}")
    @CsvFileSource(resources = "/data/product/delete/deletePositive.csv", numLinesToSkip = 0)
    public void deletePositive(String role, Integer statusCode) {
        Product product = ProductHelper.postGenericProduct();
        RequestSpecification deleteProductRequest = RestAssured.given(baseProductSpec);

        deleteProductRequest.header("Authorization", "Bearer " + SessionHelper.getSessionByRole(role).getToken());

        Response response = deleteProductRequest.delete("/" + product.getId());
        logToAllure("Response", response.body().asString());

        checkResponseCode(response, statusCode);
        checkThatResponseBodyIsEmpty(response);
    }

    @Test()
    @DisplayName("Delete inexisting product")
    public void deleteInexistingProduct() {
        RequestSpecification getProductsRequest = RestAssured.given(baseProductSpec);
        getProductsRequest.header("Authorization", "Bearer " + SessionHelper.getSessionByRole(ADMIN_ROLE).getToken());

        Response getAllResponse = getProductsRequest.get("/");
        logToAllure("Response to GET", getAllResponse.body().asString());

        Integer inexistingId = ((ArrayList<Integer>) getAllResponse.getBody().jsonPath().get("id")).
                stream().max((a, b) -> { return a > b ? 1 : -1; }).orElse(1) + 1;

        RequestSpecification deleteRequest = RestAssured.given(baseProductSpec);
        deleteRequest.header("Authorization", "Bearer " + SessionHelper.getSessionByRole(ADMIN_ROLE).getToken());

        Response deleteResponse = deleteRequest.delete("/" + inexistingId);
        logToAllure("Response to DELETE", deleteResponse.body().asString());

        checkResponseCode(deleteResponse, 404);
        checkErrorResponse(deleteResponse, 404, "Not Found", "Product with id=" + inexistingId + " was not found.");
    }

    @AfterAll
    public static void cleanUp() {
        cleanProductData();
    }

    private static void cleanProductData() {
        RequestSpecification getAllProductsReqest = RestAssured.given(baseProductSpec);
        getAllProductsReqest.header("Authorization", "Bearer " + SessionHelper.getAdminSession().getToken());

        Object[] products = Arrays.stream(getAllProductsReqest.get("/").getBody().as(Product[].class)).toArray();

        for(Object userObject : products) {
            Product product = (Product) userObject;
            RequestSpecification deleteRequest = RestAssured.given(baseProductSpec);
            deleteRequest.header("Authorization", "Bearer " + SessionHelper.getAdminSession().getToken());
            deleteRequest.delete("/" + product.getId());
        }
    }
}