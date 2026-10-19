package space.ghostcastle.Records;

import com.fasterxml.jackson.databind.node.ArrayNode;

public record DockerProxyRecord (String name, ArrayNode containers) {}
