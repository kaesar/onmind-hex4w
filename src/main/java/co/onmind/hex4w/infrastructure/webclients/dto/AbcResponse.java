package co.onmind.hex4w.infrastructure.webclients.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Generic response from XDB ABC API.
 * Based on ../api/xdb/src/main/resources/static/js/abcapi.js response() function
 * Returns { ok, status, message, total, data }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AbcResponse(
    @JsonProperty("ok") Boolean ok,
    @JsonProperty("status") Integer status,
    @JsonProperty("message") String message,
    @JsonProperty("total") Integer total,
    @JsonProperty("data") Object data
) {
    public boolean isSuccess() {
        return Boolean.TRUE.equals(ok) && status != null && status >= 200 && status < 300;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> dataAsList(Class<T> clazz) {
        if (data instanceof List<?> list) {
            return (List<T>) list;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public <T> T dataAs(Class<T> clazz) {
        if (clazz.isInstance(data)) {
            return clazz.cast(data);
        }
        return null;
    }
}