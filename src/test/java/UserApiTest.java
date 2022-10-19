import com.github.fge.jsonschema.SchemaVersion;
import com.github.fge.jsonschema.cfg.ValidationConfiguration;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import helpers.SessionHelper;
import io.qameta.allure.Description;
import io.restassured.RestAssured;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.util.Arrays;
import java.util.List;

import static helpers.JsonHelper.pojoToJson;
import static io.restassured.module.jsv.JsonSchemaValidatorSettings.settings;
import static steps.AllureSteps.logToAllure;
import static steps.Assertions.*;

@Tag("Product")
@DisplayName("User API test")
public class UserApiTest {
    //TODO put content-type into common request spec in the parent abstract class for all tests
    private static final RequestSpecification baseUserSpec = RestAssured.given()
            .baseUri("http://localhost:80/user").header("Content-Type", "application/json");
    private static final JsonSchemaFactory JSON_FACTORY = JsonSchemaFactory.newBuilder()
            .setValidationConfiguration(ValidationConfiguration.newBuilder()
                    .setDefaultVersion(SchemaVersion.DRAFTV4).freeze()).freeze();

    @BeforeAll()
    public static void setup() {
        JsonSchemaValidator.settings = settings().with().jsonSchemaFactory(JSON_FACTORY)
                .and().with().checkedValidation(false);
        cleanUserData();
    }

    @ParameterizedTest(name = "Post valid user: name = {0}, password = {1}, email = {2}, firstName = {3},"
                            + "lastName = {4}, role = {5}")
    @CsvFileSource(resources = "/data/user/post/postValidUser.csv", numLinesToSkip = 0)
    @Description("Only admin is allowed to register a user")
    public void postValidUsers(String username, String password, String email, String firstName, String lastName, String role) {
        RequestSpecification postUserReqest = RestAssured.given(baseUserSpec);
        User userToRegister = new User(null, username, password, email, firstName, lastName, role);
        postUserReqest.body(pojoToJson(userToRegister));

        //TODO put Authorization and Bearer routine into a separate method
        postUserReqest.header("Authorization", "Bearer " + SessionHelper.getAdminSession().getToken());

        //TODO move /register constant to some apropriate place
        Response registerUserResponse = postUserReqest.post("/register");
        logToAllure("Response to POST user", registerUserResponse.asString());

        checkResponseCode(registerUserResponse, 200);
        asserUserEqualsUserFromResponseNoId(userToRegister, registerUserResponse);
    }

    @Test()
    @DisplayName("User constraint violations")
    @Description("Unable to post user with the same Username or Email as an existing user")
    public void postDuplicateUsers() {
        RequestSpecification postOriginalUser = RestAssured.given(baseUserSpec);
        String email = "original@mail.com";
        String userName = "originalUser";

        //TODO put Authorization and Bearer routine into a separate method
        postOriginalUser.header("Authorization", "Bearer " + SessionHelper.getAdminSession().getToken());
        postOriginalUser.body(pojoToJson(new User(null, userName, "originalUser1!", email,
                "Original", "Original", "admin")));

        //TODO move /register constant to some apropriate place
        Response originalUserResponse = postOriginalUser.post("/register");
        logToAllure("Original user", originalUserResponse.asString());

        User duplicateEmail = new User(null, "duplicate", "DuplicateUser1!",
                email, "Duplicate", "Duplicate", "user");

        User duplicateUsername = new User(null, userName, "DuplicateUser1!",
                "nonDuplicate@email.com", "Duplicate", "Duplicate", "user");

        RequestSpecification registerUserWithDuplicateEmail = RestAssured.given(baseUserSpec);
        registerUserWithDuplicateEmail.header("Authorization", "Bearer " + SessionHelper.getAdminSession().getToken());
        registerUserWithDuplicateEmail.body(pojoToJson(duplicateEmail));
        Response postDuplicateEmail = registerUserWithDuplicateEmail.post("/register");
        logToAllure("Register a user with duplicate email", postDuplicateEmail.asString());

        checkResponseCode(postDuplicateEmail, 400);
        checkErrorInField(postDuplicateEmail, 400, "Bad Request", "email", "'" + email + "' email address has already been taken.");

        //TODO move POST user into separate test step
        RequestSpecification registerUserWithDuplicateLogin = RestAssured.given(baseUserSpec);
        registerUserWithDuplicateEmail.header("Authorization", "Bearer " + SessionHelper.getAdminSession().getToken());
        registerUserWithDuplicateEmail.body(pojoToJson(duplicateUsername));
        Response postDuplicateLogin = registerUserWithDuplicateEmail.post("/register");
        logToAllure("Register a user with duplicate Username", postDuplicateLogin.asString());

        checkResponseCode(postDuplicateLogin, 400);
        checkErrorInField(postDuplicateLogin, 400, "Bad Request", "username", "'" + userName + "' username has already been taken.");
    }

    @ParameterizedTest(name = "Post invalid user: name = {0}, password = {1}, email = {2}, firstName = {3},"
            + "lastName = {4}, role = {5}")
    @CsvFileSource(resources = "/data/user/post/postInvalidUser.csv", numLinesToSkip = 0)
    @Description("All users have some constraint violation")
    public void postInvalidUsers(String username, String password, String email, String firstName,
                                 String lastName, String role, String invalidField, String errorMessage) {
        RequestSpecification postUserReqest = RestAssured.given(baseUserSpec);
        User userToRegister = new User(null, username, password, email, firstName, lastName, role);
        postUserReqest.body(pojoToJson(userToRegister));

        //TODO put Authorization and Bearer routine into a separate method
        postUserReqest.header("Authorization", "Bearer " + SessionHelper.getAdminSession().getToken());

        //TODO move /register constant to some apropriate place
        Response registerUserResponse = postUserReqest.post("/register");
        logToAllure("Response to POST user", registerUserResponse.asString());

        checkResponseCode(registerUserResponse, 400);
        checkErrorInField(registerUserResponse, 400, "Bad Request", invalidField, errorMessage);

    }

    @AfterAll()
    public static void cleanup() {
        cleanUserData();
    }

    private static void cleanUserData() {
        RequestSpecification getAllUsersReqest = RestAssured.given(baseUserSpec);
        getAllUsersReqest.header("Authorization", "Bearer " + SessionHelper.getAdminSession().getToken());

        Object[] users = Arrays.stream(getAllUsersReqest.get("/").getBody().as(User[].class))
                .filter(u -> (!"admin".equals(u.getUsername()))).toArray();

        for(Object userObject : users) {
            User user = (User) userObject;
            RequestSpecification deleteRequest = RestAssured.given(baseUserSpec);
            deleteRequest.header("Authorization", "Bearer " + SessionHelper.getAdminSession().getToken());
            deleteRequest.delete("/" + user.getId());
        }
    }
}
