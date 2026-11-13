package com.example.air.dto;

import com.example.air.domain.Station;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StationFormTest {

    private StationForm baseForm() {
        StationForm f = new StationForm();
        f.setName("중구");
        f.setRegion("서울");
        return f;
    }

    @Test
    void applyToCopiesApiMapping() {
        StationForm f = baseForm();
        f.setAirkoreaName("중구");
        f.setKmaStnId(108);
        Station s = new Station();
        f.applyTo(s);
        assertThat(s.getAirkoreaName()).isEqualTo("중구");
        assertThat(s.getKmaStnId()).isEqualTo(108);
    }

    @Test
    void blankAirkoreaNameIsStoredAsNull() {
        StationForm f = baseForm();
        f.setAirkoreaName("   ");
        Station s = new Station();
        f.applyTo(s);
        assertThat(s.getAirkoreaName()).isNull();
    }

    @Test
    void fromCopiesApiMapping() {
        Station s = new Station();
        s.setName("수원");
        s.setRegion("경기");
        s.setAirkoreaName("인계동");
        s.setKmaStnId(119);
        StationForm f = StationForm.from(s);
        assertThat(f.getAirkoreaName()).isEqualTo("인계동");
        assertThat(f.getKmaStnId()).isEqualTo(119);
    }
}
