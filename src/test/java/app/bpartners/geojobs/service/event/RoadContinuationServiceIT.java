package app.bpartners.geojobs.service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.geojobs.conf.FacadeIT;
import app.bpartners.geojobs.endpoint.event.model.RoadContinuationRequested;
import app.bpartners.geojobs.file.bucket.BucketComponent;
import app.bpartners.geojobs.file.hash.FileHash;
import app.bpartners.geojobs.repository.GeoJsonRoadContinuationRepository;
import app.bpartners.geojobs.service.RoadContinuerService;
import java.io.File;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

class RoadContinuationServiceIT extends FacadeIT {

  private static final String EXPECTED_PRESIGNED_URL =
      "https://mock-presigned-url/continued.geojson";
  private final RoadContinuerService continuer = mock(RoadContinuerService.class);
  @Autowired private RoadContinuationService roadContinuationService;
  @Autowired private GeoJsonRoadContinuationRepository roadContinuationRepository;

  @Test
  void should_process_road_continuation_base() throws URISyntaxException {
    var resource = getClass().getResource("/geojson/ambohimanjaka.geojson");
    assertNotNull(resource);
    var geoJSON = new File(resource.toURI());
    int zoom = 17;
    int imageSize = 1_024;

    var event = new RoadContinuationRequested(geoJSON, zoom, imageSize);
    roadContinuationService.accept(event);
    assertFalse(roadContinuationRepository.findAll().isEmpty());
  }

  @Test
  void should_process_road_continuation_with_anosy_rond_point() throws URISyntaxException {
    var resource = getClass().getResource("/geojson/anosy-rond-point.geojson");
    assertNotNull(resource);
    var geoJSON = new File(resource.toURI());
    int zoom = 20;
    int imageSize = 1_040;

    var event = new RoadContinuationRequested(geoJSON, zoom, imageSize);
    roadContinuationService.accept(event);
    var actualR = roadContinuationRepository.findAll();
    var actualContent = actualR.stream().findFirst().orElse(null);

    assertFalse(actualR.isEmpty());
    assertEquals(EXPECTED_PRESIGNED_URL, actualContent.getContinuedGeoJsonPath());
  }

  @Test
  void should_process_road_continuation_with_ambohijatovo() throws URISyntaxException {
    var resource = getClass().getResource("/geojson/ambohijatovo-crossed.geojson");
    assertNotNull(resource);
    var geoJSON = new File(resource.toURI());
    int zoom = 17;
    int imageSize = 1_024;

    var event = new RoadContinuationRequested(geoJSON, zoom, imageSize);
    roadContinuationService.accept(event);
    assertFalse(roadContinuationRepository.findAll().isEmpty());
  }

  @Test
  void should_process_road_continuation_with_quai_de_bourbon() throws URISyntaxException {
    var resource = getClass().getResource("/geojson/quai-de-bourbon.geojson");
    assertNotNull(resource);
    var geoJSON = new File(resource.toURI());
    int zoom = 17;
    int imageSize = 1_025;
    var event = new RoadContinuationRequested(geoJSON, zoom, imageSize);
    roadContinuationService.accept(event);
    assertFalse(roadContinuationRepository.findAll().isEmpty());
  }

  @TestConfiguration
  static class MockConfig {
    @Bean
    public BucketComponent bucketComponent() {
      BucketComponent mock = mock(BucketComponent.class);
      when(mock.upload(any(File.class), anyString())).thenReturn(mock(FileHash.class));
      when(mock.presign(anyString())).thenReturn(EXPECTED_PRESIGNED_URL);
      return mock;
    }
  }
}
