package com.shelter.peace.weather.service;

import com.shelter.peace.weather.dto.TodayResponseDTO;
import com.shelter.peace.weather.dto.WeekResponseDTO;
import com.shelter.peace.weather.dtoToday.OpenWeatherResponseDTO;
import com.shelter.peace.weather.dtoWeek.WeekWeatherItemDTO;
import com.shelter.peace.weather.dtoWeek.WeekWeatherResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OpenWeatherService {
    public final String weatherUrl = "https://api.openweathermap.org/data/2.5/";
    public final String now = "weather";
    public final String week = "forecast";
    public final double aaa = 273.15;

    @Value("${open.weather.map.api.key}")
    public String apiKey;

    public TodayResponseDTO getNowWeather(double lat, double lon) {
        try {
            StringBuilder stringBuilder = new StringBuilder()
                    .append(weatherUrl)
                    .append(now)
                    .append("?lat=")
                    .append(lat)
                    .append("&lon=")
                    .append(lon)
                    .append("&lang=kr")
                    .append("&units=metric")
                    .append("&appid=")
                    .append(apiKey);

            RestTemplate restTemplate = new RestTemplate();
            OpenWeatherResponseDTO json = restTemplate.getForObject(new URI(stringBuilder.toString()), OpenWeatherResponseDTO.class);

            TodayResponseDTO responseDTO = TodayResponseDTO.builder()
                    .temp(json.getMain().getTemp() + " C")
                    .description(json.getWeather().get(0).getDescription())
                    .feelTemp(json.getMain().getFeels_like() + " C")
                    .humidity(json.getMain().getHumidity() + " %")
                    .windSpeed(json.getWind().getSpeed() + " m/s")
                    .cloud(json.getClouds().getAll() + " %")
                    .sunrise(dtToLocalDateTime(json.getSys().getSunrise()).toString())
                    .sunset(dtToLocalDateTime(json.getSys().getSunset()).toString())
                    .country(json.getSys().getCountry())
                    .build();

            if (json.getRain() != null) {
                responseDTO.setRainInfo(true);
                responseDTO.setRain1h(json.getRain().getRain1h() + "mm");
            } else {
                responseDTO.setRainInfo(false);
                responseDTO.setRain1h("0mm");
            }

            return responseDTO;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public List<WeekResponseDTO> getWeekWeather(double lat, double lon) {
        try {
            StringBuilder stringBuilder = new StringBuilder()
                    .append(weatherUrl)
                    .append(week)
                    .append("?lat=")
                    .append(lat)
                    .append("&lon=")
                    .append(lon)
                    .append("&lang=kr")
                    .append("&units=metric")
                    .append("&appid=")
                    .append(apiKey);

            RestTemplate restTemplate = new RestTemplate();

            WeekWeatherResponseDTO json = restTemplate.getForObject(new URI(stringBuilder.toString()), WeekWeatherResponseDTO.class);
            return getWeekWeatherInfo(json.getList());

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public List<WeekResponseDTO> getWeekWeatherInfo(List<WeekWeatherItemDTO> weatherItemList) {
        List<WeekResponseDTO> returnList = new ArrayList<>();

        Map<LocalDate, List<WeekWeatherItemDTO>> groupedByDate = weatherItemList.stream()
                .collect(
                        Collectors.groupingBy(
                                item -> dtToLocalDate(item.getDt())
                        ));
        Map<LocalDate, List<WeekWeatherItemDTO>> sortedGroupedByDate = groupedByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));

        sortedGroupedByDate.forEach((date, weatherList) -> {
            double minTemp = weatherList.stream()
                    .mapToDouble(item -> item.getMain().getTemp())
                    .min()
                    .orElse(Double.NaN);

            double maxTemp = weatherList.stream()
                    .mapToDouble(item -> item.getMain().getTemp())
                    .max()
                    .orElse(Double.NaN);

            WeekResponseDTO weatherData = new WeekResponseDTO();
            weatherData.setDate(date.toString());
            weatherData.setMinTemperature(minTemp);
            weatherData.setMaxTemperature(maxTemp);

            returnList.add(weatherData);
        });

        return returnList;
    }

    public LocalDate dtToLocalDate(long dt) {
        return Instant.ofEpochSecond(dt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public LocalDateTime dtToLocalDateTime(long dt) {
        return Instant.ofEpochSecond(dt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
