package model;

import lombok.*;

@Setter
@Getter
@ToString
@AllArgsConstructor
@RequiredArgsConstructor
public class User {

    public static final String ADMIN_ROLE = "admin";
    public static final String USER_ROLE = "user";

    Integer id;
    String username;
    String password;
    String email;
    String firstName;
    String lastName;
    String role;
}
