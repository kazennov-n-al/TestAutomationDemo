package helpers;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import model.Product;

public class ProductHelper {
    public static Product postGenericProduct() {
        SessionHelper.getAdminSession();
        Product genericProduct = new Product(null, "SomeName", "SomeDescription", 51.0);

        Response response = RestAssured.given()
                .baseUri("http://localhost:80/product")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer "  + SessionHelper.getAdminSession().getToken())
                .body(JsonHelper.pojoToJson(genericProduct))
                .post("");

        genericProduct.setId(response.getBody().jsonPath().getLong("id"));

        return genericProduct;
    }
}
