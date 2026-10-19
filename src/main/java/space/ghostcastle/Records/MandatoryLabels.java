package space.ghostcastle.Records;

import java.util.List;

public record MandatoryLabels (
    List<String> entrypoints,
    StringBuilder rule,
    StringBuilder tls,
    StringBuilder service,
    List<String> urls
) {}
