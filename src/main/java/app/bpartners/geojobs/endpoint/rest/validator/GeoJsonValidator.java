package app.bpartners.geojobs.endpoint.rest.validator;

import app.bpartners.geojobs.endpoint.rest.mapper.FileFromMultipartFileMapper;
import app.bpartners.geojobs.model.exception.BadRequestException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.locationtech.jts.geom.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@AllArgsConstructor
public class GeoJsonValidator implements Consumer<File> {
  private final ObjectMapper mapper;
  private final GeometryFactory geometryFactory = new GeometryFactory();
  private final FileFromMultipartFileMapper fileMapper;

  public void accept(MultipartFile multipartFile) {
    var file = fileMapper.apply(multipartFile);
    accept(file);
  }

  // reference: https://datatracker.ietf.org/doc/html/rfc7946#section-12
  @Override
  public void accept(File file) {
    try {

      if (!file.getName().endsWith(".geojson"))
        throw new BadRequestException("Should be a GeoJSON file");

      String content = Files.readString(file.toPath());
      JsonNode root = mapper.readTree(content);
      JsonNode features = root.get("features");

      if (features == null || !features.isArray())
        throw new BadRequestException("Missing or invalid 'features' array");

      for (int i = 0; i < features.size(); i++) {
        JsonNode feature = features.get(i);
        JsonNode geometryNode = feature.get("geometry");

        if (geometryNode == null || !geometryNode.get("type").asText().equals("Polygon"))
          throw new BadRequestException("Feature #" + i + " has invalid geometry type");

        JsonNode coordinatesNode = geometryNode.get("coordinates");
        if (coordinatesNode == null || !coordinatesNode.isArray())
          throw new BadRequestException("Feature #" + i + " has missing or malformed coordinates");

        Polygon polygon = parsePolygon(coordinatesNode);
        if (polygon == null || !polygon.isValid())
          throw new BadRequestException("Feature #" + i + " contains an invalid polygon geometry");
      }
    } catch (JsonProcessingException e) {
      throw new BadRequestException("Malformed JSON in GeoJSON file, " + e);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read the GeoJSON file", e);
    }
  }

  private Polygon parsePolygon(JsonNode coordinatesArray) {
    var rings = new ArrayList<LinearRing>();
    for (JsonNode ringCoords : coordinatesArray) {
      LinearRing ring = createRing(ringCoords);
      if (ring == null) throw new BadRequestException("Invalid ring structure");
      rings.add(ring);
    }

    LinearRing shell = rings.getFirst();
    LinearRing[] holes =
        rings.size() > 1 ? rings.subList(1, rings.size()).toArray(new LinearRing[0]) : null;

    return geometryFactory.createPolygon(shell, holes);
  }

  private LinearRing createRing(JsonNode coords) {
    var coordinateList = new ArrayList<Coordinate>();
    for (JsonNode point : coords) {
      if (!point.isArray() || point.size() < 2) return null;
      coordinateList.add(new Coordinate(point.get(0).asDouble(), point.get(1).asDouble()));
    }
    return geometryFactory.createLinearRing(coordinateList.toArray(new Coordinate[0]));
  }
}
