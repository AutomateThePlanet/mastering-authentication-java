package models;

import lombok.Data;

@Data
public class TwoFA {
    private String secret;
    private boolean enabled;
}
