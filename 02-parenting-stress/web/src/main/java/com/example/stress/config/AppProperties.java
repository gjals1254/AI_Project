package com.example.stress.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Ai ai = new Ai();
    private final Security security = new Security();

    public Ai getAi() { return ai; }
    public Security getSecurity() { return security; }

    public static class Ai {
        private String baseUrl = "http://localhost:8002";
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 30000;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }

    public static class Security {
        private String adminUsername = "admin";
        private String adminPassword = "admin1234";
        private String userUsername = "counselor";
        private String userPassword = "counselor1234";

        public String getAdminUsername() { return adminUsername; }
        public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
        public String getAdminPassword() { return adminPassword; }
        public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
        public String getUserUsername() { return userUsername; }
        public void setUserUsername(String userUsername) { this.userUsername = userUsername; }
        public String getUserPassword() { return userPassword; }
        public void setUserPassword(String userPassword) { this.userPassword = userPassword; }
    }
}
