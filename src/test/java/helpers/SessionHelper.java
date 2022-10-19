package helpers;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import model.Session;
import model.User;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static helpers.JsonHelper.pojoToJson;
import static model.User.ADMIN_ROLE;
import static model.User.USER_ROLE;
import static steps.AllureSteps.logToAllure;

public class SessionHelper {

    private static final RequestSpecification authentificationSpec = RestAssured.given()
            .baseUri("http://localhost:80/user")
            .header("Content-Type", "application/json");

    private static List<Session> sessions = new ArrayList<Session>();

    public static Session getAdminSession() {
        Session adminSession = sessions.stream()
                .filter(s -> s.getUser().getRole() == User.ADMIN_ROLE).findAny().orElse(null);
        if(adminSession == null) {
            User admin = UserHelper.getAdminUser();
            adminSession = new Session(admin, loginWith(admin));
        }
        return adminSession;
    }

    public static Session getUserSession() {
        Session userSession = sessions.stream()
                .filter(s -> s.getUser().getRole() == User.USER_ROLE).findAny().orElse(null);
        if(userSession == null) {
            Session adminSession = getAdminSession();
            Response getUserResponse = RestAssured.given(authentificationSpec)
                    .header("Authorization", "Bearer " + adminSession.getToken())
                    .get("?username=user");
            User user = UserHelper.getRegularUser();
            if("[]".equals(getUserResponse.body().asString())) {
                Response postUserResponse = authentificationSpec
                        .header("Authorization", "Bearer " + adminSession.getToken())
                        .body(pojoToJson(user))
                        .post("/register");
            }
            userSession = new Session(user, loginWith(user));
        }
        return userSession;
    }

    public static Session getSessionByRole(String role) {
        switch(role) {
            case ADMIN_ROLE:
                return SessionHelper.getAdminSession();
            case USER_ROLE:
                return SessionHelper.getUserSession();
            default:
                logToAllure("Unknown role", role);
                return new Session(new User(), "");
        }
    }

    private static String loginWith(User user) {
        JSONObject requestParams = new JSONObject();
        requestParams.put("username", user.getUsername());
        requestParams.put("password", user.getPassword());

        authentificationSpec.body(requestParams.toJSONString());

        Response response = authentificationSpec.post("/token");

        return response.getBody().jsonPath().get("jwt");
    }
}
