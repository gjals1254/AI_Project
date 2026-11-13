package com.example.air.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Ai ai = new Ai();
    private final Security security = new Security();
    private final OpenApi openapi = new OpenApi();

    public Ai getAi() { return ai; }
    public Security getSecurity() { return security; }
    public OpenApi getOpenapi() { return openapi; }

    /** 공공데이터포털(에어코리아·기상청) 연동. 인증키는 web/config/application.yml 에 둔다. */
    public static class OpenApi {
        private String serviceKey = "";
        private String airkoreaBaseUrl = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc";
        private String kmaBaseUrl = "https://apis.data.go.kr/1360000/AsosDalyInfoService";
        private int timeoutMs = 10000;

        public String getServiceKey() { return serviceKey; }
        public void setServiceKey(String serviceKey) { this.serviceKey = serviceKey; }
        public String getAirkoreaBaseUrl() { return airkoreaBaseUrl; }
        public void setAirkoreaBaseUrl(String airkoreaBaseUrl) { this.airkoreaBaseUrl = airkoreaBaseUrl; }
        public String getKmaBaseUrl() { return kmaBaseUrl; }
        public void setKmaBaseUrl(String kmaBaseUrl) { this.kmaBaseUrl = kmaBaseUrl; }
        public int getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    }

    public static class Ai {
        private String baseUrl = "http://localhost:8003";
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
        private String userUsername = "operator";
        private String userPassword = "operator1234";

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
