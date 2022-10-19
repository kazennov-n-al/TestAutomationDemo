package helpers;

import model.User;

import static model.User.ADMIN_ROLE;
import static model.User.USER_ROLE;

public class UserHelper {
    public static User getAdminUser() {
        return new User(
                0,
                "admin",
                "admin",
                "admin@email.com",
                "admin",
                "admin",
                ADMIN_ROLE
        );
    }

    public static User getRegularUser() {
        return new User(
                1,
                "user",
                "UserPassword1!",
                "user@email.com",
                "user",
                "user",
                USER_ROLE
        );
    }
}
