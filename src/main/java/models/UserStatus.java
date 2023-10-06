package models;

public enum UserStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    PENDING("pending");

    private final String status;

    UserStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}
