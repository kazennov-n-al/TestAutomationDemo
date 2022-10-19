package steps;

import io.qameta.allure.Step;
import io.restassured.response.Response;
import model.Product;
import model.User;
import org.junit.Assert;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

public class Assertions {
    @Step("Assert that status code equals {1}")
    public static void checkResponseCode(Response response, int statusCode) {
        response.then().statusCode(statusCode);
    }

    @Step("Assert that response has empty body")
    public static void checkThatResponseBodyIsEmpty(Response response) {
        Assert.assertEquals(0, response.body().asString().length());
    }

    @Step("Validate response body using JSON Schema {1}")
    public static void validateResponseWithJsonSchema(Response response, String jsonSchemaPath) {
        response.then().assertThat().body(matchesJsonSchemaInClasspath(jsonSchemaPath));
    }

    @Step("Assert product equls product from response")
    public static void assertProductEqualsNoId(Product product, Response response) {
        org.junit.jupiter.api.Assertions.assertAll(
            () -> Assert.assertEquals(response.body().jsonPath().get("name"), product.getName()),
            () -> Assert.assertEquals(response.body().jsonPath().get("description"), product.getDescription()),
            () -> Assert.assertEquals(response.body().jsonPath().getDouble("price"), product.getPrice(), Double.MIN_VALUE)
        );
    }

    @Step("Assert product equls product from response")
    public static void assertProductEquals(Product product, Response response) {
        org.junit.jupiter.api.Assertions.assertAll(
            () -> Assert.assertEquals((Long) response.body().jsonPath().getLong("id"), product.getId()),
            () -> Assert.assertEquals(response.body().jsonPath().get("name"), product.getName()),
            () -> Assert.assertEquals(response.body().jsonPath().get("description"), product.getDescription()),
            () -> Assert.assertEquals(response.body().jsonPath().getDouble("price"), product.getPrice(), Double.MIN_VALUE)
        );
    }

    @Step("Assert user equls user from response")
    public static void asserUserEqualsUserFromResponseNoId(User user, Response response) {
        org.junit.jupiter.api.Assertions.assertAll(
            () -> Assert.assertEquals(response.body().jsonPath().get("username"), user.getUsername()),
            () -> Assert.assertEquals(response.body().jsonPath().get("email"), user.getEmail()),
            () -> Assert.assertEquals(response.body().jsonPath().get("firstName"), user.getFirstName()),
            () -> Assert.assertEquals(response.body().jsonPath().get("lastName"), user.getLastName()),
            () -> Assert.assertEquals(response.body().jsonPath().get("role"), user.getRole())
        );
    }

    @Step("Check error information attributes")
    public static void checkErrorResponse(Response response, Integer code, String reason, String message) {
        org.junit.jupiter.api.Assertions.assertAll(
            () -> Assert.assertEquals(response.body().jsonPath().get("code"), code),
            () -> Assert.assertEquals(response.body().jsonPath().get("reason"), reason),
            () -> Assert.assertEquals(response.body().jsonPath().get("message"), message)
        );
    }

    @Step("Check error information attributes")
    public static void checkErrorInField(Response response, Integer code, String reason, String field, String message) {
        org.junit.jupiter.api.Assertions.assertAll(
                () -> Assert.assertEquals(response.body().jsonPath().get("code"), code),
                () -> Assert.assertEquals(response.body().jsonPath().get("reason"), reason),
                () -> Assert.assertEquals(response.body().jsonPath().get("errors[0].field"), field),
                () -> Assert.assertEquals(response.body().jsonPath().get("errors[0].messages[0]"), message)
        );
    }
}
