package com.example.trademark.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** application.yml 의 app.* 설정 */
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String uploadDir = "./uploads";
    private final Ai ai = new Ai();
    private final Search search = new Search();
    private final Security security = new Security();

    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }
    public Ai getAi() { return ai; }
    public Search getSearch() { return search; }
    public Security getSecurity() { return security; }

    public static class Ai {
        private String baseUrl = "http://localhost:8001";
        private int connectTimeoutMs = 3000;
        private int readTimeoutMs = 60000;

        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getConnectTimeoutMs() { return connectTimeoutMs; }
        public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
        public int getReadTimeoutMs() { return readTimeoutMs; }
        public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    }

    public static class Search {
        private int topK = 10;
        private double minScore = 0.0;
        private double highThreshold = 0.85;
        private double mediumThreshold = 0.70;

        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public double getMinScore() { return minScore; }
        public void setMinScore(double minScore) { this.minScore = minScore; }
        public double getHighThreshold() { return highThreshold; }
        public void setHighThreshold(double highThreshold) { this.highThreshold = highThreshold; }
        public double getMediumThreshold() { return mediumThreshold; }
        public void setMediumThreshold(double mediumThreshold) { this.mediumThreshold = mediumThreshold; }
    }

    public static class Security {
        private String adminUsername = "admin";
        private String adminPassword = "admin1234";
        private String userUsername = "user";
        private String userPassword = "user1234";

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
