package app.bpartners.geojobs.endpoint.rest.validator;

import static org.junit.jupiter.api.Assertions.*;

import app.bpartners.geojobs.endpoint.rest.mapper.FileFromMultipartFileMapper;
import app.bpartners.geojobs.model.exception.BadRequestException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GeoJsonValidatorTest {

  private GeoJsonValidator subject;

  @BeforeEach
  void setUp() {
    ObjectMapper mapper = new ObjectMapper();
    FileFromMultipartFileMapper fileMapper = new FileFromMultipartFileMapper();
    subject = new GeoJsonValidator(mapper, fileMapper);
  }

  @Test
  void invalid_geojson_without_polygon_closure_should_throw_IllegalArgumentException()
      throws IOException {
    String content =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {
                "type": "Polygon",
                "coordinates": [
                  [
                    [0.0, 0.0],
                    [1.0, 0.0],
                    [1.0, 1.0],
                    [0.0, 1.0]
                  ]
                ]
              },
              "properties": {}
            }
          ]
        }
        """;
    var file = getInvalidGeoJSONFile(content);
    assertThrows(IllegalArgumentException.class, () -> subject.accept(file));
  }

  @Test
  void invalid_geojson_with_self_intersection_should_throw_BadRequestException()
      throws IOException {
    String content =
        """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "geometry": {
                "type": "Polygon",
                "coordinates": [
                  [
                    [0.0, 0.0],
                    [2.0, 2.0],
                    [0.0, 2.0],
                    [2.0, 0.0],
                    [0.0, 0.0]
                  ]
                ]
              },
              "properties": {
                "name": "Self-intersecting polygon"
              }
            }
          ]
        }
        """;
    var file = getInvalidGeoJSONFile(content);
    assertThrows(BadRequestException.class, () -> subject.accept(file));
  }

  @Test
  void invalid_geojson_with_overlapping_outer_shell_should_throw_BadRequestException()
      throws IOException {
    String content =
        """
        {
          "type": "Feature",
          "geometry": {
            "type": "Polygon",
            "coordinates": [
              [
                [0.0, 0.0],
                [4.0, 0.0],
                [4.0, 4.0],
                [0.0, 4.0],
                [0.0, 0.0]
              ],
              [
                [2.0, 2.0],
                [5.0, 2.0],
                [5.0, 5.0],
                [2.0, 5.0],
                [2.0, 2.0]
              ]
            ]
          },
          "properties": {
            "name": "Hole overlapping outer shell"
          }
        }
        """;
    var file = getInvalidGeoJSONFile(content);
    assertThrows(BadRequestException.class, () -> subject.accept(file));
  }

  @Test
  void invalid_geojson_with_wrong_dimension_should_throw_BadRequestException() throws IOException {
    String content =
        """
        {
          "type": "Feature",
          "geometry": {
            "type": "Polygon",
            "coordinates": [
              [
                [0.0, 0.0],
                [2.0, 0.0, 10.0],
                [2.0, 2.0],
                [0.0, 2.0],
                [0.0, 0.0]
              ]
            ]
          },
          "properties": {
            "name": "Dimension mismatch"
          }
        }
        """;
    var file = getInvalidGeoJSONFile(content);
    assertThrows(BadRequestException.class, () -> subject.accept(file));
  }

  @Test
  void valid_geojson_should_not_throw_any_exception() throws URISyntaxException {
    var resource = getClass().getResource("/geojson/quai-de-bourbon.geojson");
    assertNotNull(resource);
    var file = new File(resource.toURI());
    assertDoesNotThrow(() -> subject.accept(file));
  }

  @Test
  void valid_geojson_from_dijon_should_not_throw_any_exception() throws URISyntaxException {
    var resource = getClass().getResource("/dijon/line-cleaned.geojson");
    assertNotNull(resource);
    var file = new File(resource.toURI());
    assertDoesNotThrow(() -> subject.accept(file));
  }

  @Test
  void invalid_excel_file_should_throw_BadRequestException() throws URISyntaxException {
    var resource = getClass().getResource("/excel/excelFile.xlsx");
    assertNotNull(resource);
    var file = new File(resource.toURI());
    assertThrows(BadRequestException.class, () -> subject.accept(file));
  }

  private File getInvalidGeoJSONFile(String content) throws IOException {
    var tempFile = Files.createTempFile("invalid-" + UUID.randomUUID(), ".geojson").toFile();
    Files.writeString(tempFile.toPath(), content);
    return tempFile;
  }
}
