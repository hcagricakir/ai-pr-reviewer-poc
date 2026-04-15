package dev.prreviewer.diff;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ScenarioManifest(Scenario scenario, List<ScenarioChange> changes) {

    @JsonCreator
    public ScenarioManifest(
            @JsonProperty("scenario") Scenario scenario,
            @JsonProperty("changes") List<ScenarioChange> changes
    ) {
        this.scenario = scenario == null ? new Scenario("sample", "Sample", "") : scenario;
        this.changes = changes == null ? List.of() : List.copyOf(changes);
    }

    public record Scenario(String id, String title, String description) {
        @JsonCreator
        public Scenario(
                @JsonProperty("id") String id,
                @JsonProperty("title") String title,
                @JsonProperty("description") String description
        ) {
            this.id = id == null ? "sample" : id;
            this.title = title == null ? id : title;
            this.description = description == null ? "" : description;
        }
    }

    public record ScenarioChange(String path, String base, String head, String changeType) {
        @JsonCreator
        public ScenarioChange(
                @JsonProperty("path") String path,
                @JsonProperty("base") String base,
                @JsonProperty("head") String head,
                @JsonProperty("changeType") String changeType
        ) {
            this.path = path == null ? "" : path;
            this.base = base == null ? "" : base;
            this.head = head == null ? "" : head;
            this.changeType = changeType == null ? "" : changeType;
        }
    }
}
