package org.k.user;

import com.google.common.base.Preconditions;

import java.util.Objects;

public class UserInfo {
    private final String username;
    private final String passwordHash;
    private final String role;

    public UserInfo(String username, String passwordHash, String role) {
        this.username = Preconditions.checkNotNull(username);
        this.passwordHash = Preconditions.checkNotNull(passwordHash);
        this.role = Preconditions.checkNotNull(role);
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserInfo)) return false;
        UserInfo userInfo = (UserInfo) o;
        return Objects.equals(username, userInfo.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "username='" + username + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
