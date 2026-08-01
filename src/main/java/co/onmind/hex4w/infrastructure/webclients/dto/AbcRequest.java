package co.onmind.hex4w.infrastructure.webclients.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Generic request payload for XDB ABC API.
 * Based on ../api/xdb/src/main/resources/static/js/abcapi.js
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AbcRequest(
    @JsonProperty("way") String way,
    @JsonProperty("what") String what,
    @JsonProperty("from") String from,
    @JsonProperty("some") String some,
    @JsonProperty("show") String show,
    @JsonProperty("call") String call,
    @JsonProperty("with") String with,
    @JsonProperty("puts") Object puts,
    @JsonProperty("where") Object where,
    @JsonProperty("sort") String sort,
    @JsonProperty("limit") Integer limit,
    @JsonProperty("offset") Integer offset
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String way = "sql";
        private String what;
        private String from;
        private String some;
        private String show;
        private String call;
        private String with;
        private Object puts;
        private Object where;
        private String sort;
        private Integer limit;
        private Integer offset;

        public Builder way(String way) { this.way = way; return this; }
        public Builder what(String what) { this.what = what; return this; }
        public Builder from(String from) { this.from = from; return this; }
        public Builder some(String some) { this.some = some; return this; }
        public Builder show(String show) { this.show = show; return this; }
        public Builder call(String call) { this.call = call; return this; }
        public Builder with(String with) { this.with = with; return this; }
        public Builder puts(Object puts) { this.puts = puts; return this; }
        public Builder where(Object where) { this.where = where; return this; }
        public Builder sort(String sort) { this.sort = sort; return this; }
        public Builder limit(int limit) { this.limit = limit; return this; }
        public Builder offset(int offset) { this.offset = offset; return this; }

        public AbcRequest build() {
            return new AbcRequest(way, what, from, some, show, call, with, puts, where, sort, limit, offset);
        }
    }
}