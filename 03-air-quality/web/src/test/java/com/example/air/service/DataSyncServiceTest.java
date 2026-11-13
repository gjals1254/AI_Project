package com.example.air.service;

import com.example.air.domain.Station;
import com.example.air.dto.MeasurementForm;
import com.example.air.dto.Views.SyncResult;
import com.example.air.openapi.OpenApiClient;
import com.example.air.openapi.OpenApiClient.AirDaily;
import com.example.air.openapi.OpenApiClient.WeatherDaily;
import com.example.air.openapi.OpenMeteoClient;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DataSyncServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 12);
    private static final AirDaily AIR = new AirDaily(45.0, 22.0, 0.03, 0.02, 0.5, 0.004);
    private static final WeatherDaily WEATHER = new WeatherDaily(8.5, 61.0, 2.2, 0.0);

    private StationService stations;
    private OpenApiClient api;
    private OpenMeteoClient meteo;
    private AirQualityService air;
    private DataSyncService sync;

    @BeforeEach
    void setUp() {
        stations = mock(StationService.class);
        api = mock(OpenApiClient.class);
        meteo = mock(OpenMeteoClient.class);
        air = mock(AirQualityService.class);
        sync = service("publicdata");
    }

    private DataSyncService service(String source) {
        return new DataSyncService(stations, api, meteo, air,
            Validation.buildDefaultValidatorFactory().getValidator(), source);
    }

    private static Station station(long id, String name, String airkorea, Integer kma) {
        Station s = new Station();
        s.setId(id);
        s.setName(name);
        s.setAirkoreaName(airkorea);
        s.setKmaStnId(kma);
        return s;
    }

    @Test
    void savesMergedMeasurement() {
        when(stations.findAll(true)).thenReturn(List.of(station(1, "중구", "중구", 108)));
        when(api.fetchAir("중구", DAY)).thenReturn(AIR);
        when(api.fetchWeather(108, DAY)).thenReturn(WEATHER);

        SyncResult r = sync.sync(DAY);

        ArgumentCaptor<MeasurementForm> form = ArgumentCaptor.forClass(MeasurementForm.class);
        verify(air).registerMeasurement(form.capture());
        assertThat(form.getValue().getStationId()).isEqualTo(1L);
        assertThat(form.getValue().getMeasureDate()).isEqualTo(DAY);
        assertThat(form.getValue().getPm10()).isEqualTo(45.0);
        assertThat(form.getValue().getTemperature()).isEqualTo(8.5);
        assertThat(form.getValue().getPrecipitation()).isEqualTo(0.0);
        assertThat(r.success()).isEqualTo(1);
        assertThat(r.failed()).isZero();
    }

    @Test
    void oneStationFailureDoesNotStopOthers() {
        when(stations.findAll(true)).thenReturn(List.of(station(1, "중구", "중구", 108), station(2, "수원", "인계동", 119)));
        when(api.fetchAir("중구", DAY)).thenThrow(new RestClientException("timeout"));
        when(api.fetchAir("인계동", DAY)).thenReturn(AIR);
        when(api.fetchWeather(119, DAY)).thenReturn(WEATHER);

        SyncResult r = sync.sync(DAY);

        verify(air, times(1)).registerMeasurement(any());
        assertThat(r.success()).isEqualTo(1);
        assertThat(r.failed()).isEqualTo(1);
        assertThat(r.errors().get(0)).contains("중구");
    }

    @Test
    void missingPollutantSkipsSave() {
        when(stations.findAll(true)).thenReturn(List.of(station(1, "중구", "중구", 108)));
        when(api.fetchAir("중구", DAY)).thenReturn(new AirDaily(45.0, null, 0.03, 0.02, 0.5, 0.004));
        when(api.fetchWeather(108, DAY)).thenReturn(WEATHER);

        SyncResult r = sync.sync(DAY);

        verify(air, never()).registerMeasurement(any());
        assertThat(r.failed()).isEqualTo(1);
        assertThat(r.errors().get(0)).contains("PM2.5 결측");
    }

    @Test
    void unmappedStationsAreSkipped() {
        when(stations.findAll(true)).thenReturn(List.of(station(1, "중구", null, 108), station(2, "수원", "인계동", null)));

        SyncResult r = sync.sync(DAY);

        verify(api, never()).fetchAir(anyString(), any());
        verify(api, never()).fetchWeather(anyInt(), any());
        assertThat(r.success()).isZero();
        assertThat(r.failed()).isZero();
    }

    @Test
    void outOfRangeValueIsRejected() {
        when(stations.findAll(true)).thenReturn(List.of(station(1, "중구", "중구", 108)));
        when(api.fetchAir("중구", DAY)).thenReturn(AIR);
        when(api.fetchWeather(108, DAY)).thenReturn(new WeatherDaily(99.0, 61.0, 2.2, 0.0));

        SyncResult r = sync.sync(DAY);

        verify(air, never()).registerMeasurement(any());
        assertThat(r.failed()).isEqualTo(1);
        assertThat(r.errors().get(0)).contains("temperature");
    }

    private static Station located(long id, String name, Double lat, Double lon) {
        Station s = station(id, name, null, null);
        s.setLatitude(lat);
        s.setLongitude(lon);
        return s;
    }

    @Test
    void openMeteoUsesCoordinatesWithoutApiMapping() {
        DataSyncService om = service("openmeteo");
        when(stations.findAll(true)).thenReturn(List.of(located(1, "중구", 37.5643, 126.975)));
        when(meteo.fetchAir(37.5643, 126.975, DAY)).thenReturn(AIR);
        when(meteo.fetchWeather(37.5643, 126.975, DAY)).thenReturn(WEATHER);

        SyncResult r = om.sync(DAY);

        verify(air).registerMeasurement(any());
        verify(api, never()).fetchAir(anyString(), any());
        assertThat(r.success()).isEqualTo(1);
    }

    @Test
    void openMeteoSkipsStationsWithoutCoordinates() {
        DataSyncService om = service("openmeteo");
        when(stations.findAll(true)).thenReturn(List.of(located(1, "중구", null, 126.975)));

        SyncResult r = om.sync(DAY);

        verify(meteo, never()).fetchAir(anyDouble(), anyDouble(), any());
        assertThat(r.success() + r.failed()).isZero();
    }

    @Test
    void readyDependsOnSource() {
        when(api.hasKey()).thenReturn(false);
        assertThat(service("openmeteo").ready()).isTrue();
        assertThat(service("publicdata").ready()).isFalse();
    }
}
