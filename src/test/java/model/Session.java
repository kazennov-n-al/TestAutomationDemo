package model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;

@Setter
@Getter
@ToString
public class Session {
    private User user;
    private String token;
    private Date startedAt;

    private Session() { }

    public Session(User user, String token) {
        this.user = user;
        this.token = token;
        this.startedAt = new Date();
    }
}
