import com.github.fge.jsonschema.SchemaVersion;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import helpers.UserHelper;
import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import model.User;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static io.restassured.module.jsv.JsonSchemaValidatorSettings.settings;
import static steps.Assertions.checkResponseCode;
import static steps.Assertions.validateResponseWithJsonSchema;

@Tag("Authentification")
@DisplayName("Authentification tests")
public class TokenApiTest {

    private static final RequestSpecification baseAuthentificationSpec = RestAssured.given()
            .baseUri("http://localhost:80/user/token").header("Content-Type", "application/json");
    private static final JsonSchemaFactory JSON_FACTORY = JsonSchemaFactory.newBuilder()
            .setValidationConfiguration(ValidationConfiguration.newBuilder()
            .setDefaultVersion(SchemaVersion.DRAFTV4).freeze()).freeze();

    @BeforeAll()
    public static void setup() {
        JsonSchemaValidator.settings = settings().with().jsonSchemaFactory(JSON_FACTORY)
                .and().with().checkedValidation(false);
    }


    @ParameterizedTest(name = "Authentificate as non-existing user")
    @MethodSource("getNonExistingUser")
    @Description("User are: {0}")
    public void athentificateAsNonExistingUser(User noSuchUser) {
        RequestSpecification getTokenRequest = RestAssured.given(baseAuthentificationSpec);
        JSONObject requestParams = new JSONObject();
        requestParams.put("username", noSuchUser.getUsername());
        requestParams.put("password", noSuchUser.getPassword());

        getTokenRequest.body(requestParams.toJSONString());

        Response response = getTokenRequest.post("");

        checkResponseCode(response, 400);
        validateResponseWithJsonSchema(response, "schemes/authentification/authentification_failed.json");
    }

    @ParameterizedTest(name = "Authentificate with invalid password")
    @MethodSource("getInvalidPasswordUser")
    @Description("User are: {0}")
    public void authentificateWithInvalidPassword(User invalidPasswordUser) {
        RequestSpecification getTokenRequest = RestAssured.given(baseAuthentificationSpec);
        JSONObject requestParams = new JSONObject();
        requestParams.put("username", invalidPasswordUser.getUsername());
        requestParams.put("password", invalidPasswordUser.getPassword());

        getTokenRequest.body(requestParams.toJSONString());

        Response response = getTokenRequest.post("");

        checkResponseCode(response, 400);
        validateResponseWithJsonSchema(response, "schemes/authentification/authentification_failed.json");
    }

    @ParameterizedTest(name = "Authorize using valid User")
    @MethodSource("getAdminUser")
    @Description("User are: {0}")
    public void authentificateWithValidUser(User validUser) {
        RequestSpecification getTokenRequest = RestAssured.given(baseAuthentificationSpec);
        JSONObject requestParams = new JSONObject();
        requestParams.put("username", validUser.getUsername());
        requestParams.put("password", validUser.getPassword());

        getTokenRequest.body(requestParams.toJSONString());

        Response response = getTokenRequest.post("");

        checkResponseCode(response, 200);
        validateResponseWithJsonSchema(response, "schemes/authentification/authentification_successful.json");
    }

    @Tag("Generate non-existing user")
    static Stream<User> getNonExistingUser() {
        User noSuchUser = UserHelper.getRegularUser();
        noSuchUser.setUsername("noSuchUser");
        return Stream.of(noSuchUser);
    }

    @Tag("Generate user with invalid password")
    static Stream<User> getInvalidPasswordUser() {
        User user = UserHelper.getAdminUser();
        user.setPassword("invalidPassword");
        return Stream.of(user);
    }

    @Tag("Generate admin user and password")
    static Stream<User> getAdminUser() {
        return Stream.of(UserHelper.getAdminUser());
    }
}