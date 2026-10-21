package space.ghostcastle.Records;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record MandatoryLabels (
        List<String> entrypoints,
        StringBuilder rule,
        StringBuilder tls,
        StringBuilder service,
        List<String> urls,
        StringBuilder containerName
) {}
